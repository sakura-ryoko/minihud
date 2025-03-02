package fi.dy.masa.minihud.renderer;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import com.google.gson.JsonObject;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;

public class OverlayRendererRandomTickableChunks extends OverlayRendererBase
{
    protected static boolean needsUpdate = true;
    @Nullable public static Vec3d newPos;
    private static final Direction[] HORIZONTALS = new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };

    protected final RendererToggle toggle;
    protected Vec3d pos = Vec3d.ZERO;
    protected double minX;
    protected double minZ;
    protected double maxX;
    protected double maxZ;

    @Override
    public String getName()
    {
        return "RandomTickableChunks";
    }

    public static void setNeedsUpdate()
    {
        needsUpdate = true;
    }

    public OverlayRendererRandomTickableChunks(RendererToggle toggle)
    {
        this.toggle = toggle;
    }

    @Override
    public boolean shouldRender(MinecraftClient mc)
    {
        return this.toggle.getBooleanValue();
    }

    @Override
    public boolean needsUpdate(Entity entity, MinecraftClient mc)
    {
        if (needsUpdate)
        {
            return true;
        }

        if (this.toggle == RendererToggle.OVERLAY_RANDOM_TICKS_FIXED)
        {
            return newPos != null;
        }
        // Player-following renderer
        else if (this.toggle == RendererToggle.OVERLAY_RANDOM_TICKS_PLAYER)
        {
            return entity.getX() != this.pos.x || entity.getZ() != this.pos.z;
        }

        return false;
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity, MinecraftClient mc)
    {
        if (this.toggle == RendererToggle.OVERLAY_RANDOM_TICKS_PLAYER)
        {
            this.pos = entity.getPos();
        }
        else if (newPos != null)
        {
            this.pos = newPos;
            newPos = null;
        }

        final Color4f color = this.toggle == RendererToggle.OVERLAY_RANDOM_TICKS_PLAYER ?
                Configs.Colors.RANDOM_TICKS_PLAYER_OVERLAY_COLOR.getColor() :
                Configs.Colors.RANDOM_TICKS_FIXED_OVERLAY_COLOR.getColor();

        RenderObjectBase renderQuads = this.renderObjects.get(0);
        RenderObjectBase renderLines = this.renderObjects.get(1);
        /*
        BUFFER_1 = TESSELLATOR_1.begin(renderQuads.getGlMode(), VertexFormats.POSITION_COLOR);
        BUFFER_2 = TESSELLATOR_2.begin(renderLines.getGlMode(), VertexFormats.POSITION_COLOR);
         */

        BufferBuilder builder1 = CONTEXT_1.startNoShader(this::getName, VertexFormats.POSITION_COLOR, renderQuads.getGlMode(), GlUsage.STATIC_WRITE);
        BufferBuilder builder2 = CONTEXT_2.startNoShader(this::getName, VertexFormats.POSITION_COLOR, renderLines.getGlMode(), GlUsage.STATIC_WRITE);
        CONTEXT_1.setShader(MaLiLibPipelines.POSITION_COLOR_SIMPLE);
        CONTEXT_2.setShader(MaLiLibPipelines.POSITION_COLOR_SIMPLE);

        Set<ChunkPos> chunks = this.getRandomTickableChunks(this.pos);

        for (ChunkPos pos : chunks)
        {
            this.renderChunkEdgesIfApplicable(cameraPos, pos, chunks, entity.getEntityWorld(), color, builder1, builder2);
        }

        CONTEXT_1 = CONTEXT_1.setBuilder(builder1);
        CONTEXT_2 = CONTEXT_2.setBuilder(builder2);

        renderQuads.uploadData(builder1);
        renderLines.uploadData(builder2);

        needsUpdate = false;
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

        return set;
    }

    protected void renderChunkEdgesIfApplicable(Vec3d cameraPos, ChunkPos pos, Set<ChunkPos> chunks, World world, Color4f color, BufferBuilder builder1, BufferBuilder builder2)
    {
        for (Direction side : HORIZONTALS)
        {
            ChunkPos posAdj = new ChunkPos(pos.x + side.getOffsetX(), pos.z + side.getOffsetZ());

            if (chunks.contains(posAdj) == false)
            {
                this.renderChunkEdge(pos, side, cameraPos, color, world, builder1, builder2);
            }
        }
    }

    private void renderChunkEdge(ChunkPos pos, Direction side, Vec3d cameraPos, Color4f color, World world, BufferBuilder builder1, BufferBuilder builder2)
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
                return;
        }

        int minY = world != null ? world.getBottomY() : -64;
        int maxY = world != null ? world.getTopYInclusive() + 1 : 320;

        RenderUtils.renderWallWithLines(minX, minY, minZ, maxX, maxY, maxZ, 16, 16, true, cameraPos, color, builder1, builder2);
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
