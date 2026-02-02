package fi.dy.masa.minihud.renderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.position.PositionUtils;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.renderer.shapes.SideQuad;
import fi.dy.masa.minihud.util.MiscUtils;
import fi.dy.masa.minihud.util.ShapeRenderType;
import fi.dy.masa.minihud.util.shape.SphereUtils;

public class OverlayRendererLightningRodRange extends OverlayRendererBase {
  public static final OverlayRendererLightningRodRange INSTANCE = new OverlayRendererLightningRodRange();

  private final List<RodEntry> lightningRods; // Replace HashMap<BlockPos, Boolean>
  private final ShapeRenderType renderType;
  private final LayerRange layerRange;
  private final Direction.Axis quadAxis;
  private boolean combineQuads;
  private boolean needsUpdate = true;
  private boolean hasData = false;
  private boolean initialScanComplete = false;

  private OverlayRendererLightningRodRange() {
    this.lightningRods = new ArrayList<>();
    this.quadAxis = Direction.UP.getAxis(); // Y-axis for vertical orientation
    this.renderType = ShapeRenderType.OUTER_EDGE;
    this.layerRange = new LayerRange(null);
    this.combineQuads = Configs.Generic.LIGHTNING_ROD_RANGE_OVERLAY_COMBINE_QUADS.getBooleanValue();
    this.useCulling = false;
  }

  @Override
  protected void allocateBuffers(boolean useOutlines) {
    this.clearBuffers();
    // Index 0: Attraction zones (sphere quads)
    this.renderObjects.add(
        new RenderObjectVbo(() -> this.getName() + "/AttractionZones", MaLiLibPipelines.MINIHUD_SHAPE_OFFSET_NO_CULL));
    // Index 1: Damage zones (box quads)
    this.renderObjects
        .add(new RenderObjectVbo(() -> this.getName() + "/DamageZones", MaLiLibPipelines.MINIHUD_SHAPE_OFFSET_NO_CULL));
    // Index 2: Outlines (lines)
    if (useOutlines) {
      this.renderObjects.add(new RenderObjectVbo(() -> this.getName() + "/Outlines",
          MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH));
    }
    // Index 3: REMOVED - Coverage indicators no longer needed (sphere visual shows
    // coverage)
  }

  @Override
  public String getName() {
    return "LightningRodRange";
  }

  @Override
  public boolean hasData() {
    return this.hasData;
  }

  public void setNeedsUpdate() {
    this.needsUpdate = true;
  }

  /**
   * Called when a block changes in the world. Checks if it's a lightning rod
   * being placed or broken.
   * This allows instant updates without needing to rescan all chunks.
   */
  public void onBlockChange(BlockPos pos, BlockState newState, Level world) {
    if (!RendererToggle.OVERLAY_LIGHTNING_ROD_RANGE.getBooleanValue()) {
      return;
    }

    // Lightning only occurs in the Overworld
    if (!MiscUtils.isOverworld(world)) {
      return;
    }

    Block block = newState.getBlock();
    boolean isRod = isLightningRodBlock(block);
    boolean wasTracked = this.lightningRods.stream().anyMatch(e -> e.pos.equals(pos));

    if (isRod && !wasTracked) {
      // New lightning rod placed - check if it's eligible (sky access)
      boolean isEligible = isLightningRodEligible(world, pos, world.getMaxY());
      RodEntry entry = calculateSphereForRod(pos.immutable(), isEligible);
      addOrReplaceRodEntry(entry);
      this.hasData = true;
      this.setNeedsUpdate(); // Trigger render update
    } else if (!isRod && wasTracked) {
      // Lightning rod removed - remove from cache
      this.lightningRods.removeIf(e -> e.pos.equals(pos));
      this.hasData = !this.lightningRods.isEmpty();
      this.setNeedsUpdate();
    } else if (!isRod && !wasTracked) {
      // A block was placed/removed above/below existing rods - might affect
      // eligibility
      // Check if any tracked rods are in the same XZ column and rebuild their spheres
      for (int i = 0; i < this.lightningRods.size(); i++) {
        RodEntry entry = this.lightningRods.get(i);
        BlockPos rodPos = entry.pos;

        if (rodPos.getX() == pos.getX() && rodPos.getZ() == pos.getZ()) {
          // Recheck eligibility for this rod
          boolean isNowEligible = isLightningRodEligible(world, rodPos, world.getMaxY());
          if (isNowEligible != entry.isEligible) {
            // Eligibility changed - rebuild sphere
            RodEntry newEntry = calculateSphereForRod(rodPos, isNowEligible);
            entry.clear();
            this.lightningRods.set(i, newEntry);
            this.setNeedsUpdate();
          }
        }
      }
    }
  }

  /**
   * Called when a chunk loads. Scans the chunk for lightning rods.
   * This ensures rods in newly loaded chunks are detected without a full world
   * rescan.
   */
  public void onChunkLoad(int chunkX, int chunkZ, Level world) {
    if (!RendererToggle.OVERLAY_LIGHTNING_ROD_RANGE.getBooleanValue() || world == null) {
      return;
    }

    // Lightning only occurs in the Overworld
    if (!MiscUtils.isOverworld(world)) {
      return;
    }

    // Get the chunk and scan it for lightning rods
    LevelChunk chunk = world.getChunk(chunkX, chunkZ);
    if (chunk != null) {
      this.scanChunkForLightningRods(chunk, world);

      // Update hasData flag if we found any rods
      if (!this.lightningRods.isEmpty()) {
        this.hasData = true;
        this.setNeedsUpdate();
      }
    }
  }

  @Override
  public boolean shouldRender(Minecraft mc) {
    // Lightning only occurs in the Overworld, so only render there
    return RendererToggle.OVERLAY_LIGHTNING_ROD_RANGE.getBooleanValue() &&
        mc.level != null &&
        MiscUtils.isOverworld(mc.level);
  }

  @Override
  public boolean needsUpdate(Entity entity, Minecraft mc) {
    // Only update when explicitly requested by events or first load
    // Lightning rods are static blocks - once we know where they are, we:
    // - Remove them via distance cleanup when too far (in scanForLightningRods)
    // - Add/remove them via onBlockChange() when placed/broken (wired via
    // NotificationUtils)
    // - Add them via onChunkLoad() when chunks load (wired via NotificationUtils)
    return this.needsUpdate || this.lastUpdatePos == null;
  }

  @Override
  public void update(Vec3 cameraPos, Entity entity, Minecraft mc, ProfilerFiller profiler) {
    if (this.needsUpdate) {
      profiler.push("lightning_rod_scan");
      this.scanForLightningRods(entity, mc);
      profiler.pop();
      this.needsUpdate = false;

      // Only rebuild VBOs after scanning (when data actually changed)
      // NOT on every update() call!
      if (this.hasData) {
        this.render(cameraPos, mc, profiler);
      }
    }
  }

  // Use default draw() - no per-frame validation needed
  // Cache is updated by events (when wired up) or manual toggle

  private void scanForLightningRods(Entity entity, Minecraft mc) {
    Level world = mc.level;

    if (world == null) {
      return;
    }

    // Clean up rods that are too far away (beyond render distance + 2 chunks)
    // This prevents unbounded memory growth as player explores
    // Follows same pattern as BaseBlockRangeOverlay.updateBlockRanges()
    final Vec3 entityPos = entity.position();
    final double maxDist = (mc.options.renderDistance().get() + 2) * 16;
    final double maxDistSq = maxDist * maxDist;

    this.lightningRods.removeIf(entry -> {
      double dx = entityPos.x - entry.pos.getX();
      double dz = entityPos.z - entry.pos.getZ();
      double distSq = dx * dx + dz * dz;

      if (distSq > maxDistSq) {
        entry.clear(); // Clean up VBO resources
        return true;
      }
      return false;
    });

    // Initial scan: Check top blocks in loaded chunks within render distance
    // Cache any lightning rods found
    // After initial scan, cache is updated by events

    if (!this.initialScanComplete) {
      final int centerChunkX = Mth.floor(entity.getX()) >> 4;
      final int centerChunkZ = Mth.floor(entity.getZ()) >> 4;
      int radius = Math.min(mc.options.renderDistance().get(), 8);

      for (int xOff = -radius; xOff <= radius; xOff++) {
        for (int zOff = -radius; zOff <= radius; zOff++) {
          int chunkX = centerChunkX + xOff;
          int chunkZ = centerChunkZ + zOff;

          LevelChunk chunk = world.getChunk(chunkX, chunkZ);

          if (chunk != null) {
            this.scanChunkForLightningRods(chunk, world);
          }
        }
      }
      this.initialScanComplete = true;
    }

    this.hasData = !this.lightningRods.isEmpty();
  }

  private void scanChunkForLightningRods(LevelChunk chunk, Level world) {
    int chunkMinX = chunk.getPos().getMinBlockX();
    int chunkMinZ = chunk.getPos().getMinBlockZ();
    int minY = world.getMinY();
    int maxY = world.getMaxY();

    // Scan from top to bottom - stop at first non-air block in each column
    // This avoids checking every single Y level
    for (int localX = 0; localX < 16; localX++) {
      for (int localZ = 0; localZ < 16; localZ++) {
        // Start from max height and scan downward
        for (int y = maxY; y >= minY; y--) {
          BlockPos pos = new BlockPos(chunkMinX + localX, y, chunkMinZ + localZ);
          BlockState state = chunk.getBlockState(pos);

          // Skip air blocks - continue down the column
          if (state.isAir()) {
            continue;
          }

          // Found first non-air block - check if it's a lightning rod
          if (isLightningRodBlock(state.getBlock())) {
            // Check eligibility (sky access) and create entry with sphere calculation
            boolean isEligible = isLightningRodEligible(world, pos, maxY);
            RodEntry entry = calculateSphereForRod(pos.immutable(), isEligible);
            addOrReplaceRodEntry(entry);
          }

          // Stop scanning this column - we found the top block
          break;
        }
      }
    }
  }

  /**
   * Checks if a lightning rod has clear sky access (all blocks above it are
   * transparent to skylight).
   * This implements the game's requirement that rods must be the highest block in
   * their column
   * with unobstructed sky access.
   * 
   * @param world  The world/level
   * @param rodPos The position of the lightning rod
   * @param maxY   The maximum Y coordinate to check up to
   * @return true if rod has clear sky access, false if any block above blocks
   *         skylight
   */
  private boolean isLightningRodEligible(Level world, BlockPos rodPos, int maxY) {
    // Check all blocks above the rod up to world max height
    for (int y = rodPos.getY() + 1; y <= maxY; y++) {
      BlockPos checkPos = new BlockPos(rodPos.getX(), y, rodPos.getZ());
      BlockState state = world.getBlockState(checkPos);

      // If it's air, continue checking
      if (state.isAir()) {
        continue;
      }

      // If it blocks skylight, rod is not eligible
      if (!state.propagatesSkylightDown()) {
        return false; // Early exit - found a blocking block
      }
    }

    // All blocks above are either air or transparent - rod is eligible
    return true;
  }

  private static boolean isLightningRodBlock(Block block) {
    return block == Blocks.LIGHTNING_ROD ||
        block == Blocks.EXPOSED_LIGHTNING_ROD ||
        block == Blocks.WEATHERED_LIGHTNING_ROD ||
        block == Blocks.OXIDIZED_LIGHTNING_ROD ||
        block == Blocks.WAXED_LIGHTNING_ROD ||
        block == Blocks.WAXED_EXPOSED_LIGHTNING_ROD ||
        block == Blocks.WAXED_WEATHERED_LIGHTNING_ROD ||
        block == Blocks.WAXED_OXIDIZED_LIGHTNING_ROD;
  }

  /**
   * Creates a position test for 3D spherical range calculation.
   * Adapted from OverlayRendererConduitRange.
   * 
   * @param rodPos The position of the lightning rod
   * @param range  The radius of the sphere (128 blocks)
   * @return A position test that checks if a block is within the sphere
   */
  private static SphereUtils.RingPositionTest getPositionTest(BlockPos rodPos, int range) {
    // Rod center position (block center)
    Vec3 center = new Vec3(rodPos.getX() + 0.5, rodPos.getY() + 0.5, rodPos.getZ() + 0.5);
    double squareRange = range * range; // 128 * 128 = 16384

    return (x, y, z, dir) -> SphereUtils.isPositionInsideOrClosestToRadiusOnBlockRing(
        x, y, z, center, squareRange, Direction.EAST);
  }

  /**
   * Calculates the sphere positions and builds optimized quads for a lightning
   * rod.
   * This is an expensive operation (~30-60ms), so results are cached.
   * 
   * @param pos        The position of the lightning rod
   * @param isEligible Whether the rod is eligible (has sky access)
   * @return A RodEntry containing the cached sphere data
   */
  private RodEntry calculateSphereForRod(BlockPos pos, boolean isEligible) {
    RodEntry entry = new RodEntry(pos, isEligible);

    if (!isEligible) {
      return entry; // No sphere calculation for ineligible rods
    }

    final int RADIUS = 128;

    // Create position test for this rod's sphere
    entry.setTest(getPositionTest(pos, RADIUS));

    // Collect all block positions on sphere shell
    Consumer<BlockPos.MutableBlockPos> positionCollector = (p) -> entry.addPosition(p.asLong());
    SphereUtils.collectSpherePositions(positionCollector, entry.getTest(), pos, RADIUS);

    // Build optimized quads from positions
    if (this.combineQuads) {
      entry.setQuads(SphereUtils.buildSphereShellToQuads(
          entry.getPositions(), this.quadAxis, entry.getTest(),
          this.renderType, this.layerRange));
    }

    return entry;
  }

  /**
   * Adds or replaces a rod entry in the list.
   * If an entry already exists at the same position, it's replaced.
   * 
   * @param entry The rod entry to add or use as replacement
   */
  private void addOrReplaceRodEntry(RodEntry entry) {
    // Find and replace existing entry at same position
    for (int i = 0; i < this.lightningRods.size(); i++) {
      RodEntry existing = this.lightningRods.get(i);
      if (existing.pos.equals(entry.pos)) {
        existing.clear();
        this.lightningRods.set(i, entry);
        return;
      }
    }

    // Not found - add new entry
    this.lightningRods.add(entry);
  }

  @Override
  public void render(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler) {
    Level world = mc.level;

    if (world == null || this.lightningRods.isEmpty()) {
      this.clearBuffers();
      return;
    }

    // Rebuild VBOs because vertices are camera-relative
    // Simply render all cached rod positions - no validation needed
    // Cache is updated by: initial scan, events (when wired up), or manual toggle
    this.allocateBuffers(true);

    profiler.push("attraction_zones");
    this.renderAttractionZones(cameraPos, mc, profiler);
    profiler.pop();

    profiler.push("damage_zones");
    this.renderDamageZones(cameraPos, mc, profiler);
    profiler.pop();

    profiler.push("outlines");
    this.renderOutlines(cameraPos, mc, profiler);
    profiler.pop();
  }

  private void renderAttractionZones(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler) {
    Level world = mc.level;

    if (world == null) {
      return;
    }

    Color4f color = Color4f.fromColor(Configs.Colors.LIGHTNING_ROD_RANGE_OVERLAY_COLOR.getIntegerValue());

    profiler.push("attraction_zone_quads");
    RenderObjectVbo ctx = this.renderObjects.get(0);
    BufferBuilder builder = ctx.start(() -> "minihud:lightning_rod/attraction_zones",
        MaLiLibPipelines.MINIHUD_SHAPE_OFFSET_NO_CULL);

    // Render each eligible rod's sphere
    this.lightningRods.forEach((entry) -> {
      if (!entry.isEligible) {
        return;
      }

      if (this.combineQuads) {
        // Render optimized quads (FAST)
        RenderUtils.renderQuads(entry.getQuads(), color, 0, cameraPos, builder);
      } else {
        // Render individual block positions (SLOWER, more accurate at distance)
        RenderUtils.renderCircleBlockPositions(
            entry.getPositions(), PositionUtils.ALL_DIRECTIONS,
            entry.getTest(), this.renderType, this.layerRange,
            color, 0, cameraPos, builder);
      }
    });

    try {
      MeshData meshData = builder.build();

      if (meshData != null) {
        ctx.upload(meshData, false); // Static geometry - no sorting needed
        meshData.close(); // Release mesh data after upload
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    profiler.pop();
  }

  private void renderDamageZones(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler) {
    final double camX = cameraPos.x;
    final double camY = cameraPos.y;
    final double camZ = cameraPos.z;

    RenderObjectVbo ctx = this.renderObjects.get(1);
    BufferBuilder builder = ctx.start(() -> "minihud:lightning_rod/damage_zones",
        MaLiLibPipelines.MINIHUD_SHAPE_OFFSET_NO_CULL);

    Color4f color = Color4f.fromColor(Configs.Colors.LIGHTNING_ROD_DAMAGE_ZONE_COLOR.getIntegerValue());

    this.lightningRods.forEach((entry) -> {
      if (entry.isEligible) {
        BlockPos pos = entry.pos;
        final double rodX = pos.getX() + 0.5 - camX;
        final double rodY = pos.getY() - camY;
        final double rodZ = pos.getZ() + 0.5 - camZ;

        // Damage zone is 6×12×6 (3 blocks each direction horizontally, 2 below and 9
        // above)
        final double minX = rodX - 3;
        final double minY = rodY - 2;
        final double minZ = rodZ - 3;
        final double maxX = rodX + 3;
        final double maxY = rodY + 10; // 9 blocks above + 1 for the rod itself
        final double maxZ = rodZ + 3;

        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllSidesBatchedQuads(
            (float) minX, (float) minY, (float) minZ,
            (float) maxX, (float) maxY, (float) maxZ,
            color, builder);
      }
    });

    try {
      MeshData meshData = builder.build();

      if (meshData != null) {
        ctx.upload(meshData, false); // Static geometry - no sorting needed
        meshData.close(); // Release mesh data after upload
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void renderOutlines(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler) {
    Level world = mc.level;

    if (world == null) {
      return;
    }

    Color4f attractionColor = Color4f.fromColor(Configs.Colors.LIGHTNING_ROD_RANGE_OVERLAY_COLOR.getIntegerValue(),
        1.0f);
    Color4f damageColor = Color4f.fromColor(Configs.Colors.LIGHTNING_ROD_DAMAGE_ZONE_COLOR.getIntegerValue(), 1.0f);

    profiler.push("outlines");
    RenderObjectVbo ctx = this.renderObjects.get(2);
    BufferBuilder builder = ctx.start(() -> "minihud:lightning_rod/outlines",
        MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH);

    this.lightningRods.forEach((entry) -> {
      if (!entry.isEligible) {
        return;
      }

      // Render sphere outlines
      if (this.combineQuads) {
        RenderUtils.renderQuadLines(entry.getQuads(), attractionColor,
            0, cameraPos, this.glLineWidth, builder);
      } else {
        RenderUtils.renderCircleBlockOutlines(
            entry.getPositions(), PositionUtils.ALL_DIRECTIONS,
            entry.getTest(), this.renderType, this.layerRange,
            attractionColor, 0, cameraPos, this.glLineWidth, builder);
      }

      // Render damage zone box (unchanged)
      BlockPos pos = entry.pos;
      final double rodX = pos.getX() + 0.5 - cameraPos.x;
      final double rodY = pos.getY() - cameraPos.y;
      final double rodZ = pos.getZ() + 0.5 - cameraPos.z;

      final double minX = rodX - 3;
      final double minY = rodY - 2;
      final double minZ = rodZ - 3;
      final double maxX = rodX + 3;
      final double maxY = rodY + 10;
      final double maxZ = rodZ + 3;

      fi.dy.masa.malilib.render.RenderUtils.drawBoxAllEdgesBatchedLines(
          (float) minX, (float) minY, (float) minZ,
          (float) maxX, (float) maxY, (float) maxZ,
          damageColor, this.glLineWidth, builder);
    });

    try {
      MeshData meshData = builder.build();

      if (meshData != null) {
        ctx.upload(meshData, false); // Lines don't need sorting
        meshData.close(); // Release mesh data after upload
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    profiler.pop();
  }

  @Override
  public void reset() {
    this.lightningRods.forEach(RodEntry::clear);
    this.lightningRods.clear();
    this.hasData = false;
    this.needsUpdate = true;
    // DO NOT reset initialScanComplete here!
    // This method is called every frame when hasData is false
    // Only reset scan flag on explicit events (toggle, dimension change)
    // which call setNeedsUpdate() directly
    this.clearBuffers();
  }

  /**
   * Called when overlay is toggled or dimension changes.
   * Forces a full rescan on next update.
   */
  public void forceRescan() {
    this.initialScanComplete = false;
    this.setNeedsUpdate();
  }

  /**
   * Inner class to hold data for each lightning rod's spherical range.
   * Similar to Entry class in OverlayRendererConduitRange.
   */
  private static class RodEntry {
    public final BlockPos pos;
    public final boolean isEligible;
    private final LongOpenHashSet positions; // Sphere shell positions
    @Nullable
    private SphereUtils.RingPositionTest test;
    private final List<SideQuad> quads;

    RodEntry(BlockPos pos, boolean isEligible) {
      this.pos = pos;
      this.isEligible = isEligible;
      this.positions = new LongOpenHashSet();
      this.test = null;
      this.quads = new ArrayList<>();
    }

    public void addPosition(long pos) {
      this.positions.add(pos);
    }

    public LongOpenHashSet getPositions() {
      return this.positions;
    }

    public void setTest(@Nullable SphereUtils.RingPositionTest test) {
      this.test = test;
    }

    @Nullable
    public SphereUtils.RingPositionTest getTest() {
      return this.test;
    }

    public void setQuads(List<SideQuad> quads) {
      this.quads.clear();
      this.quads.addAll(quads);
    }

    public List<SideQuad> getQuads() {
      return this.quads;
    }

    public void clear() {
      this.positions.clear();
      this.quads.clear();
      this.test = null;
    }
  }
}
