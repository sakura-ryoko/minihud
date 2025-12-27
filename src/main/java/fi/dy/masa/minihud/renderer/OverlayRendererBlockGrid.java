package fi.dy.masa.minihud.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.malilib.util.PositionUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.util.BlockGridMode;

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
    public boolean shouldRender(MinecraftClient mc)
    {
        return RendererToggle.OVERLAY_BLOCK_GRID.getBooleanValue();
    }

    @Override
    public boolean needsUpdate(Entity entity, MinecraftClient mc)
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
    public void update(Vec3d cameraPos, Entity entity, MinecraftClient mc, Profiler profiler)
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
        this.renderObjects.add(this.createQuadsVbo());
    }

    @Override
    public void render(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        this.allocateBuffers();
        this.renderOutlines(cameraPos, mc, profiler);
    }

    private void renderOutlines(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        if (mc.world == null || mc.player == null ||
            this.lastUpdatePos == null || this.cameraEntity == null)
        {
            return;
        }

        profiler.push("block_grid_outlines");
        BlockGridMode mode = (BlockGridMode) Configs.Generic.BLOCK_GRID_OVERLAY_MODE.getOptionListValue();
        int radius = Configs.Generic.BLOCK_GRID_OVERLAY_RADIUS.getIntegerValue();
        Color4f color = Configs.Colors.BLOCK_GRID_OVERLAY_COLOR.getColor();

        RenderObjectVbo ctx = this.renderObjects.getFirst();
        BufferBuilder builder = ctx.start(
		        () -> "minihud:block_grid/outlines",
                VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR, ShaderProgramKeys.POSITION_COLOR, GlUsage.STATIC_WRITE
        );

        switch (mode)
        {
            case ALL:
                this.renderLinesAll(cameraPos, this.lastUpdatePos, radius, color, builder);
                break;
            case NON_AIR:
                this.renderLinesNonAir(cameraPos, this.cameraEntity.getEntityWorld(), this.lastUpdatePos, radius, color, builder);
                break;
            case ADJACENT:
                this.renderLinesAdjacentToNonAir(cameraPos, this.cameraEntity.getEntityWorld(), this.lastUpdatePos, radius, color, builder);
                break;
        }

        try
        {
            BuiltBuffer meshData = builder.endNullable();

            if (meshData != null)
            {
                ctx.upload(meshData);
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

    protected void renderLinesAll(Vec3d cameraPos, BlockPos center, int radius, Color4f color,
//                                  BufferBuilder buffer, MatrixStack.Entry e)
                                  BufferBuilder buffer)
    {
        final int startX = center.getX() - radius - MathHelper.floor(cameraPos.x);
        final int startY = center.getY() - radius - MathHelper.floor(cameraPos.y);
        final int startZ = center.getZ() - radius - MathHelper.floor(cameraPos.z);
        final int endX = center.getX() + radius - MathHelper.ceil(cameraPos.x);
        final int endY = center.getY() + radius - MathHelper.ceil(cameraPos.y);
        final int endZ = center.getZ() + radius - MathHelper.ceil(cameraPos.z);

        for (int x = startX; x <= endX; x++)
        {
            for (int y = startY; y <= endY; y++)
            {
                buffer.vertex((float) x, (float) y, (float) startZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex((float) x, (float) y, (float) endZ).color(color.r, color.g, color.b, color.a);
            }
        }

        for (int x = startX; x <= endX; x++)
        {
            for (int z = startZ; z <= endZ; z++)
            {
                buffer.vertex((float) x, (float) startY, (float) z).color(color.r, color.g, color.b, color.a);
                buffer.vertex((float) x, (float) endY, (float) z).color(color.r, color.g, color.b, color.a);
            }
        }

        for (int z = startZ; z <= endZ; z++)
        {
            for (int y = startY; y <= endY; y++)
            {
                buffer.vertex((float) startX, (float) y, (float) z).color(color.r, color.g, color.b, color.a);
                buffer.vertex((float) endX, (float) y, (float) z).color(color.r, color.g, color.b, color.a);
            }
        }
    }

    protected void renderLinesNonAir(Vec3d cameraPos, World world, BlockPos center, int radius, Color4f color,
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
        WorldChunk chunk = world.getChunk(lastCX, lastCZ);
        BlockPos.Mutable posMutable = new BlockPos.Mutable();

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
                    if (y > chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, x, z))
                    {
                        break;
                    }

                    posMutable.set(x, y, z);

                    if (!chunk.getBlockState(posMutable).isAir())
                    {
                        fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(posMutable, cameraPos, color, 0.001, buffer);
                    }
                }
            }
        }
    }

    protected void renderLinesAdjacentToNonAir(Vec3d cameraPos, World world, BlockPos center, int radius, Color4f color,
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
        WorldChunk chunk = world.getChunk(lastCX, lastCZ);
        BlockPos.Mutable posMutable = new BlockPos.Mutable();
        BlockPos.Mutable posMutable2 = new BlockPos.Mutable();

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
                                    posMutable.getX() + side.getOffsetX(),
                                    posMutable.getY() + side.getOffsetY(),
                                    posMutable.getZ() + side.getOffsetZ());

                            if (!chunk.getBlockState(posMutable2).isAir())
                            {
                                fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(posMutable, cameraPos, color, 0.001, buffer);
                                break;
                            }
                        }

                        for (Direction side : PositionUtils.HORIZONTAL_DIRECTIONS)
                        {
                            posMutable2.set(
                                    posMutable.getX() + side.getOffsetX(),
                                    posMutable.getY() + side.getOffsetY(),
                                    posMutable.getZ() + side.getOffsetZ());

                            if (!world.isAir(posMutable2))
                            {
                                fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(posMutable, cameraPos, color, 0.001, buffer);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
}
