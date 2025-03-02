package fi.dy.masa.minihud.renderer.shapes;

import java.util.List;
import java.util.function.Consumer;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.position.PositionUtils;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.renderer.RenderObjectBase;
import fi.dy.masa.minihud.renderer.RenderUtils;
import fi.dy.masa.minihud.util.shape.SphereUtils;

public class ShapeSphereBlocky extends ShapeCircleBase
{
    public ShapeSphereBlocky()
    {
        this(ShapeType.SPHERE_BLOCKY, Configs.Colors.SHAPE_SPHERE_BLOCKY.getColor(), 16);
    }

    public ShapeSphereBlocky(ShapeType type, Color4f color, double radius)
    {
        super(type, color, radius);
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity, MinecraftClient mc)
    {
        this.renderSphereShape(cameraPos);
        this.needsUpdate = false;
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

    protected void renderSphereShape(Vec3d cameraPos)
    {
        SphereUtils.RingPositionTest test = this.getPositionTest();
        LongOpenHashSet positions = new LongOpenHashSet();
        Consumer<BlockPos.Mutable> positionConsumer = this.getPositionCollector(positions);
        BlockPos centerPos = this.getCenterBlock();
        double expand = 0;

        SphereUtils.collectSpherePositions(positionConsumer, test, centerPos, (int) this.getTotalRadius());

        RenderObjectBase renderQuads = this.renderObjects.getFirst();
        //BUFFER_1 = TESSELLATOR_1.begin(renderQuads.getGlMode(), VertexFormats.POSITION_COLOR);
        BufferBuilder builder1 = CONTEXT_1.startNoShader(VertexFormats.POSITION_COLOR, renderQuads.getGlMode());
        CONTEXT_1.setShader(MaLiLibPipelines.POSITION_COLOR_SIMPLE);

        if (this.getCombineQuads())
        {
            List<SideQuad> quads = SphereUtils.buildSphereShellToQuads(positions, this.mainAxis.getAxis(),
                                                                       test, this.renderType, this.layerRange);
            RenderUtils.renderQuads(quads, this.color, expand, cameraPos, builder1);
        }
        else
        {
            RenderUtils.renderCircleBlockPositions(positions, PositionUtils.ALL_DIRECTIONS, test, this.renderType,
                                                   this.layerRange, this.color, expand, cameraPos, builder1);
        }

        CONTEXT_1 = CONTEXT_1.setBuilder(builder1);
        renderQuads.uploadData(builder1);
    }
}
