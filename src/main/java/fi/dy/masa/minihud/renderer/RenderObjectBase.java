package fi.dy.masa.minihud.renderer;

import org.joml.Matrix4f;

import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;

public abstract class RenderObjectBase
{
    protected final VertexFormat.DrawMode glMode;
    protected final ShaderProgramKey shader;

    public RenderObjectBase(VertexFormat.DrawMode glMode, ShaderProgramKey shader)
    {
        this.glMode = glMode;
        this.shader = shader;
    }

    public VertexFormat.DrawMode getGlMode()
    {
        return this.glMode;
    }

    public ShaderProgramKey getShader()
    {
        return this.shader;
    }

    public abstract void uploadData(BufferBuilder buffer);

    public abstract void draw(Matrix4f matrix4f, Matrix4f projMatrix);

    public abstract void deleteGlResources();
}
