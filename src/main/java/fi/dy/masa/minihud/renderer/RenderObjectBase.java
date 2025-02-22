package fi.dy.masa.minihud.renderer;

import org.joml.Matrix4f;

import net.minecraft.client.gl.ShaderPipeline;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;

public abstract class RenderObjectBase
{
    protected final VertexFormat.DrawMode glMode;
    protected final ShaderPipeline shader;

    public RenderObjectBase(VertexFormat.DrawMode glMode, ShaderPipeline shader)
    {
        this.glMode = glMode;
        this.shader = shader;
    }

    public VertexFormat.DrawMode getGlMode()
    {
        return this.glMode;
    }

    public ShaderPipeline getShader()
    {
        return this.shader;
    }

    public abstract void uploadData(BufferBuilder buffer);

    public abstract void draw(Matrix4f matrix4f, Matrix4f projMatrix);

    public abstract void deleteGlResources();
}
