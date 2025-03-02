package fi.dy.masa.minihud.renderer;

import org.joml.Matrix4f;

import net.minecraft.client.gl.ShaderPipeline;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;

public abstract class RenderObjectBase
{
    protected final ShaderPipeline shader;

    public RenderObjectBase(ShaderPipeline shader)
    {
        this.shader = shader;
    }

    public VertexFormat.DrawMode getGlMode()
    {
        return this.shader.getDrawMode();
    }

    public VertexFormat getFormat()
    {
        return this.shader.getFormat();
    }

    public ShaderPipeline getShader()
    {
        return this.shader;
    }

    public abstract void uploadData(BufferBuilder buffer);

    public abstract void draw(Matrix4f matrix4f, Matrix4f projMatrix);

    public abstract void deleteGlResources();
}
