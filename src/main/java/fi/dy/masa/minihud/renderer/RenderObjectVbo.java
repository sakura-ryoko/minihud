package fi.dy.masa.minihud.renderer;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;

public class RenderObjectVbo extends RenderObjectBase
{
    protected final VertexBuffer vertexBuffer;
    protected final VertexFormat format;
    protected final boolean hasTexture;
    protected boolean hasData;

    //public RenderObjectVbo(VertexFormat.DrawMode glMode, VertexFormat format, Supplier<ShaderProgram> shader)
    public RenderObjectVbo(VertexFormat.DrawMode glMode, VertexFormat format, ShaderProgram shader)
    {
        super(glMode, shader);

        this.vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        this.format = format;

        boolean hasTexture = false;

        // This isn't really that nice and clean, but it'll do for now...
        for (VertexFormatElement el : this.format.getElements())
        {
            if (el.type() == VertexFormatElement.UV.type())
            {
                hasTexture = true;
                break;
            }
        }

        this.hasTexture = hasTexture;
    }

    @Override
    public void uploadData(BufferBuilder buffer)
    {
        BuiltBuffer builtBuffer;

        try
        {
            builtBuffer = buffer.endNullable();

            if (builtBuffer != null)
            {
                this.hasData = true;
                this.vertexBuffer.bind();
                this.vertexBuffer.upload(builtBuffer);
                VertexBuffer.unbind();
                builtBuffer.close();
            }
        }
        catch (Exception ignored) { }
    }

    @Override
    public void draw(Matrix4f matrix4f, Matrix4f projMatrix)
    {
        if (this.hasData)
        {
            // FIXME
            RenderSystem.setShader(this.getShader());
            this.vertexBuffer.bind();
            this.vertexBuffer.draw(matrix4f, projMatrix, this.getShader());
            VertexBuffer.unbind();
        }
    }

    @Override
    public void deleteGlResources()
    {
        this.vertexBuffer.close();
    }
}
