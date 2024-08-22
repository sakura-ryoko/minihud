package fi.dy.masa.minihud.renderer;

import org.joml.Matrix4f;

import net.minecraft.class_10156;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;

public abstract class RenderObjectBase
{
    protected final VertexFormat.DrawMode glMode;
    protected final class_10156 shader;

    public RenderObjectBase(VertexFormat.DrawMode glMode, class_10156 shader)
    {
        this.glMode = glMode;
        this.shader = shader;
    }

    public VertexFormat.DrawMode getGlMode()
    {
        return this.glMode;
    }

    public class_10156 getShader()
    {
        return this.shader;
    }

    public abstract void uploadData(BufferBuilder buffer);

    public abstract void draw(Matrix4f matrix4f, Matrix4f projMatrix);

    public abstract void deleteGlResources();
}
