package fi.dy.masa.minihud.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.render.RenderContext;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.position.PositionUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.data.HudDataManager;
import fi.dy.masa.minihud.util.DataStorage;
import fi.dy.masa.minihud.util.MiscUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.ShaderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class OverlayRendererSpawnChunks extends OverlayRendererBase implements AutoCloseable
{
    protected static boolean needsUpdate = true;

    protected final RendererToggle toggle;
    protected final boolean isPlayerFollowing;

    protected List<Box> boxesBrown;
    protected List<Box> boxesRed;
    protected List<Box> boxesYellow;
    protected List<Box> boxesGreen;
    protected BlockPos center;
    private boolean hasData;

    public OverlayRendererSpawnChunks(RendererToggle toggle)
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

    public static void setNeedsUpdate()
    {
        needsUpdate = true;
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
        if (needsUpdate)
        {
            return true;
        }

        // Use the client player, to allow looking from the camera perspective
        entity = this.isPlayerFollowing ? mc.player : entity;

        int ex = (int) Math.floor(entity.getX());
        int ez = (int) Math.floor(entity.getZ());
        int lx = this.lastUpdatePos.getX();
        int lz = this.lastUpdatePos.getZ();

        if (this.isPlayerFollowing)
        {
            return ex != lx || ez != lz;
        }

        int range = mc.options.getViewDistance().getValue() * 16;

        return Math.abs(lx - ex) > range || Math.abs(lz - ez) > range;
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity, MinecraftClient mc)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        // Use the client player, to allow looking from the camera perspective
        entity = this.isPlayerFollowing ? mc.player : entity;

        HudDataManager data = HudDataManager.getInstance();
        BlockPos spawn;
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
            spawn = PositionUtils.getEntityBlockPos(entity);
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
            spawn = data.getWorldSpawn();
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
                needsUpdate = false;

                return;
            }

            red = spawnChunkRadius + 1;
            yellow = spawnChunkRadius;
            green = spawnChunkRadius - 1;
            brown = red + 11;

            brownEnabled = Configs.Generic.SPAWN_REAL_OUTER_OVERLAY_ENABLED.getBooleanValue();
            yellowEnabled = Configs.Generic.SPAWN_REAL_REDSTONE_OVERLAY_ENABLED.getBooleanValue();
        }

//        RenderObjectBase renderQuads = this.renderObjects.get(0);
//        RenderObjectBase renderLines = this.renderObjects.get(1);

        Pair<BlockPos, BlockPos> corners;

        if (brownEnabled)
        {
            corners = this.getSpawnChunkCorners(spawn, brown, mc.world);   // Org 22 (Brown / WorldGen Only)
            this.boxesBrown = RenderUtils.calculateBoxes(corners.getLeft(), corners.getRight());
        }

        corners = this.getSpawnChunkCorners(spawn, red, mc.world);     // Org 11 (Red / Mob Caps Only)
        this.boxesRed = RenderUtils.calculateBoxes(corners.getLeft(), corners.getRight());

        if (yellowEnabled)
        {
            corners = this.getSpawnChunkCorners(spawn, yellow, mc.world);     // Org 10 (Yellow / Redstone Processing)
            this.boxesYellow = RenderUtils.calculateBoxes(corners.getLeft(), corners.getRight());
        }

        corners = this.getSpawnChunkCorners(spawn, green, mc.world);      // Org 9 (Green / Entity Processing)
        this.boxesGreen = RenderUtils.calculateBoxes(corners.getLeft(), corners.getRight());

        this.hasData = true;
        needsUpdate = false;
    }

    @Override
    public boolean hasData()
    {
        return this.hasData;
    }

    @Override
    public void render(Camera camera, Matrix4f matrix4f, Matrix4f projMatrix, MinecraftClient mc, Profiler profiler)
    {
        profiler.push("spawn_chunk_radius");
        if (this.hasData && !this.boxesGreen.isEmpty() && this.center != null)
        {
            this.renderQuads(camera, matrix4f, projMatrix, mc, profiler);
            this.renderOutlines(camera, matrix4f, projMatrix, mc, profiler);
            this.boxesBrown.clear();
            this.boxesRed.clear();
            this.boxesYellow.clear();
            this.boxesGreen.clear();
            this.center = null;
            this.hasData = false;
        }
        profiler.pop();
    }

    private void renderQuads(Camera camera, Matrix4f matrix4f, Matrix4f projMatrix, MinecraftClient mc, Profiler profiler)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        profiler.push("quads");
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

        Vec3d cameraPos = camera.getPos();

        RenderContext ctx = new RenderContext(() -> "Spawn Chunk Quads", MaLiLibPipelines.POSITION_COLOR_SIMPLE, GlUsage.STATIC_WRITE);
        BufferBuilder builder = ctx.getBuilder();
        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        Vec3d updatePos = this.getUpdatePosition();

        this.preRender();
        matrix4fstack.pushMatrix();
        matrix4fstack.translate((float) (updatePos.x - cameraPos.x), (float) (updatePos.y - cameraPos.y), (float) (updatePos.z - cameraPos.z));
        fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxSidesBatchedQuads(this.center, cameraPos, colorEntity, 0.001, builder);

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

        try
        {
            ctx.drawColor(builder.endNullable());
            ctx.close();
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererSpawnChunks#renderQuads(): Exception; {}", err.getMessage());
        }

        this.postRender();
        matrix4fstack.popMatrix();
        profiler.pop();
    }

    private void renderOutlines(Camera camera, Matrix4f matrix4f, Matrix4f projMatrix, MinecraftClient mc, Profiler profiler)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        profiler.push("outlines");
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

        Vec3d cameraPos = camera.getPos();

        RenderContext ctx = new RenderContext(() -> "Spawn Chunk Lines", ShaderPipelines.LINES, GlUsage.STATIC_WRITE);
        BufferBuilder builder = ctx.getBuilder();
        MatrixStack matrices = new MatrixStack();
        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        Vec3d updatePos = this.getUpdatePosition();

        this.preRender();
        matrices.push();
        matrix4fstack.pushMatrix();
        matrix4fstack.translate((float) (updatePos.x - cameraPos.x), (float) (updatePos.y - cameraPos.y), (float) (updatePos.z - cameraPos.z));
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
            ctx.drawColor(builder.endNullable());
            ctx.close();
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererSpawnChunks#renderOutlines(): Exception; {}", err.getMessage());
        }

        this.postRender();
        matrices.pop();
        matrix4fstack.popMatrix();

        profiler.pop();
    }

    @Override
    public void reset()
    {
        this.boxesBrown.clear();
        this.boxesRed.clear();
        this.boxesYellow.clear();
        this.boxesGreen.clear();
        this.center = BlockPos.ORIGIN;
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

        int minY = this.getMinY(world);
        int maxY = world != null ? world.getTopYInclusive() + 1 : 320;
        BlockPos pos1 = new BlockPos( (cx - chunkRange) << 4      , minY,  (cz - chunkRange) << 4);
        BlockPos pos2 = new BlockPos(((cx + chunkRange) << 4) + 15, maxY, ((cz + chunkRange) << 4) + 15);

        return Pair.of(pos1, pos2);
    }

    private int getMinY(World world)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        int minY;

        // For whatever reason, in Fabulous! Graphics, the Y level gets rendered through to -64,
        //  so let's make use of the player's current Y position, and seaLevel.
        if (MinecraftClient.isFabulousGraphicsOrBetter() && world != null && mc.player != null)
        {
            if (mc.player.getBlockPos().getY() >= world.getSeaLevel())
            {
                minY = world.getSeaLevel() - 2;
            }
            else
            {
                minY = world.getBottomY();
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
}
