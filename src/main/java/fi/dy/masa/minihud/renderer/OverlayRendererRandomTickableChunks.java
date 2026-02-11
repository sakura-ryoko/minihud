package fi.dy.masa.minihud.renderer;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
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
    @Nullable public Vec3 newPos;

    protected RendererToggle toggle;
    protected Vec3 pos = Vec3.ZERO;
    protected double minX;
    protected double minZ;
    protected double maxX;
    protected double maxZ;

    private final HashMap<ChunkPos, List<AABB>> chunkMap;
    private Entity cameraEntity;
    private boolean hasData;

    protected OverlayRendererRandomTickableChunks(RendererToggle toggle)
    {
        this.toggle = toggle;
        this.useCulling = false;
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

    public void setNewPos(@Nullable Vec3 pos)
    {
        this.newPos = pos;
    }

    @Override
    public boolean shouldRender(Minecraft mc)
    {
        return this.toggle.getBooleanValue();
    }

    @Override
    public boolean needsUpdate(Entity entity, Minecraft mc)
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
    public void update(Vec3 cameraPos, Entity entity, Minecraft mc, ProfilerFiller profiler)
    {
        if (this.toggle == RendererToggle.OVERLAY_RANDOM_TICKS_PLAYER)
        {
            this.pos = entity.position();
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

            List<AABB> boxes = new ArrayList<>();

            for (Direction side : HORIZONTALS)
            {
                ChunkPos posAdj = new ChunkPos(pos.x + side.getStepX(), pos.z + side.getStepZ());

                if (!chunks.contains(posAdj))
                {
                    AABB bb = this.calculateChunkEdge(pos, side, entity.level());

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
    public void render(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        this.allocateBuffers();
        this.renderQuads(cameraPos, mc, profiler);
        this.renderOutlines(cameraPos, mc, profiler);
    }

    private void renderQuads(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null || mc.player == null)
        {
            return;
        }

        profiler.push("random_tick_quads");
        final Color4f color = this.toggle == RendererToggle.OVERLAY_RANDOM_TICKS_PLAYER ?
                              Configs.Colors.RANDOM_TICKS_PLAYER_OVERLAY_COLOR.getColor() :
                              Configs.Colors.RANDOM_TICKS_FIXED_OVERLAY_COLOR.getColor();

        RenderObjectVbo ctx = this.renderObjects.getFirst();
        BufferBuilder builder = ctx.start(() -> "minihud:random_tick/quads", MaLiLibPipelines.MINIHUD_SHAPE_OFFSET_NO_CULL);

        this.chunkMap.forEach(
                (pos, boxes) ->
                {
                    for (AABB bb : boxes)
                    {
                        RenderUtils.renderWallQuads(bb, cameraPos, color, builder);
                    }
                });

        try
        {
            MeshData meshData = builder.build();

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
            MiniHUD.LOGGER.error("OverlayRendererRandomTickableChunks#renderQuads(): Exception; {}", err.getMessage());
        }

        profiler.pop();
    }

    private void renderOutlines(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null || mc.player == null)
        {
            return;
        }

        profiler.push("random_tick_outlines");
        Color4f color = this.toggle == RendererToggle.OVERLAY_RANDOM_TICKS_PLAYER ?
                              Configs.Colors.RANDOM_TICKS_PLAYER_OVERLAY_COLOR.getColor() :
                              Configs.Colors.RANDOM_TICKS_FIXED_OVERLAY_COLOR.getColor();

        final Color4f colorSolid = Color4f.fromColor(color, 0xFF);

        RenderObjectVbo ctx = this.renderObjects.get(1);
        BufferBuilder builder = ctx.start(() -> "minihud:random_tick/outlines", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH);

        this.chunkMap.forEach(
                (pos, boxes) ->
                {
                    for (AABB bb : boxes)
                    {
                        RenderUtils.renderWallOutlines(bb, 16, 16, true, cameraPos, colorSolid, this.glLineWidth, builder);
                    }
                });

        try
        {
            MeshData meshData = builder.build();

            if (meshData != null)
            {
                ctx.upload(meshData, false);
                meshData.close();
            }
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererRandomTickableChunks#renderOutlines(): Exception; {}", err.getMessage());
        }

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

    protected Set<ChunkPos> getRandomTickableChunks(Vec3 posCenter)
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

    private @Nullable AABB calculateChunkEdge(ChunkPos pos, Direction side, Level world)
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

        int minY = world != null ? world.getMinY() : -64;
        int maxY = world != null ? world.getMaxY() + 1 : 320;

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
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

        if (!this.pos.equals(Vec3.ZERO))
        {
            obj.add("pos", JsonUtils.vec3dToJson(this.pos));
        }

        return obj;
    }

    @Override
    public void fromJson(JsonObject obj)
    {
        if (obj.has("pos"))
        {
            Vec3 pos = JsonUtils.vec3dFromJson(obj, "pos");

            if (pos != null && this.toggle == RendererToggle.OVERLAY_RANDOM_TICKS_FIXED)
            {
                newPos = pos;
            }
        }
    }
}
