package fi.dy.masa.minihud.renderer;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlBufferTarget;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.ShaderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.position.PositionUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.data.HudDataManager;
import fi.dy.masa.minihud.util.DataStorage;
import fi.dy.masa.minihud.util.MiscUtils;

public class OverlayRendererSpawnChunks extends OverlayRendererBase implements AutoCloseable
{
    public static final OverlayRendererSpawnChunks INSTANCE_PLAYER = new OverlayRendererSpawnChunks(RendererToggle.OVERLAY_SPAWN_CHUNK_OVERLAY_PLAYER);
    public static final OverlayRendererSpawnChunks INSTANCE_REAL = new OverlayRendererSpawnChunks(RendererToggle.OVERLAY_SPAWN_CHUNK_OVERLAY_REAL);
    protected final RendererToggle toggle;
    protected final boolean isPlayerFollowing;
    protected boolean needsUpdate = true;

    protected List<Box> boxesBrown;
    protected List<Box> boxesRed;
    protected List<Box> boxesYellow;
    protected List<Box> boxesGreen;
    protected BlockPos center;
    private boolean hasData;

    protected OverlayRendererSpawnChunks(RendererToggle toggle)
    {
        this.toggle = toggle;
        this.isPlayerFollowing = toggle == RendererToggle.OVERLAY_SPAWN_CHUNK_OVERLAY_PLAYER;
        this.boxesBrown = new ArrayList<>();
        this.boxesRed = new ArrayList<>();
        this.boxesYellow = new ArrayList<>();
        this.boxesGreen = new ArrayList<>();
        this.center = BlockPos.ORIGIN;
        this.hasData = false;
    }

    @Override
    public String getName()
    {
        return "SpawnChunks";
    }

    public void setNeedsUpdate()
    {
        this.needsUpdate = true;
    }

    @Override
    public boolean shouldRender(MinecraftClient mc)
    {
        return this.toggle.getBooleanValue() &&
                (this.isPlayerFollowing ||
                 (mc.world != null && MiscUtils.isOverworld(mc.world) &&
                 HudDataManager.getInstance().isWorldSpawnKnown()));
    }

    @Override
    public boolean needsUpdate(Entity entity, MinecraftClient mc)
    {
        if (this.needsUpdate)
        {
            return true;
        }

        if (mc.player == null)
        {
            return false;
        }

        // Use the client player, to allow looking from the camera perspective
        entity = this.isPlayerFollowing ? mc.player : entity;

        if (this.lastUpdatePos == null)
        {
            this.lastUpdatePos = entity.getBlockPos();
            return true;
        }

        int ex = (int) Math.floor(entity.getX());
        int ey = (int) Math.floor(entity.getY());
        int ez = (int) Math.floor(entity.getZ());
        int lx = this.lastUpdatePos.getX();
        int ly = this.lastUpdatePos.getY();
        int lz = this.lastUpdatePos.getZ();

        if (this.isPlayerFollowing)
        {
            return ex != lx || ez != lz || Math.abs(ey - ly) > 16;
        }

        int range = mc.options.getViewDistance().getValue() * 16;

        return Math.abs(lx - ex) > range || Math.abs(ey - ly) > 16 || Math.abs(lz - ez) > range;
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity, MinecraftClient mc, Profiler profiler)
    {
        if (mc.world == null || mc.player == null || !RenderSystem.isOnRenderThread())
        {
            return;
        }

        // Use the client player, to allow looking from the camera perspective
        entity = this.isPlayerFollowing ? mc.player : entity;

        HudDataManager data = HudDataManager.getInstance();
        int spawnChunkRadius;
        int red;
        int yellow;                 // Redstone Processing border
        int green;
        int brown;
        boolean brownEnabled;
        boolean yellowEnabled;

        if (this.isPlayerFollowing)
        {
            // OVERLAY_SPAWN_CHUNK_OVERLAY_PLAYER
            this.center = PositionUtils.getEntityBlockPos(entity);
            spawnChunkRadius = getSimulationDistance();

            red = spawnChunkRadius + 1;
            yellow = spawnChunkRadius;
            green = spawnChunkRadius - 1;
            brown = red + 11;

            brownEnabled = Configs.Generic.SPAWN_PLAYER_OUTER_OVERLAY_ENABLED.getBooleanValue();
            yellowEnabled = Configs.Generic.SPAWN_PLAYER_REDSTONE_OVERLAY_ENABLED.getBooleanValue();
        }
        else
        {
            // OVERLAY_SPAWN_CHUNK_OVERLAY_REAL
            this.center = data.getWorldSpawn();
            spawnChunkRadius = data.getSpawnChunkRadius();

            if (spawnChunkRadius < 0)
            {
                spawnChunkRadius = getSpawnChunkRadius(mc.getServer());
                data.setSpawnChunkRadiusIfUnknown(spawnChunkRadius);
            }
            if (spawnChunkRadius < 0)
            {
                spawnChunkRadius = 2;       // In case there is a sync/logic issue, use Default Value.
            }
            if (spawnChunkRadius == 0)
            {
                // We have nothing to render.
                MiniHUD.LOGGER.warn("overlaySpawnChunkReal: toggling feature OFF since SPAWN_CHUNK_RADIUS is set to 0 (Nothing to render)");

                RendererToggle.OVERLAY_SPAWN_CHUNK_OVERLAY_REAL.setBooleanValue(false);
                this.needsUpdate = false;

                return;
            }

            red = spawnChunkRadius + 1;
            yellow = spawnChunkRadius;
            green = spawnChunkRadius - 1;
            brown = red + 11;

            brownEnabled = Configs.Generic.SPAWN_REAL_OUTER_OVERLAY_ENABLED.getBooleanValue();
            yellowEnabled = Configs.Generic.SPAWN_REAL_REDSTONE_OVERLAY_ENABLED.getBooleanValue();
        }

        Pair<BlockPos, BlockPos> corners;

        if (brownEnabled)
        {
            corners = this.getSpawnChunkCorners(this.center, brown, mc.world);   // Org 22 (Brown / WorldGen Only)
            this.boxesBrown = this.calculateBoxes(corners.getLeft(), corners.getRight());
        }

        corners = this.getSpawnChunkCorners(this.center, red, mc.world);     // Org 11 (Red / Mob Caps Only)
        this.boxesRed = this.calculateBoxes(corners.getLeft(), corners.getRight());

        if (yellowEnabled)
        {
            corners = this.getSpawnChunkCorners(this.center, yellow, mc.world);     // Org 10 (Yellow / Redstone Processing)
            this.boxesYellow = this.calculateBoxes(corners.getLeft(), corners.getRight());
        }

        corners = this.getSpawnChunkCorners(this.center, green, mc.world);      // Org 9 (Green / Entity Processing)
        this.boxesGreen = this.calculateBoxes(corners.getLeft(), corners.getRight());

        this.hasData = true;
        this.render(cameraPos, mc, profiler);
        this.needsUpdate = false;
    }

    @Override
    public boolean hasData()
    {
        return this.hasData && !this.boxesGreen.isEmpty() && this.center != null;
    }

    @Override
    public void render(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        this.allocateBuffers();
        this.renderQuads(cameraPos, mc, profiler);
        this.renderOutlines(cameraPos, mc, profiler);
    }

    private void renderQuads(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        profiler.push("spawn_chunk_quads");
        final Color4f colorEntity = this.isPlayerFollowing ?
                Configs.Colors.SPAWN_PLAYER_ENTITY_OVERLAY_COLOR.getColor() :
                Configs.Colors.SPAWN_REAL_ENTITY_OVERLAY_COLOR.getColor();
        final Color4f colorRedstone = this.isPlayerFollowing ?
                Configs.Colors.SPAWN_PLAYER_REDSTONE_OVERLAY_COLOR.getColor() :
                Configs.Colors.SPAWN_REAL_REDSTONE_OVERLAY_COLOR.getColor();
        final Color4f colorLazy = this.isPlayerFollowing ?
                Configs.Colors.SPAWN_PLAYER_LAZY_OVERLAY_COLOR.getColor() :
                Configs.Colors.SPAWN_REAL_LAZY_OVERLAY_COLOR.getColor();
        final Color4f colorOuter = this.isPlayerFollowing ?
                Configs.Colors.SPAWN_PLAYER_OUTER_OVERLAY_COLOR.getColor() :
                Configs.Colors.SPAWN_REAL_OUTER_OVERLAY_COLOR.getColor();

        RenderObjectVbo ctx = this.renderObjects.getFirst();
        BufferBuilder builder = ctx.start(() -> "Spawn Chunk Quads", MaLiLibPipelines.POSITION_COLOR_SIMPLE, GlUsage.STATIC_WRITE);
        MatrixStack matrices = new MatrixStack();

        matrices.push();
        fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxSidesBatchedQuads(this.center, cameraPos, colorEntity, 0.001, builder);

        MatrixStack.Entry e = matrices.peek();

        for (Box entry : this.boxesBrown)
        {
            RenderUtils.renderWallQuads(entry, cameraPos, colorOuter, builder, e);
        }
        for (Box entry : this.boxesRed)
        {
            RenderUtils.renderWallQuads(entry, cameraPos, colorLazy, builder, e);
        }
        for (Box entry : this.boxesYellow)
        {
            RenderUtils.renderWallQuads(entry, cameraPos, colorRedstone, builder, e);
        }
        for (Box entry : this.boxesGreen)
        {
            RenderUtils.renderWallQuads(entry, cameraPos, colorEntity, builder, e);
        }

        try
        {
            ctx.upload(builder.endNullable(), GlBufferTarget.VERTICES);
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererSpawnChunks#renderQuads(): Exception; {}", err.getMessage());
        }

        matrices.pop();
        profiler.pop();
    }

    private void renderOutlines(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        profiler.push("spawn_chunk_outlines");
        final Color4f colorEntity = this.isPlayerFollowing ?
                Configs.Colors.SPAWN_PLAYER_ENTITY_OVERLAY_COLOR.getColor() :
                Configs.Colors.SPAWN_REAL_ENTITY_OVERLAY_COLOR.getColor();
        final Color4f colorRedstone = this.isPlayerFollowing ?
                Configs.Colors.SPAWN_PLAYER_REDSTONE_OVERLAY_COLOR.getColor() :
                Configs.Colors.SPAWN_REAL_REDSTONE_OVERLAY_COLOR.getColor();
        final Color4f colorLazy = this.isPlayerFollowing ?
                Configs.Colors.SPAWN_PLAYER_LAZY_OVERLAY_COLOR.getColor() :
                Configs.Colors.SPAWN_REAL_LAZY_OVERLAY_COLOR.getColor();
        final Color4f colorOuter = this.isPlayerFollowing ?
                Configs.Colors.SPAWN_PLAYER_OUTER_OVERLAY_COLOR.getColor() :
                Configs.Colors.SPAWN_REAL_OUTER_OVERLAY_COLOR.getColor();

        RenderObjectVbo ctx = this.renderObjects.get(1);
        BufferBuilder builder = ctx.start(() -> "Spawn Chunk Lines", ShaderPipelines.LINES, GlUsage.STATIC_WRITE);
        MatrixStack matrices = new MatrixStack();

        matrices.push();
        fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(this.center, cameraPos, colorEntity, 0.001, builder, matrices);

        MatrixStack.Entry e = matrices.peek();

        for (Box entry : this.boxesBrown)
        {
            RenderUtils.renderWallOutlines(entry, 16, 16, true, cameraPos, colorOuter, builder, e);
        }
        for (Box entry : this.boxesRed)
        {
            RenderUtils.renderWallOutlines(entry, 16, 16, true, cameraPos, colorLazy, builder, e);
        }
        for (Box entry : this.boxesYellow)
        {
            RenderUtils.renderWallOutlines(entry, 16, 16, true, cameraPos, colorRedstone, builder, e);
        }
        for (Box entry : this.boxesGreen)
        {
            RenderUtils.renderWallOutlines(entry, 16, 16, true, cameraPos, colorEntity, builder, e);
        }

        try
        {
            ctx.upload(builder.endNullable(), GlBufferTarget.VERTICES);
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererSpawnChunks#renderOutlines(): Exception; {}", err.getMessage());
        }

        matrices.pop();
        profiler.pop();
    }

    @Override
    public void reset()
    {
        super.reset();
        this.boxesBrown.clear();
        this.boxesRed.clear();
        this.boxesYellow.clear();
        this.boxesGreen.clear();
        this.center = null;
//        this.renderData.clear();
        this.hasData = false;
    }

    @Override
    public void close()
    {
        this.reset();
    }

    protected Pair<BlockPos, BlockPos> getSpawnChunkCorners(BlockPos worldSpawn, int chunkRange, World world)
    {
        int cx = (worldSpawn.getX() >> 4);
        int cz = (worldSpawn.getZ() >> 4);

        int minY = this.getMinY(world, cx, cz);
        int maxY = world != null ? world.getTopYInclusive() + 1 : 320;
        BlockPos pos1 = new BlockPos( (cx - chunkRange) << 4      , minY,  (cz - chunkRange) << 4);
        BlockPos pos2 = new BlockPos(((cx + chunkRange) << 4) + 15, maxY, ((cz + chunkRange) << 4) + 15);

        return Pair.of(pos1, pos2);
    }

    private int getMinY(World world, int cx, int cz)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        int minY;

        // For whatever reason, in Fabulous! Graphics, the Y level gets rendered through to -64,
        //  so let's make use of the player's current Y position, and seaLevel.
//        if (MinecraftClient.isFabulousGraphicsOrBetter() && world != null && mc.player != null)
        if (world != null && mc.player != null)
        {
            if (mc.player.getBlockPos().getY() >= world.getSeaLevel())
            {
                minY = world.getSeaLevel();
            }
            else
            {
                // Dumb hack to help correct the display
                minY = Math.max(world.getBottomSectionCoord(), mc.player.getBlockPos().getY() - 16);
            }
        }
        else
        {
            minY = world != null ? world.getBottomY() : -64;
        }

        return minY;
    }

    protected int getSpawnChunkRadius(@Nullable MinecraftServer server)
    {
        if (server != null)
        {
            return server.getOverworld().getGameRules().getInt(GameRules.SPAWN_CHUNK_RADIUS);
        }
        else if (HudDataManager.getInstance().isSpawnChunkRadiusKnown())
        {
            return HudDataManager.getInstance().getSpawnChunkRadius();
        }

        return 2;
    }

    protected int getSimulationDistance()
    {
        if (DataStorage.getInstance().isSimulationDistanceKnown())
        {
            return DataStorage.getInstance().getSimulationDistance();
        }

        return 10;
    }

    /**
     * Assumes a BufferBuilder in GL_QUADS mode has been initialized
     */
//    public static void drawBlockBoundingBoxSidesBatchedQuads(BlockPos pos, Vec3d cameraPos, Color4f color, double expand, BufferBuilder buffer)
//    {
//        float minX = (float) (pos.getX() - cameraPos.x - expand);
//        float minY = (float) (pos.getY() - cameraPos.y - expand);
//        float minZ = (float) (pos.getZ() - cameraPos.z - expand);
//        float maxX = (float) (pos.getX() - cameraPos.x + expand + 1);
//        float maxY = (float) (pos.getY() - cameraPos.y + expand + 1);
//        float maxZ = (float) (pos.getZ() - cameraPos.z + expand + 1);
//
//        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllSidesBatchedQuads(minX, minY, minZ, maxX, maxY, maxZ, color, buffer);
//    }

    public List<Box> calculateBoxes(
            BlockPos posStart,
            BlockPos posEnd)
    {
        Entity entity = EntityUtils.getCameraEntity();
        if (entity == null) return List.of();
//        World world = entity.getEntityWorld();
        final int boxMinX = Math.min(posStart.getX(), posEnd.getX());
        final int boxMinZ = Math.min(posStart.getZ(), posEnd.getZ());
        final int boxMaxX = Math.max(posStart.getX(), posEnd.getX());
        final int boxMaxZ = Math.max(posStart.getZ(), posEnd.getZ());

        final int centerX = (int) Math.floor(entity.getX());
        final int centerZ = (int) Math.floor(entity.getZ());
        final int maxDist = MinecraftClient.getInstance().options.getViewDistance().getValue() * 32; // double the view distance in blocks
        final int rangeMinX = centerX - maxDist;
        final int rangeMinZ = centerZ - maxDist;
        final int rangeMaxX = centerX + maxDist;
        final int rangeMaxZ = centerZ + maxDist;
        final double minY = Math.min(posStart.getY(), posEnd.getY());
        final double maxY = Math.max(posStart.getY(), posEnd.getY()) + 1;
        double minX, minZ, maxX, maxZ;

        List<Box> boxes = new ArrayList<>();

        // The sides of the box along the x-axis can be at least partially inside the range
        if (rangeMinX <= boxMaxX && rangeMaxX >= boxMinX)
        {
            minX = Math.max(boxMinX, rangeMinX);
            maxX = Math.min(boxMaxX, rangeMaxX) + 1;

            if (rangeMinZ <= boxMinZ && rangeMaxZ >= boxMinZ)
            {
                minZ = maxZ = boxMinZ;
                boxes.add(new Box(minX, minY, minZ, maxX, maxY, maxZ));
            }

            if (rangeMinZ <= boxMaxZ && rangeMaxZ >= boxMaxZ)
            {
                minZ = maxZ = boxMaxZ + 1;
                boxes.add(new Box(minX, minY, minZ, maxX, maxY, maxZ));
            }
        }

        // The sides of the box along the z-axis can be at least partially inside the range
        if (rangeMinZ <= boxMaxZ && rangeMaxZ >= boxMinZ)
        {
            minZ = Math.max(boxMinZ, rangeMinZ);
            maxZ = Math.min(boxMaxZ, rangeMaxZ) + 1;

            if (rangeMinX <= boxMinX && rangeMaxX >= boxMinX)
            {
                minX = maxX = boxMinX;
                boxes.add(new Box(minX, minY, minZ, maxX, maxY, maxZ));
            }

            if (rangeMinX <= boxMaxX && rangeMaxX >= boxMaxX)
            {
                minX = maxX = boxMaxX + 1;
                boxes.add(new Box(minX, minY, minZ, maxX, maxY, maxZ));
            }
        }

        return boxes;
    }

//    private static void dumpBoxes(List<Box> boxes)
//    {
//        System.out.print("DUMP BOXES -->\n");
//        int i = 0;
//
//        for (Box bb : boxes)
//        {
//            System.out.printf("  Box[%d]: [%s]\n", i, bb.toString());
//            i++;
//        }
//
//        System.out.printf("END DUMP (total: %d)\n", i);
//    }
}
