package fi.dy.masa.minihud.renderer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.position.PositionUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.util.BlockGridMode;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class OverlayRendererBlockGrid extends OverlayRendererBase
{
    public static final OverlayRendererBlockGrid INSTANCE = new OverlayRendererBlockGrid();
    private Entity cameraEntity;
    private boolean hasData;

    public OverlayRendererBlockGrid()
    {
        this.cameraEntity = null;
        this.hasData = false;
        this.useCulling = true;
        this.renderThrough = false;
    }

    @Override
    public String getName()
    {
        return "BlockGrid";
    }

    @Override
    public boolean shouldRender(Minecraft mc)
    {
        return RendererToggle.OVERLAY_BLOCK_GRID.getBooleanValue();
    }

    @Override
    public boolean needsUpdate(Entity entity, Minecraft mc)
    {
        if (this.lastUpdatePos == null)
        {
            return true;
        }

        return Math.abs(entity.getX() - this.lastUpdatePos.getX()) > 8 ||
               Math.abs(entity.getY() - this.lastUpdatePos.getY()) > 8 ||
               Math.abs(entity.getZ() - this.lastUpdatePos.getZ()) > 8;
    }

    @Override
    public void update(Vec3 cameraPos, Entity entity, Minecraft mc, ProfilerFiller profiler)
    {
        if (RendererToggle.OVERLAY_BLOCK_GRID.getBooleanValue())
        {
            this.cameraEntity = entity;
            this.hasData = true;
            this.render(cameraPos, mc, profiler);
        }
    }

    @Override
    public boolean hasData()
    {
        return this.hasData;
    }

    @Override
    protected void allocateBuffers()
    {
        this.clearBuffers();
        this.renderObjects.add(new RenderObjectVbo(() -> this.getName()+" Lines", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH));
    }

    @Override
    public void render(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        this.allocateBuffers();
        this.renderOutlines(cameraPos, mc, profiler);
    }

    private void renderOutlines(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null || mc.player == null ||
            this.lastUpdatePos == null || this.cameraEntity == null)
        {
            return;
        }

        profiler.push("block_grid_outlines");
        BlockGridMode mode = (BlockGridMode) Configs.Generic.BLOCK_GRID_OVERLAY_MODE.getOptionListValue();
        int radius = Configs.Generic.BLOCK_GRID_OVERLAY_RADIUS.getIntegerValue();
        Color4f color = Configs.Colors.BLOCK_GRID_OVERLAY_COLOR.getColor();

        RenderObjectVbo ctx = this.renderObjects.getFirst();
        BufferBuilder builder = ctx.start(() -> "minihud:block_grid/outlines", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH);

        switch (mode)
        {
            case ALL:
                this.renderLinesAll(cameraPos, this.lastUpdatePos, radius, color, this.glLineWidth, builder);
                break;
            case NON_AIR:
                this.renderLinesNonAir(cameraPos, this.cameraEntity.level(), this.lastUpdatePos, radius, color, this.glLineWidth, builder);
                break;
            case ADJACENT:
                this.renderLinesAdjacentToNonAir(cameraPos, this.cameraEntity.level(), this.lastUpdatePos, radius, color, this.glLineWidth, builder);
                break;
        }

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
            MiniHUD.LOGGER.error("OverlayRendererBlockGrid#renderOutlines(): Exception; {}", err.getMessage());
        }

        profiler.pop();
    }

    @Override
    public void reset()
    {
        super.reset();
        this.cameraEntity = null;
        this.hasData = false;
    }

    protected void renderLinesAll(Vec3 cameraPos, BlockPos center, int radius, Color4f color,
								  float lineWidth,
                                  BufferBuilder buffer)
    {
        final int startX = center.getX() - radius - Mth.floor(cameraPos.x);
        final int startY = center.getY() - radius - Mth.floor(cameraPos.y);
        final int startZ = center.getZ() - radius - Mth.floor(cameraPos.z);
        final int endX = center.getX() + radius - Mth.ceil(cameraPos.x);
        final int endY = center.getY() + radius - Mth.ceil(cameraPos.y);
        final int endZ = center.getZ() + radius - Mth.ceil(cameraPos.z);

        for (int x = startX; x <= endX; x++)
        {
            for (int y = startY; y <= endY; y++)
            {
                buffer.addVertex((float) x, (float) y, (float) startZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex((float) x, (float) y, (float) endZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
            }
        }

        for (int x = startX; x <= endX; x++)
        {
            for (int z = startZ; z <= endZ; z++)
            {
                buffer.addVertex((float) x, (float) startY, (float) z).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex((float) x, (float) endY, (float) z).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
            }
        }

        for (int z = startZ; z <= endZ; z++)
        {
            for (int y = startY; y <= endY; y++)
            {
                buffer.addVertex((float) startX, (float) y, (float) z).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex((float) endX, (float) y, (float) z).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
            }
        }
    }

    protected void renderLinesNonAir(Vec3 cameraPos, Level world, BlockPos center, int radius, Color4f color,
                                     float lineWidth,
                                     BufferBuilder buffer)
    {
        final int startX = center.getX() - radius;
        final int startY = center.getY() - radius;
        final int startZ = center.getZ() - radius;
        final int endX = center.getX() + radius;
        final int endY = center.getY() + radius;
        final int endZ = center.getZ() + radius;
        int lastCX = startX >> 4;
        int lastCZ = startZ >> 4;
        LevelChunk chunk = world.getChunk(lastCX, lastCZ);
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();

        for (int x = startX; x <= endX; ++x)
        {
            for (int z = startZ; z <= endZ; ++z)
            {
                int cx = x >> 4;
                int cz = z >> 4;

                if (cx != lastCX || cz != lastCZ)
                {
                    chunk = world.getChunk(cx, cz);
                    lastCX = cx;
                    lastCZ = cz;
                }

                for (int y = startY; y <= endY; ++y)
                {
                    if (y > chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z))
                    {
                        break;
                    }

                    posMutable.set(x, y, z);

                    if (!chunk.getBlockState(posMutable).isAir())
                    {
                        fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(posMutable, cameraPos, color, 0.001, lineWidth, buffer);
                    }
                }
            }
        }
    }

    protected void renderLinesAdjacentToNonAir(Vec3 cameraPos, Level world, BlockPos center, int radius, Color4f color,
                                               float lineWidth,
                                               BufferBuilder buffer)
    {
        final int startX = center.getX() - radius;
        final int startY = center.getY() - radius;
        final int startZ = center.getZ() - radius;
        final int endX = center.getX() + radius;
        final int endY = center.getY() + radius;
        final int endZ = center.getZ() + radius;
        int lastCX = startX >> 4;
        int lastCZ = startZ >> 4;
        LevelChunk chunk = world.getChunk(lastCX, lastCZ);
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos posMutable2 = new BlockPos.MutableBlockPos();

        for (int x = startX; x <= endX; ++x)
        {
            for (int z = startZ; z <= endZ; ++z)
            {
                int cx = x >> 4;
                int cz = z >> 4;

                if (cx != lastCX || cz != lastCZ)
                {
                    chunk = world.getChunk(cx, cz);
                    lastCX = cx;
                    lastCZ = cz;
                }

                for (int y = startY; y <= endY; ++y)
                {
                    posMutable.set(x, y, z);

                    if (chunk.getBlockState(posMutable).isAir())
                    {
                        for (Direction side : PositionUtils.VERTICAL_DIRECTIONS)
                        {
                            posMutable2.set(
                                    posMutable.getX() + side.getStepX(),
                                    posMutable.getY() + side.getStepY(),
                                    posMutable.getZ() + side.getStepZ());

                            if (!chunk.getBlockState(posMutable2).isAir())
                            {
                                fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(posMutable, cameraPos, color, 0.001, lineWidth, buffer);
                                break;
                            }
                        }

                        for (Direction side : PositionUtils.HORIZONTAL_DIRECTIONS)
                        {
                            posMutable2.set(
                                    posMutable.getX() + side.getStepX(),
                                    posMutable.getY() + side.getStepY(),
                                    posMutable.getZ() + side.getStepZ());

                            if (!world.isEmptyBlock(posMutable2))
                            {
                                fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(posMutable, cameraPos, color, 0.001, lineWidth, buffer);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
}
