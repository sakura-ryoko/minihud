package fi.dy.masa.minihud.renderer;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import com.google.gson.JsonObject;
import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public abstract class OverlayRendererBase implements IOverlayRenderer
{
    protected static final Tessellator TESSELLATOR_1 = new Tessellator(2097152);
    protected static final Tessellator TESSELLATOR_2 = new Tessellator(2097152);
    protected static BufferBuilder BUFFER_1;
    protected static BufferBuilder BUFFER_2;

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
            RenderSystem.disableDepthTest();
            //RenderSystem.depthMask(false);
        }

        if (this.useCulling)
        {
            RenderSystem.enableCull();
        }
        else
        {
            RenderSystem.disableCull();
        }
    }

    protected void postRender()
    {
        if (this.renderThrough)
        {
            RenderSystem.enableDepthTest();
            //RenderSystem.depthMask(true);
        }

        RenderSystem.enableCull();
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
    protected RenderObjectBase allocateBuffer(VertexFormat.DrawMode glMode)
    {
        // FIXME
        //return this.allocateBuffer(glMode, VertexFormats.POSITION_COLOR, GameRenderer::getPositionColorProgram);
        return this.allocateBuffer(glMode, VertexFormats.POSITION_COLOR, RenderSystem.getShader());
    }

    /**
     * Allocates a new VBO or display list, adds it to the list, and returns it
     * @param glMode
     * @return
     */
    //protected RenderObjectBase allocateBuffer(VertexFormat.DrawMode glMode, VertexFormat format, Supplier<ShaderProgram> shader)
    protected RenderObjectBase allocateBuffer(VertexFormat.DrawMode glMode, VertexFormat format, ShaderProgram shader)
    {
        RenderObjectBase obj = new RenderObjectVbo(glMode, format, shader);
        this.renderObjects.add(obj);
        return obj;
    }

    @Override
    public void allocateGlResources()
    {
        this.allocateBuffer(VertexFormat.DrawMode.QUADS);
        this.allocateBuffer(VertexFormat.DrawMode.DEBUG_LINES);
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
