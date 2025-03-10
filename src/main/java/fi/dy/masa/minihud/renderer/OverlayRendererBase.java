package fi.dy.masa.minihud.renderer;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import com.google.gson.JsonObject;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.ShaderPipeline;
import net.minecraft.client.gl.ShaderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.render.RenderUtils;

public abstract class OverlayRendererBase implements IOverlayRenderer
{
    protected final List<RenderObjectVbo> renderObjects = new ArrayList<>();
    protected boolean renderThrough;
    protected boolean useCulling;
    protected float glLineWidth;
    @Nullable protected BlockPos lastUpdatePos;
    private Vec3d updateCameraPos;

    public OverlayRendererBase()
    {
        this.glLineWidth = 1.0f;
        this.lastUpdatePos = BlockPos.ORIGIN;
        this.updateCameraPos = Vec3d.ZERO;
        this.renderThrough = false;
        this.useCulling = false;
    }

    protected void clearBuffers()
    {
        if (!this.renderObjects.isEmpty())
        {
            this.resetBuffers();
            this.renderObjects.clear();
        }
    }

    protected void allocateBuffers()
    {
        this.clearBuffers();
        this.renderObjects.add(new RenderObjectVbo(() -> this.getName()+" Quads", MaLiLibPipelines.POSITION_COLOR_SIMPLE, GlUsage.STATIC_WRITE));
        this.renderObjects.add(new RenderObjectVbo(() -> this.getName()+" Lines", ShaderPipelines.LINES, GlUsage.STATIC_WRITE));
    }

    protected void resetBuffers()
    {
        this.renderObjects.forEach(RenderObjectVbo::reset);
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

    protected ShaderPipeline getRenderThrough()
    {
        return this.renderThrough ? MaLiLibPipelines.POSITION_COLOR_SIMPLE : MaLiLibPipelines.POSITION_COLOR_LESSER_DEPTH;
    }

    protected void preRender()
    {
        RenderSystem.lineWidth(this.glLineWidth);

        if (this.renderThrough)
        {
            RenderUtils.depthTest(false);
            RenderUtils.depthMask(false);
        }

        if (this.useCulling)
        {
            RenderUtils.culling(true);
        }
    }

    protected void postRender()
    {
        if (this.renderThrough)
        {
            RenderUtils.depthTest(true);
            RenderUtils.depthMask(true);
        }

        if (this.useCulling)
        {
            RenderUtils.culling(false);
        }
    }

    @Override
    public void draw()
    {
        this.preRender();

        for (RenderObjectVbo obj : this.renderObjects)
        {
            obj.drawPost(null, -1, new float[]{0.0F, 0.0F, 0.0F}, false, this.glLineWidth, (obj.getFormat() == VertexFormats.LINES));
        }

        this.postRender();
    }

    @Override
    public void reset()
    {
        this.resetBuffers();
        this.glLineWidth = 1f;
        this.lastUpdatePos = BlockPos.ORIGIN;
        this.updateCameraPos = Vec3d.ZERO;
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
