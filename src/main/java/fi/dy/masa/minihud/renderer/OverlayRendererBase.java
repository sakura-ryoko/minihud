package fi.dy.masa.minihud.renderer;

import javax.annotation.Nullable;
import com.google.gson.JsonObject;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import fi.dy.masa.malilib.render.RenderUtils;

public abstract class OverlayRendererBase implements IOverlayRenderer
{
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

//    @Override
//    public void draw(Matrix4f matrix4f, Matrix4f projMatrix)
//    {
//        this.preRender();
//
//        for (RenderObjectBase obj : this.renderObjects)
//        {
//            obj.draw(matrix4f, projMatrix);
//        }
//
//        this.postRender();
//    }
//

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
