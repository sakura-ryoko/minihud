package fi.dy.masa.minihud.renderer;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.malilib.render.RenderUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class OverlayRendererBase implements IOverlayRenderer
{
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
//    @Override
//    public void deleteGlResources()
//    {
//        for (RenderObjectBase obj : this.renderObjects)
//        {
//            obj.deleteGlResources();
//        }
//
//        this.renderObjects.clear();
//    }
//
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
//    protected RenderObjectBase allocateBuffer(ShaderPipeline shader)
//    {
//        RenderObjectBase obj = new RenderObjectVbo(shader);
//        this.renderObjects.add(obj);
//        return obj;
//    }
//
//    @Override
//    public void allocateGlResources()
//    {
//        //ShaderPipelines.DEBUG_LINE_STRIP
//        this.allocateBuffer(MaLiLibPipelines.POSITION_COLOR_SIMPLE);
//        this.allocateBuffer(MaLiLibPipelines.DEBUG_LINES_SIMPLE);
//    }

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
