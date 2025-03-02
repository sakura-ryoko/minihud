package fi.dy.masa.minihud.renderer;

import javax.annotation.Nullable;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.ShaderPipeline;
import net.minecraft.client.gl.ShaderPipelines;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.render.RenderContext;
import fi.dy.masa.malilib.render.RenderUtils;

public abstract class OverlayRendererBase implements IOverlayRenderer
{
    /*
    protected static final Tessellator TESSELLATOR_1 = new Tessellator(2097152);
    protected static final Tessellator TESSELLATOR_2 = new Tessellator(2097152);
    protected static BufferBuilder BUFFER_1;
    protected static BufferBuilder BUFFER_2;
     */
    protected static RenderContext CONTEXT_1 = new RenderContext(MaLiLibPipelines.POSITION_COLOR_SIMPLE, GlUsage.STATIC_WRITE);
    protected static RenderContext CONTEXT_2 = new RenderContext(MaLiLibPipelines.DEBUG_LINES_SIMPLE, GlUsage.STATIC_WRITE);

    protected final List<RenderObjectBase> renderObjects = new ArrayList<>();
    protected boolean renderThrough;
    protected boolean useCulling;
    protected float glLineWidth = 1f;
    @Nullable protected BlockPos lastUpdatePos = BlockPos.ORIGIN;
    private Vec3d updateCameraPos = Vec3d.ZERO;

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

        if (this.useCulling)
        {
            RenderUtils.culling(true);
        }
        else
        {
            RenderUtils.culling(false);
        }
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
    public void draw(Matrix4f matrix4f, Matrix4f projMatrix)
    {
        this.preRender();

        for (RenderObjectBase obj : this.renderObjects)
        {
            obj.draw(matrix4f, projMatrix);
        }

        this.postRender();
    }

    @Override
    public void deleteGlResources()
    {
        for (RenderObjectBase obj : this.renderObjects)
        {
            obj.deleteGlResources();
        }

        this.renderObjects.clear();
    }

    /**
     * Allocates a new VBO or display list, adds it to the list, and returns it
     * @param glMode
     * @return
     */
    /*
    protected RenderObjectBase allocateBuffer(VertexFormat.DrawMode glMode)
    {
        // ShaderPipelines.DEBUG_LINE_STRIP
        return this.allocateBuffer(glMode, VertexFormats.POSITION_COLOR, MaLiLibPipelines.POSITION_COLOR_SIMPLE);
    }
     */

    /**
     * Allocates a new VBO or display list, adds it to the list, and returns it
     * @param shader
     * @return
     */
    protected RenderObjectBase allocateBuffer(ShaderPipeline shader)
    {
        RenderObjectBase obj = new RenderObjectVbo(shader);
        this.renderObjects.add(obj);
        return obj;
    }

    @Override
    public void allocateGlResources()
    {
        //ShaderPipelines.DEBUG_LINE_STRIP
        this.allocateBuffer(MaLiLibPipelines.POSITION_COLOR_SIMPLE);
        this.allocateBuffer(MaLiLibPipelines.DEBUG_LINES_SIMPLE);
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
