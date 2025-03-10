package fi.dy.masa.minihud.renderer.shapes;

import java.util.List;
import java.util.function.Consumer;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlBufferTarget;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.ShaderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;

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
        this.useCulling = true;
        this.renderThrough = false;
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity, MinecraftClient mc, Profiler profiler)
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
    protected void allocateBuffers()
    {
        this.clearBuffers();
        this.renderObjects.add(new RenderObjectVbo(() -> this.getName()+" Quads", ShaderPipelines.DEBUG_QUADS, GlUsage.STATIC_WRITE));
    }

    @Override
    public void render(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        this.allocateBuffers();
        this.renderQuads(cameraPos, mc, profiler);
    }

    private void renderQuads(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        profiler.push("sphere_blocky_quads");

        RenderObjectVbo ctx = this.renderObjects.getFirst();
        BufferBuilder builder = ctx.start(() -> "Sphere Blocky Quads", ShaderPipelines.DEBUG_QUADS, GlUsage.STATIC_WRITE);
        MatrixStack matrices = new MatrixStack();

        matrices.push();
        this.renderSphereShape(cameraPos, builder);

        try
        {
            ctx.upload(builder.endNullable(), GlBufferTarget.VERTICES);
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("ShapeSphereBlocky#renderQuads(): Exception; {}", err.getMessage());
        }

        matrices.pop();
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

    protected void renderSphereShape(Vec3d cameraPos, BufferBuilder builder)
    {
        SphereUtils.RingPositionTest test = this.getPositionTest();
        LongOpenHashSet positions = new LongOpenHashSet();
        Consumer<BlockPos.Mutable> positionConsumer = this.getPositionCollector(positions);
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
}
