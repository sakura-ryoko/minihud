package fi.dy.masa.minihud.renderer;

import javax.annotation.Nullable;
import com.google.gson.JsonObject;
import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;

import fi.dy.masa.malilib.render.RenderUtils;

public abstract class OverlayRendererBase implements IOverlayRenderer
{
    // TODO -- ADD GPU BUFFERS HERE
    protected boolean renderThrough;
    protected boolean useCulling;
    protected float glLineWidth;
    @Nullable protected BlockPos lastUpdatePos;
    private Vec3d updateCameraPos;

    public OverlayRendererBase()
    {
        this.glLineWidth = 1f;
        this.lastUpdatePos = BlockPos.ORIGIN;
        this.updateCameraPos = Vec3d.ZERO;
    }

    @Override
    public final Vec3d getUpdatePosition()
    {
        return this.updateCameraPos;
    }

    @Override
    public final void setUpdatePosition(Vec3d cameraPosition)
    {
        this.updateCameraPos = cameraPosition;
    }

    protected void preRender()
    {
        RenderSystem.lineWidth(this.glLineWidth);

        if (this.renderThrough)
        {
            RenderUtils.depthTest(false);
            //RenderSystem.depthMask(false);
        }

        RenderUtils.culling(this.useCulling);
    }

    protected void postRender()
    {
        if (this.renderThrough)
        {
            RenderUtils.depthTest(true);
            //RenderSystem.depthMask(true);
        }

        RenderUtils.culling(true);
    }

    @Override
    public void reset()
    {
        this.glLineWidth = 1f;
        this.lastUpdatePos = BlockPos.ORIGIN;
        this.updateCameraPos = Vec3d.ZERO;
    }

    @Override
    public void draw(Camera camera, Matrix4f posMatrix, Matrix4f projMatrix, MinecraftClient mc, Profiler profiler)
    {
    }

    public void setRenderThrough(boolean renderThrough)
    {
        this.renderThrough = renderThrough;
    }

    public String getSaveId()
    {
        return "";
    }

    @Nullable
    public JsonObject toJson()
    {
        return null;
    }

    public void fromJson(JsonObject obj)
    {
    }
}
