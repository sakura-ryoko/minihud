package fi.dy.masa.minihud.renderer.shapes;

import java.util.List;
import java.util.function.Consumer;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;

import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.render.RenderContext;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.position.PositionUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;
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
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity, MinecraftClient mc)
    {
        this.hasData = true;
        this.needsUpdate = false;
    }

    @Override
    public boolean hasData()
    {
        return this.hasData;
    }

    @Override
    public void render(Camera camera, Matrix4f matrix4f, Matrix4f projMatrix, MinecraftClient mc, Profiler profiler)
    {
        if (this.hasData())
        {
            this.renderQuads(camera, matrix4f, projMatrix, mc, profiler);
        }
    }

    private void renderQuads(Camera camera, Matrix4f matrix4f, Matrix4f projMatrix, MinecraftClient mc, Profiler profiler)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        profiler.push("sphere_blocky_quads");
        Vec3d cameraPos = camera.getPos();

        RenderContext ctx = new RenderContext(() -> "Sphere Blocky Quads", MaLiLibPipelines.POSITION_COLOR_SIMPLE, GlUsage.STATIC_WRITE);
        BufferBuilder builder = ctx.getBuilder();
        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        Vec3d updatePos = this.getUpdatePosition();

        this.preRender();
        matrix4fstack.pushMatrix();
        matrix4fstack.translate((float) (updatePos.x - cameraPos.x), (float) (updatePos.y - cameraPos.y), (float) (updatePos.z - cameraPos.z));

        this.renderSphereShape(cameraPos, builder);

        try
        {
            ctx.drawColor(builder.endNullable());
            ctx.close();
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("ShapeSphereBlocky#renderQuads(): Exception; {}", err.getMessage());
        }

        this.postRender();
        matrix4fstack.popMatrix();
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
