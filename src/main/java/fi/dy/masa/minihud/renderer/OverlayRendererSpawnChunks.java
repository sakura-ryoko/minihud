package fi.dy.masa.minihud.renderer;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.position.PositionUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.data.HudDataManager;
import fi.dy.masa.minihud.util.DataStorage;

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
                 (mc.world != null &&
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
            spawnChunkRadius = this.getSimulationDistance();

            red = spawnChunkRadius + 1;
            yellow = spawnChunkRadius;
            green = spawnChunkRadius - 1;
            brown = red + 11;

            brownEnabled = Configs.Generic.SPAWN_PLAYER_OUTER_OVERLAY_ENABLED.getBooleanValue();
            yellowEnabled = Configs.Generic.SPAWN_PLAYER_REDSTONE_OVERLAY_ENABLED.getBooleanValue();
        }
        else if (data.isSpawnChunkRadiusKnown() &&
		         data.getSpawnChunkRadius() > 0 &&
				 data.getWorldSpawn().dimension().equals(mc.world.getRegistryKey()))
        {
            // OVERLAY_SPAWN_CHUNK_OVERLAY_REAL
            this.center = data.getWorldSpawn().pos();
            spawnChunkRadius = data.getSpawnChunkRadius();
            red = spawnChunkRadius + 1;
            yellow = spawnChunkRadius;
            green = spawnChunkRadius - 1;
            brown = red + 11;

            brownEnabled = Configs.Generic.SPAWN_REAL_OUTER_OVERLAY_ENABLED.getBooleanValue();
            yellowEnabled = Configs.Generic.SPAWN_REAL_REDSTONE_OVERLAY_ENABLED.getBooleanValue();
        }
        else
        {
			if (data.getWorldSpawn().dimension().equals(mc.world.getRegistryKey()))
			{
				this.center = data.getWorldSpawn().pos();
				red = 0;
				yellow = 0;
				green = 0;
				brown = 0;

				brownEnabled = false;
				yellowEnabled = false;
			}
			else
			{
				this.hasData = false;
				this.needsUpdate = false;
				return;
			}
        }

        Pair<BlockPos, BlockPos> corners;

		if (this.isPlayerFollowing || data.isSpawnChunkRadiusKnown())
		{
			if (brownEnabled)
			{
				corners = this.getSpawnChunkCorners(this.center, brown, mc.world);   // Org 22 (Brown / WorldGen Only)
				this.boxesBrown = RenderUtils.calculateBoxes(corners.getLeft(), corners.getRight());
			}

			corners = this.getSpawnChunkCorners(this.center, red, mc.world);     // Org 11 (Red / Mob Caps Only)
			this.boxesRed = RenderUtils.calculateBoxes(corners.getLeft(), corners.getRight());

			if (yellowEnabled)
			{
				corners = this.getSpawnChunkCorners(this.center, yellow, mc.world);     // Org 10 (Yellow / Redstone Processing)
				this.boxesYellow = RenderUtils.calculateBoxes(corners.getLeft(), corners.getRight());
			}

			corners = this.getSpawnChunkCorners(this.center, green, mc.world);      // Org 9 (Green / Entity Processing)
			this.boxesGreen = RenderUtils.calculateBoxes(corners.getLeft(), corners.getRight());
		}

        this.hasData = true;
        this.render(cameraPos, mc, profiler);
        this.needsUpdate = false;
    }

    @Override
    public boolean hasData()
    {
        return this.hasData && this.center != null;
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
        BufferBuilder builder = ctx.start(() -> "minihud:spawn_chunk/quads", MaLiLibPipelines.MINIHUD_SHAPE_OFFSET_NO_CULL);

        fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxSidesBatchedQuads(this.center, cameraPos, colorEntity, 0.001, builder);

		if (this.isPlayerFollowing || HudDataManager.getInstance().isSpawnChunkRadiusKnown())
		{
			for (Box entry : this.boxesBrown)
			{
				RenderUtils.renderWallQuads(entry, cameraPos, colorOuter, builder);
			}
			for (Box entry : this.boxesRed)
			{
				RenderUtils.renderWallQuads(entry, cameraPos, colorLazy, builder);
			}
			for (Box entry : this.boxesYellow)
			{
				RenderUtils.renderWallQuads(entry, cameraPos, colorRedstone, builder);
			}
			for (Box entry : this.boxesGreen)
			{
				RenderUtils.renderWallQuads(entry, cameraPos, colorEntity, builder);
			}
		}

        try
        {
            BuiltBuffer meshData = builder.endNullable();

            if (meshData != null)
            {
                ctx.upload(meshData, this.shouldResort);

                if (this.shouldResort)
                {
                    ctx.startResorting(meshData, ctx.createVertexSorter(cameraPos));
                }

                meshData.close();
            }
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererSpawnChunks#renderQuads(): Exception; {}", err.getMessage());
        }

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
        BufferBuilder builder = ctx.start(() -> "minihud:spawn_chunk/outlines", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH);

        // The SpawnPos box looks better with white outlines.  You can't really see the `colorEntity` value
        fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(this.center, cameraPos, Color4f.WHITE, 0.001, builder);

		if (this.isPlayerFollowing || HudDataManager.getInstance().isSpawnChunkRadiusKnown())
		{
			for (Box entry : this.boxesBrown)
			{
				RenderUtils.renderWallOutlines(entry, 16, 16, true, cameraPos, colorOuter, builder);
			}
			for (Box entry : this.boxesRed)
			{
				RenderUtils.renderWallOutlines(entry, 16, 16, true, cameraPos, colorLazy, builder);
			}
			for (Box entry : this.boxesYellow)
			{
				RenderUtils.renderWallOutlines(entry, 16, 16, true, cameraPos, colorRedstone, builder);
			}
			for (Box entry : this.boxesGreen)
			{
				RenderUtils.renderWallOutlines(entry, 16, 16, true, cameraPos, colorEntity, builder);
			}
		}

        try
        {
            BuiltBuffer meshData = builder.endNullable();

            if (meshData != null)
            {
                ctx.upload(meshData, false);
                meshData.close();
            }
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererSpawnChunks#renderOutlines(): Exception; {}", err.getMessage());
        }

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

        int minY = this.getMinY(world, worldSpawn, cx, cz);
        int maxY = world != null ? world.getTopYInclusive() + 1 : 320;
        BlockPos pos1 = new BlockPos( (cx - chunkRange) << 4      , minY,  (cz - chunkRange) << 4);
        BlockPos pos2 = new BlockPos(((cx + chunkRange) << 4) + 15, maxY, ((cz + chunkRange) << 4) + 15);

        return Pair.of(pos1, pos2);
    }

    private int getMinY(World world, BlockPos pos, int cx, int cz)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        int minY;

        // For whatever reason, in Fabulous! Graphics, the Y level gets rendered through to -64,
        //  so let's make use of the player's current Y position, and seaLevel.
//        if (MinecraftClient.isFabulousGraphicsOrBetter() && world != null && mc.player != null)
        if (world != null && mc.player != null)
        {
            int ws = world.getChunk(cx, cz).sampleHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());

            if (mc.player.getBlockPos().getY() >= world.getSeaLevel())
            {
                minY = Math.min(world.getSeaLevel(), ws);
            }
            else
            {
                // Dumb hack to help correct the display
                // mc.player.getBlockPos().getY() - 16
                minY = Math.min(Math.max(world.getBottomSectionCoord(), ws), mc.player.getBlockPos().getY() - 16);
            }
        }
        else
        {
            minY = world != null ? world.getBottomY() : -64;
        }

        return minY;
    }

    protected int getSimulationDistance()
    {
        if (DataStorage.getInstance().isSimulationDistanceKnown())
        {
            return DataStorage.getInstance().getSimulationDistance();
        }

        return 10;
    }
}
