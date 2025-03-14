package fi.dy.masa.minihud.renderer;

import java.util.List;
import java.util.function.Consumer;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ConduitBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;

import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.position.PositionUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.renderer.shapes.SideQuad;
import fi.dy.masa.minihud.util.ConduitExtra;
import fi.dy.masa.minihud.util.ShapeRenderType;
import fi.dy.masa.minihud.util.shape.SphereUtils;

public class OverlayRendererConduitRange extends BaseBlockRangeOverlay<ConduitBlockEntity>
{
    public static final OverlayRendererConduitRange INSTANCE = new OverlayRendererConduitRange();

    private final ShapeRenderType renderType;
    private final LayerRange layerRange;
    private final Direction.Axis quadAxis;
    private boolean combineQuads;

    private LongOpenHashSet positions;
    private SphereUtils.RingPositionTest test;
    private List<SideQuad> quads;

    public OverlayRendererConduitRange()
    {
        super(RendererToggle.OVERLAY_CONDUIT_RANGE, BlockEntityType.CONDUIT, ConduitBlockEntity.class);
        this.quadAxis = Direction.UP.getAxis();
        this.renderType = ShapeRenderType.OUTER_EDGE;
        this.layerRange = new LayerRange(null);
        this.positions = new LongOpenHashSet();
        this.test = null;
        this.quads = null;
    }

    @Override
    public String getName()
    {
        return "ConduitRange";
    }

//    @Override
//    protected void allocateBuffers()
//    {
//        this.clearBuffers();
//        this.renderObjects.add(new RenderObjectVbo(() -> this.getName()+" Quads", MaLiLibPipelines.POSITION_COLOR_SIMPLE, GlUsage.STATIC_WRITE));
//    }

    @Override
    protected void renderBlockRange(World world, BlockPos pos, ConduitBlockEntity be, Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        if (!be.isActive())
        {
            return;
        }

        int range = ((ConduitExtra) be).minihud$getStoredActivatingBlockCount() / 7 * 16;

        this.positions.clear();
        Consumer<BlockPos.Mutable> positionCollector = (p) -> this.positions.add(p.asLong());
        this.test = this.getPositionTest(pos, range);
        SphereUtils.collectSpherePositions(positionCollector, this.test, pos, range);

        this.combineQuads = true;
        this.renderThrough = Configs.Generic.SHAPE_RENDER_THROUGH.getBooleanValue();

        if (this.combineQuads)
        {
            this.quads = SphereUtils.buildSphereShellToQuads(this.positions, this.quadAxis, this.test, this.renderType, this.layerRange);
        }

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

        Color4f color = Configs.Colors.CONDUIT_RANGE_OVERLAY_COLOR.getColor();

        // MaLiLibPipelines.POSITION_COLOR_MASA_LESSER_DEPTH
        profiler.push("conduit_quads");
        RenderObjectVbo ctx = this.renderObjects.getFirst();
        BufferBuilder builder = ctx.start(() -> "Conduit Quads", this.renderThrough ? MaLiLibPipelines.getPositionColorSimple() : MaLiLibPipelines.POSITION_COLOR_MASA_LESSER_DEPTH, BufferUsage.STATIC_WRITE);
        MatrixStack matrices = new MatrixStack();

        matrices.push();

        if (this.combineQuads)
        {
            RenderUtils.renderQuads(this.quads, color, 0, cameraPos, builder, matrices.peek());
        }
        else
        {
            RenderUtils.renderCircleBlockPositions(this.positions, PositionUtils.ALL_DIRECTIONS,
                                                   this.test, this.renderType,
                                                   this.layerRange, color, 0,
                                                   cameraPos, builder, matrices.peek());
        }

        try
        {
            ctx.upload(builder.endNullable(), BufferType.VERTICES);
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererConduitRange#renderQuads(): Exception; {}", err.getMessage());
        }

        matrices.pop();
        profiler.pop();

    }

    private void renderOutlines(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        if (mc.world == null || mc.player == null || !Configs.Generic.SHAPE_RENDER_OUTLINES.getBooleanValue())
        {
            return;
        }

        Color4f color = Configs.Colors.CONDUIT_RANGE_OVERLAY_COLOR.getColor();

        profiler.push("conduit_outlines");
        RenderObjectVbo ctx = this.renderObjects.get(1);
        BufferBuilder builder = ctx.start(() -> "Conduit Outlines", ShaderPipelines.LINES, BufferUsage.STATIC_WRITE);
        MatrixStack matrices = new MatrixStack();

        matrices.push();

        if (this.combineQuads)
        {
            RenderUtils.renderQuadLines(this.quads, color, 0, cameraPos, builder, matrices.peek());
        }
        else
        {
            RenderUtils.renderCircleBlockOutlines(this.positions, PositionUtils.ALL_DIRECTIONS,
                                                  this.test, this.renderType,
                                                  this.layerRange, Color4f.fromColor(color.intValue, 1f), 0,
                                                  cameraPos, builder, matrices.peek());
        }

        try
        {
            ctx.upload(builder.endNullable(), BufferType.VERTICES);
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererConduitRange#renderBlockRange(): Exception; {}", err.getMessage());
        }

        matrices.pop();
        profiler.pop();

    }

    @Override
    public void reset()
    {
        super.reset();
        this.positions = new LongOpenHashSet();
        this.test = null;
        this.quads = null;
    }

    protected SphereUtils.RingPositionTest getPositionTest(BlockPos centerPos, int range)
    {
        Vec3d center = new Vec3d(centerPos.getX() + 0.5, centerPos.getY() + 0.5, centerPos.getZ() + 0.5);
        double squareRange = range * range;

        return (x, y, z, dir) -> SphereUtils.isPositionInsideOrClosestToRadiusOnBlockRing(
                x, y, z, center, squareRange, Direction.EAST);
    }
}
