package fi.dy.masa.minihud.renderer;

import org.joml.Matrix4f;

import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.ShaderPipeline;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormatElement;

import fi.dy.masa.malilib.render.RenderContext;

public class RenderObjectVbo extends RenderObjectBase
{
    protected RenderContext ctx;
    protected final boolean hasTexture;
    protected boolean hasData;

    public RenderObjectVbo(ShaderPipeline shader)
    {
        super(shader);

//        this.vertexBuffer = new VertexBuffer(GlUsage.STATIC_WRITE);
//        this.format = format;
        this.ctx = new RenderContext(shader, GlUsage.STATIC_WRITE);
        boolean hasTexture = false;

        // This isn't really that nice and clean, but it'll do for now...
        for (VertexFormatElement el : this.ctx.getFormat().getElements())
        {
            if (el.type() == VertexFormatElement.UV.type())
            {
                hasTexture = true;
                break;
            }
        }

        this.hasTexture = hasTexture;
    }

    public RenderContext getContext()
    {
        return this.ctx;
    }

    public BufferBuilder getBuilder()
    {
        return this.ctx.getBuilder();
    }

    @Override
    public void uploadData(BufferBuilder builder)
    {
        BuiltBuffer meshData;
        this.ctx = this.ctx.setBuilder(builder);

        try
        {
            meshData = builder.endNullable();

            if (meshData != null)
            {
                this.hasData = true;
                /*
                this.vertexBuffer.bind();
                this.vertexBuffer.upload(builtBuffer);
                VertexBuffer.unbind();
                builtBuffer.close();
                 */

                this.ctx.upload(meshData);
            }
        }
        catch (Exception ignored) { }
    }

    @Override
    public void draw(Matrix4f matrix4f, Matrix4f projMatrix)
    {
        if (this.hasData)
        {
            //ShaderProgram program = RenderSystem.setShader(this.getShader());
            /*
            this.vertexBuffer.bind();
            this.vertexBuffer.draw(matrix4f, projMatrix, this.getShader().getProgram());
            VertexBuffer.unbind();
             */
            this.ctx.drawColor();
        }
    }

    @Override
    public void deleteGlResources()
    {
        //this.vertexBuffer.close();
        this.ctx.reset();
        this.hasData = false;
    }
}
