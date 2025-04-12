package fi.dy.masa.minihud.renderer;

import java.util.function.Supplier;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
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

    public RenderObjectVbo(VertexFormat.DrawMode glMode, VertexFormat format, Supplier<ShaderProgram> shader)
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
            RenderSystem.setShader(this.getShader());
            this.vertexBuffer.bind();
            this.vertexBuffer.draw(matrix4f, projMatrix, this.getShader().get());
            //this.vertexBuffer.draw(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), this.getShader().get());
            VertexBuffer.unbind();
        }
    }

    @Override
    public void deleteGlResources()
    {
        this.vertexBuffer.close();
    }
}
