package fi.dy.masa.minihud.renderer;

import java.util.function.Supplier;
import org.joml.Matrix4f;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;

/**
 * Simply not needed
 */
@Deprecated(forRemoval = true)
public abstract class RenderObjectBase
{
    protected final VertexFormat.DrawMode glMode;
    protected final Supplier<ShaderProgram> shader;

    public RenderObjectBase(VertexFormat.DrawMode glMode, Supplier<ShaderProgram> shader)
    {
        this.glMode = glMode;
        this.shader = shader;
    }

    public VertexFormat.DrawMode getGlMode()
    {
        return this.glMode;
    }

    public Supplier<ShaderProgram> getShader()
    {
        return this.shader;
    }

    public abstract void uploadData(BufferBuilder buffer);

    public abstract void draw(Matrix4f matrix4f, Matrix4f projMatrix);

    public abstract void deleteGlResources();
}
