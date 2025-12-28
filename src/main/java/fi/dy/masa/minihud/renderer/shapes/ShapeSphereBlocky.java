package fi.dy.masa.minihud.renderer.shapes;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.position.PositionUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.renderer.RenderObjectVbo;
import fi.dy.masa.minihud.renderer.RenderUtils;
import fi.dy.masa.minihud.util.shape.SphereUtils;

public class ShapeSphereBlocky extends ShapeCircleBase
{
    private boolean hasData;

    public ShapeSphereBlocky()
    {
        this(ShapeType.SPHERE_BLOCKY, Configs.Colors.SHAPE_SPHERE_BLOCKY.getColor(), 16);
    }

    public ShapeSphereBlocky(ShapeType type, Color4f color, double radius)
    {
        super(type, color, radius);
        this.hasData = false;
        this.useCulling = false;
    }

    @Override
    public void update(Vec3 cameraPos, Entity entity, Minecraft mc, ProfilerFiller profiler)
    {
        this.hasData = true;
        this.render(cameraPos, mc, profiler);
        this.needsUpdate = false;
    }

    @Override
    public boolean hasData()
    {
        return this.hasData;
    }

    @Override
    public void render(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        this.allocateBuffers(this.renderLines);
        this.renderQuads(cameraPos, mc, profiler);

        if (this.renderLines)
        {
            this.renderOutlines(cameraPos, mc, profiler);
        }
    }

    private void renderQuads(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null || mc.player == null)
        {
            return;
        }

        profiler.push("sphere_blocky_quads");

        RenderObjectVbo ctx = this.renderObjects.getFirst();
        BufferBuilder builder = ctx.start(() -> "minihud:sphere_blocky/quads", this.renderThroughShape ? MaLiLibPipelines.MINIHUD_SHAPE_NO_DEPTH_OFFSET : MaLiLibPipelines.MINIHUD_SHAPE_OFFSET_NO_CULL);

        this.renderSphereShapeQuads(cameraPos, builder);

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
            MiniHUD.LOGGER.error("ShapeSphereBlocky#renderQuads(): Exception; {}", err.getMessage());
        }

        profiler.pop();
    }

    private void renderOutlines(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null || mc.player == null || !this.renderLines)
        {
            return;
        }

        profiler.push("sphere_blocky_outlines");

        RenderObjectVbo ctx = this.renderObjects.get(1);
        BufferBuilder builder = ctx.start(() -> "minihud:sphere_blocky/outlines", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH);

        this.renderSphereShapeOutlines(cameraPos, this.glLineWidth, builder);

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
            MiniHUD.LOGGER.error("ShapeSphereBlocky#renderOutlines(): Exception; {}", err.getMessage());
        }

        profiler.pop();
    }

    @Override
    public void reset()
    {
        super.reset();
        this.hasData = false;
    }

    protected SphereUtils.RingPositionTest getPositionTest()
    {
        return (x, y, z, dir) -> SphereUtils.isPositionInsideOrClosestToRadiusOnBlockRing(
                                    x, y, z, this.getEffectiveCenter(), this.getSquaredRadius(), Direction.EAST);
    }

    protected double getTotalRadius()
    {
        return this.getRadius();
    }

    protected void renderSphereShapeQuads(Vec3 cameraPos, BufferBuilder builder)
    {
        SphereUtils.RingPositionTest test = this.getPositionTest();
        LongOpenHashSet positions = new LongOpenHashSet();
        Consumer<BlockPos.MutableBlockPos> positionConsumer = this.getPositionCollector(positions);
        BlockPos centerPos = this.getCenterBlock();
        double expand = 0;

        SphereUtils.collectSpherePositions(positionConsumer, test, centerPos, (int) this.getTotalRadius());

        if (this.getCombineQuads())
        {
            List<SideQuad> quads = SphereUtils.buildSphereShellToQuads(positions, this.mainAxis.getAxis(),
                                                                       test, this.renderType, this.layerRange);
            RenderUtils.renderQuads(quads, this.color, expand, cameraPos, builder);
        }
        else
        {
            RenderUtils.renderCircleBlockPositions(positions, PositionUtils.ALL_DIRECTIONS, test, this.renderType,
                                                   this.layerRange, this.color, expand, cameraPos, builder);
        }
    }

    protected void renderSphereShapeOutlines(Vec3 cameraPos, float lineWidth, BufferBuilder builder)
    {
        SphereUtils.RingPositionTest test = this.getPositionTest();
        LongOpenHashSet positions = new LongOpenHashSet();
        Consumer<BlockPos.MutableBlockPos> positionConsumer = this.getPositionCollector(positions);
        BlockPos centerPos = this.getCenterBlock();
        double expand = 0;

        SphereUtils.collectSpherePositions(positionConsumer, test, centerPos, (int) this.getTotalRadius());

        if (this.getCombineQuads())
        {
            List<SideQuad> quads = SphereUtils.buildSphereShellToQuads(positions, this.mainAxis.getAxis(),
                                                                       test, this.renderType, this.layerRange);
            RenderUtils.renderQuadLines(quads, this.colorLines, expand, cameraPos, lineWidth, builder);
        }
        else
        {
            RenderUtils.renderCircleBlockOutlines(positions, PositionUtils.ALL_DIRECTIONS, test, this.renderType,
                                                  this.layerRange, this.colorLines, expand, cameraPos, lineWidth, builder);
        }
    }
}
