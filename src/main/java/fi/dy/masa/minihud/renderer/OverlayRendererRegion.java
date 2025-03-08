package fi.dy.masa.minihud.renderer;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.ShaderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;

import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.render.RenderContext;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;

public class OverlayRendererRegion extends OverlayRendererBase
{
    public static final OverlayRendererRegion INSTANCE = new OverlayRendererRegion();
    protected boolean needsUpdate = true;
    private Box box;
    private boolean hasData;

    protected OverlayRendererRegion()
    {
        this.box = null;
        this.hasData = false;
    }

    public void setNeedsUpdate()
    {
        this.needsUpdate = true;
    }

    @Override
    public String getName()
    {
        return "Region";
    }

    @Override
    public boolean shouldRender(MinecraftClient mc)
    {
        return RendererToggle.OVERLAY_REGION_FILE.getBooleanValue();
    }

    @Override
    public boolean needsUpdate(Entity entity, MinecraftClient mc)
    {
        if (this.needsUpdate)
        {
            return true;
        }

        int ex = (int) Math.floor(entity.getX());
        int ez = (int) Math.floor(entity.getZ());
        int lx = this.lastUpdatePos.getX();
        int lz = this.lastUpdatePos.getZ();

        return (ex >> 9) != (lx >> 9) || (ez >> 9) != (lz >> 9) || Math.abs(lx - ex) > 16 || Math.abs(lz - ez) > 16;
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity, MinecraftClient mc)
    {
        this.calculateRegions(entity);
        this.needsUpdate = false;
    }

    private void calculateRegions(Entity entity)
    {
        World world = entity.getEntityWorld();
        int minY = world != null ? world.getBottomY() : -64;
        int maxY = world != null ? world.getTopYInclusive() + 1 : 320;
        int rx = MathHelper.floor(entity.getX()) & ~0x1FF;
        int rz = MathHelper.floor(entity.getZ()) & ~0x1FF;
        BlockPos pos1 = new BlockPos(rx,       minY, rz      );
        BlockPos pos2 = new BlockPos(rx + 511, maxY, rz + 511);
        this.box = Box.enclosing(pos1, pos2);
        this.hasData = true;
    }

    @Override
    public boolean hasData()
    {
        return this.hasData && this.box != null;
    }

    @Override
    public void render(Camera camera, Matrix4f matrix4f, Matrix4f projMatrix, MinecraftClient mc, Profiler profiler)
    {
        if (this.hasData() && this.box != null)
        {
            this.renderQuads(camera, matrix4f, projMatrix, mc, profiler);
            this.renderOutlines(camera, matrix4f, projMatrix, mc, profiler);
        }
    }

    private void renderQuads(Camera camera, Matrix4f matrix4f, Matrix4f projMatrix, MinecraftClient mc, Profiler profiler)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        profiler.push("region_quads");
        Color4f color = Configs.Colors.REGION_OVERLAY_COLOR.getColor();

        Vec3d cameraPos = camera.getPos();

        RenderContext ctx = new RenderContext(() -> "Region Quads", MaLiLibPipelines.POSITION_COLOR_SIMPLE, GlUsage.STATIC_WRITE);
        BufferBuilder builder = ctx.getBuilder();
        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        Vec3d updatePos = this.getUpdatePosition();

        this.preRender();
        matrix4fstack.pushMatrix();
        matrix4fstack.translate((float) (updatePos.x - cameraPos.x), (float) (updatePos.y - cameraPos.y), (float) (updatePos.z - cameraPos.z));

        RenderUtils.renderWallQuads(this.box, cameraPos, color, builder);

        try
        {
            ctx.drawColor(builder.endNullable());
            ctx.close();
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererRegion#renderQuads(): Exception; {}", err.getMessage());
        }

        this.postRender();
        matrix4fstack.popMatrix();
        profiler.pop();
    }

    private void renderOutlines(Camera camera, Matrix4f matrix4f, Matrix4f projMatrix, MinecraftClient mc, Profiler profiler)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        profiler.push("region_outlines");
        Color4f color = Configs.Colors.REGION_OVERLAY_COLOR.getColor();
        Vec3d cameraPos = camera.getPos();

        RenderContext ctx = new RenderContext(() -> "Region Lines", ShaderPipelines.LINES, GlUsage.STATIC_WRITE);
        BufferBuilder builder = ctx.getBuilder();
        MatrixStack matrices = new MatrixStack();
        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        Vec3d updatePos = this.getUpdatePosition();

        this.preRender();
        matrices.push();
        matrix4fstack.pushMatrix();
        matrix4fstack.translate((float) (updatePos.x - cameraPos.x), (float) (updatePos.y - cameraPos.y), (float) (updatePos.z - cameraPos.z));

        MatrixStack.Entry e = matrices.peek();
        RenderUtils.renderWallOutlines(this.box, 16, 16, true, cameraPos, color, builder, e);

        try
        {
            ctx.drawColor(builder.endNullable());
            ctx.close();
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererRegion#renderOutlines(): Exception; {}", err.getMessage());
        }

        this.postRender();
        matrices.pop();
        matrix4fstack.popMatrix();
        profiler.pop();
    }

    @Override
    public void reset()
    {
        super.reset();
        this.box = null;
        this.hasData = false;
    }
}
