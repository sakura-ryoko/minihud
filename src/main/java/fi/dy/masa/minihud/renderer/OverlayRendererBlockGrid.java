package fi.dy.masa.minihud.renderer;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlBufferTarget;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.ShaderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.position.PositionUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.util.BlockGridMode;

public class OverlayRendererBlockGrid extends OverlayRendererBase
{
    public static final OverlayRendererBlockGrid INSTANCE = new OverlayRendererBlockGrid();
    private final List<BlockPos> positions;
    private boolean hasData;

    public OverlayRendererBlockGrid()
    {
        this.positions = new ArrayList<>();
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
        int radius = Configs.Generic.BLOCK_GRID_OVERLAY_RADIUS.getIntegerValue();

        BlockGridMode mode = (BlockGridMode) Configs.Generic.BLOCK_GRID_OVERLAY_MODE.getOptionListValue();

        switch (mode)
        {
            case ALL:
                this.calculateLinesAll(cameraPos, this.lastUpdatePos, radius);
                break;
            case NON_AIR:
                this.calculateLinesNonAir(entity.getEntityWorld(), this.lastUpdatePos, radius);
                break;
            case ADJACENT:
                this.calculateLinesAdjacentToNonAir(entity.getEntityWorld(), this.lastUpdatePos, radius);
                break;
        }

        if (!this.positions.isEmpty())
        {
            this.hasData = true;
            this.render(cameraPos, mc, profiler);
        }
    }

    @Override
    public boolean hasData()
    {
        return this.hasData && !this.positions.isEmpty();
    }

    @Override
    protected void allocateBuffers()
    {
        this.clearBuffers();
        this.renderObjects.add(new RenderObjectVbo(() -> this.getName()+" Lines", ShaderPipelines.LINES, GlUsage.STATIC_WRITE));
    }

    @Override
    public void render(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        this.allocateBuffers();
        this.renderOutlines(cameraPos, mc, profiler);
    }

    private void renderOutlines(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        profiler.push("block_grid_outlines");
        BlockGridMode mode = (BlockGridMode) Configs.Generic.BLOCK_GRID_OVERLAY_MODE.getOptionListValue();
        Color4f color = Configs.Colors.BLOCK_GRID_OVERLAY_COLOR.getColor();

        RenderObjectVbo ctx = this.renderObjects.getFirst();
        BufferBuilder builder = ctx.start(() -> "BlockGrid Lines", ShaderPipelines.LINES, GlUsage.STATIC_WRITE);
        MatrixStack matrices = new MatrixStack();

        matrices.push();

        for (BlockPos pos : this.positions)
        {
            switch (mode)
            {
                case ALL -> this.renderBatchedLinesAll(color, builder, matrices.peek());
                case NON_AIR, ADJACENT -> fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(pos, cameraPos, color, 0.001, builder, matrices);
            }
        }

        try
        {
            ctx.upload(builder.endNullable(), GlBufferTarget.VERTICES);
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererBlockGrid#renderOutlines(): Exception; {}", err.getMessage());
        }

        matrices.pop();
        profiler.pop();
    }

    @Override
    public void reset()
    {
        super.reset();
        this.positions.clear();
        this.hasData = false;
    }

    protected void calculateLinesAll(Vec3d cameraPos, BlockPos center, int radius)
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
                this.positions.add(new BlockPos(x, y, startZ));
                this.positions.add(new BlockPos(x, y, endZ));
            }
        }

        for (int x = startX; x <= endX; x++)
        {
            for (int z = startZ; z <= endZ; z++)
            {
                this.positions.add(new BlockPos(x, startY, z));
                this.positions.add(new BlockPos(x, endY, z));
            }
        }

        for (int z = startZ; z <= endZ; z++)
        {
            for (int y = startY; y <= endY; y++)
            {
                this.positions.add(new BlockPos(startX, y, z));
                this.positions.add(new BlockPos(endX, y, z));
            }
        }
    }

    private void renderBatchedLinesAll(Color4f color, BufferBuilder buffer, MatrixStack.Entry e)
    {
        for (BlockPos pos : this.positions)
        {
            float x = pos.getX();
            float y = pos.getY();
            float z = pos.getZ();

            buffer.vertex(e, x, y, z).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        }
    }

    protected void calculateLinesNonAir(World world, BlockPos center, int radius)
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
                        this.positions.add(posMutable);
                    }
                }
            }
        }
    }

    protected void calculateLinesAdjacentToNonAir(World world, BlockPos center, int radius)
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
                                this.positions.add(posMutable);
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
                                this.positions.add(posMutable);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
}
