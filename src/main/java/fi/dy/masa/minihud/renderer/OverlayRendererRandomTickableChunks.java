package fi.dy.masa.minihud.renderer;

import java.util.*;
import javax.annotation.Nullable;
import com.google.gson.JsonObject;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;

import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;

public class OverlayRendererRandomTickableChunks extends OverlayRendererBase
{
    public static final OverlayRendererRandomTickableChunks INSTANCE_FIXED = new OverlayRendererRandomTickableChunks(RendererToggle.OVERLAY_RANDOM_TICKS_FIXED);
    public static final OverlayRendererRandomTickableChunks INSTANCE_PLAYER = new OverlayRendererRandomTickableChunks(RendererToggle.OVERLAY_RANDOM_TICKS_PLAYER);

    private static final Direction[] HORIZONTALS = new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };
    protected boolean needsUpdate = true;
    @Nullable public Vec3d newPos;

    protected RendererToggle toggle;
    protected Vec3d pos = Vec3d.ZERO;
    protected double minX;
    protected double minZ;
    protected double maxX;
    protected double maxZ;

    private HashMap<ChunkPos, List<Box>> chunkMap;
    private Entity cameraEntity;
    private boolean hasData;

    protected OverlayRendererRandomTickableChunks(RendererToggle toggle)
    {
        this.toggle = toggle;
        this.useCulling = true;
        this.renderThrough = false;
        this.chunkMap = new HashMap<>();
        this.cameraEntity = null;
        this.hasData = false;
    }

    @Override
    public String getName()
    {
        return "RandomTickableChunks";
    }

    public void setNeedsUpdate()
    {
        this.needsUpdate = true;
    }

    public void setNewPos(@Nullable Vec3d pos)
    {
        this.newPos = pos;
    }

    @Override
    public boolean shouldRender(MinecraftClient mc)
    {
        return this.toggle.getBooleanValue();
    }

    @Override
    public boolean needsUpdate(Entity entity, MinecraftClient mc)
    {
        if (this.needsUpdate)
        {
            return true;
        }

        if (this.toggle == RendererToggle.OVERLAY_RANDOM_TICKS_FIXED)
        {
            return this.newPos != null;
        }
        // Player-following renderer
        else if (this.toggle == RendererToggle.OVERLAY_RANDOM_TICKS_PLAYER)
        {
            return entity.getX() != this.pos.x || entity.getZ() != this.pos.z;
        }

        return false;
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity, MinecraftClient mc, Profiler profiler)
    {
        if (this.toggle == RendererToggle.OVERLAY_RANDOM_TICKS_PLAYER)
        {
            this.pos = entity.getPos();
        }
        else if (this.newPos != null)
        {
            this.pos = this.newPos;
            this.newPos = null;
        }

        this.chunkMap.clear();
        Set<ChunkPos> chunks = this.getRandomTickableChunks(this.pos);
        this.cameraEntity = entity;

        for (ChunkPos pos : chunks)
        {
//            this.calculateChunkEdgesIfApplicable(pos, chunks, entity.getEntityWorld());

            List<Box> boxes = new ArrayList<>();

            for (Direction side : HORIZONTALS)
            {
                ChunkPos posAdj = new ChunkPos(pos.x + side.getOffsetX(), pos.z + side.getOffsetZ());

                if (!chunks.contains(posAdj))
                {
                    Box bb = this.calculateChunkEdge(pos, side, entity.getEntityWorld());

                    if (bb != null)
                    {
                        boxes.add(bb);
                    }
                }
            }

            if (!boxes.isEmpty())
            {
                this.chunkMap.put(pos, boxes);
                this.hasData = true;
            }
        }

        if (this.hasData())
        {
            this.render(cameraPos, mc, profiler);
        }

        this.needsUpdate = false;
    }

    @Override
    public boolean hasData()
    {
        return this.hasData && !this.chunkMap.isEmpty() && this.cameraEntity != null;
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

        profiler.push("random_tick_quads");
        final Color4f color = this.toggle == RendererToggle.OVERLAY_RANDOM_TICKS_PLAYER ?
                              Configs.Colors.RANDOM_TICKS_PLAYER_OVERLAY_COLOR.getColor() :
                              Configs.Colors.RANDOM_TICKS_FIXED_OVERLAY_COLOR.getColor();

        RenderObjectVbo ctx = this.renderObjects.getFirst();
        BufferBuilder builder = ctx.start(() -> "RandomTick Quads", MaLiLibPipelines.POSITION_COLOR_MASA_LESSER_DEPTH, BufferUsage.STATIC_WRITE);
        MatrixStack matrices = new MatrixStack();

        matrices.push();
        MatrixStack.Entry e = matrices.peek();

        this.chunkMap.forEach(
                (pos, boxes) ->
                {
                    for (Box bb : boxes)
                    {
                        RenderUtils.renderWallQuads(bb, cameraPos, color, builder, e);
                    }
                });

        try
        {
            ctx.upload(builder.endNullable(), BufferType.VERTICES);
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererRandomTickableChunks#renderQuads(): Exception; {}", err.getMessage());
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

        profiler.push("random_tick_outlines");
        final Color4f color = this.toggle == RendererToggle.OVERLAY_RANDOM_TICKS_PLAYER ?
                              Configs.Colors.RANDOM_TICKS_PLAYER_OVERLAY_COLOR.getColor() :
                              Configs.Colors.RANDOM_TICKS_FIXED_OVERLAY_COLOR.getColor();

        RenderObjectVbo ctx = this.renderObjects.get(1);
        BufferBuilder builder = ctx.start(() -> "RandomTick Lines", ShaderPipelines.LINES, BufferUsage.STATIC_WRITE);
        MatrixStack matrices = new MatrixStack();


        matrices.push();
        MatrixStack.Entry e = matrices.peek();

        this.chunkMap.forEach(
                (pos, boxes) ->
                {
                    for (Box bb : boxes)
                    {
                        RenderUtils.renderWallOutlines(bb, 16, 16, true, cameraPos, color, builder, e);
                    }
                });

        try
        {
            ctx.upload(builder.endNullable(), BufferType.VERTICES);
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererRandomTickableChunks#renderOutlines(): Exception; {}", err.getMessage());
        }

        matrices.pop();
        profiler.pop();
    }

    @Override
    public void reset()
    {
        super.reset();
        this.chunkMap.clear();
        this.cameraEntity = null;
        this.hasData = false;
    }

    protected Set<ChunkPos> getRandomTickableChunks(Vec3d posCenter)
    {
        Set<ChunkPos> set = new HashSet<>();
        final int centerChunkX = ((int) Math.floor(posCenter.x)) >> 4;
        final int centerChunkZ = ((int) Math.floor(posCenter.z)) >> 4;
        final double maxRange = 128D * 128D;
        final int r = 9;

        for (int cz = centerChunkZ - r; cz <= centerChunkZ + r; ++cz)
        {
            for (int cx = centerChunkX - r; cx <= centerChunkX + r; ++cx)
            {
                double dx = (double) (cx * 16 + 8) - posCenter.x;
                double dz = (double) (cz * 16 + 8) - posCenter.z;

                if ((dx * dx + dz * dz) < maxRange)
                {
                    set.add(new ChunkPos(cx, cz));
                }
            }
        }

        if (!set.isEmpty())
        {
            this.hasData = true;
        }

        return set;
    }

//    protected void calculateChunkEdgesIfApplicable(ChunkPos pos, Set<ChunkPos> chunks, World world)
//    {
//        for (Direction side : HORIZONTALS)
//        {
//            ChunkPos posAdj = new ChunkPos(pos.x + side.getOffsetX(), pos.z + side.getOffsetZ());
//
//            if (!chunks.contains(posAdj))
//            {
//                this.calculateChunkEdge(pos, side, world);
//            }
//        }
//    }

    private @Nullable Box calculateChunkEdge(ChunkPos pos, Direction side, World world)
    {
        float minX, minZ, maxX, maxZ;

        switch (side)
        {
            case NORTH:
                minX = (float) (pos.x << 4);
                minZ = (float) (pos.z << 4);
                maxX = (float) ((double) (pos.x << 4) + 16.0);
                maxZ = (float) (pos.z << 4);
                break;
            case SOUTH:
                minX = (float) (pos.x << 4);
                minZ = (float) ((double) (pos.z << 4) + 16.0);
                maxX = (float) ((double) (pos.x << 4) + 16.0);
                maxZ = (float) ((double) (pos.z << 4) + 16.0);
                break;
            case WEST:
                minX = (float) (pos.x << 4);
                minZ = (float) (pos.z << 4);
                maxX = (float) (pos.x << 4);
                maxZ = (float) ((double) (pos.z << 4) + 16.0);
                break;
            case EAST:
                minX = (float) ((double) (pos.x << 4) + 16.0);
                minZ = (float) (pos.z << 4);
                maxX = (float) ((double) (pos.x << 4) + 16.0);
                maxZ = (float) ((double) (pos.z << 4) + 16.0);
                break;
            default:
                return null;
        }

        int minY = world != null ? world.getBottomY() : -64;
        int maxY = world != null ? world.getTopYInclusive() + 1 : 320;

        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public String getSaveId()
    {
        return this.toggle == RendererToggle.OVERLAY_RANDOM_TICKS_FIXED ? "random_tickable_chunks" : "";
    }

    @Nullable
    @Override
    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();
        obj.add("pos", JsonUtils.vec3dToJson(this.pos));
        return obj;
    }

    @Override
    public void fromJson(JsonObject obj)
    {
        Vec3d pos = JsonUtils.vec3dFromJson(obj, "pos");

        if (pos != null && this.toggle == RendererToggle.OVERLAY_RANDOM_TICKS_FIXED)
        {
            newPos = pos;
        }
    }
}
