package fi.dy.masa.minihud.renderer;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import com.google.gson.JsonObject;

import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.VertexFormats;
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
        this.allocateBuffers(true);
    }

    protected void allocateBuffers(boolean useOutlines)
    {
        this.clearBuffers();
        this.renderObjects.add(new RenderObjectVbo(() -> this.getName()+" Quads", MaLiLibPipelines.POSITION_COLOR_MASA_NO_DEPTH_NO_CULL, BufferUsage.STATIC_WRITE));

        if (useOutlines)
        {
            this.renderObjects.add(new RenderObjectVbo(() -> this.getName() + " Lines", RenderPipelines.LINES, BufferUsage.STATIC_WRITE));
        }
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

    protected RenderPipeline getRenderThrough()
    {
        return this.renderThrough ? MaLiLibPipelines.getPositionSimple() : MaLiLibPipelines.POSITION_COLOR_MASA_LESSER_DEPTH_OFFSET_1;
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
//        this.preRender();

        for (RenderObjectVbo obj : this.renderObjects)
        {
            obj.lineWidth(this.glLineWidth);
            obj.drawPost(null, false, (obj.getVertexFormat() == VertexFormats.POSITION_COLOR_NORMAL));
        }

//        this.postRender();
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
