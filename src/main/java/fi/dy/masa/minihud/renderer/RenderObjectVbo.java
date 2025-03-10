package fi.dy.masa.minihud.renderer;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;
import javax.annotation.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.util.math.ColorHelper;

import fi.dy.masa.malilib.mixin.render.IMixinBufferBuilder;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.minihud.MiniHUD;

/**
 * This was primarily copied from RenderContext, but this way
 * the RenderContainer has full control from within MiniHUD.
 */
public class RenderObjectVbo
{
    private Supplier<String> name;
    private ShaderPipeline shader;
    private GlUsage usage;
    private GpuBuffer gpuBuffer;
    private RenderSystem.ShapeIndexBuffer shapeIndex;
    private BufferAllocator alloc;
    private BufferBuilder builder;
    private VertexFormat format;
    private VertexFormat.DrawMode drawMode;
    private ResourceTexture texture;
    private boolean started;
    private int bufferIndex;

    public RenderObjectVbo(Supplier<String> name, ShaderPipeline shader, GlUsage usage)
    {
        this.name = name;
        this.alloc = new BufferAllocator(shader.getFormat().getVertexSizeByte() * 4);
        this.builder = new BufferBuilder(this.alloc, shader.getDrawMode(), shader.getFormat());
        this.shapeIndex = RenderSystem.getSequentialBuffer(shader.getDrawMode());
        this.format = shader.getFormat();
        this.drawMode = shader.getDrawMode();
        this.shader = shader;
        this.usage = usage;
        this.gpuBuffer = null;
        this.bufferIndex = -1;
        this.texture = null;
        this.started = true;
    }

    public BufferBuilder start(Supplier<String> name, ShaderPipeline shader, GlUsage usage)
    {
        this.reset();
        this.name = name;
        this.alloc = new BufferAllocator(shader.getFormat().getVertexSizeByte() * 4);
        this.builder = new BufferBuilder(this.alloc, shader.getDrawMode(), shader.getFormat());
        this.shapeIndex = RenderSystem.getSequentialBuffer(shader.getDrawMode());
        this.format = shader.getFormat();
        this.drawMode = shader.getDrawMode();
        this.shader = shader;
        this.usage = usage;
        this.gpuBuffer = null;
        this.bufferIndex = -1;
        this.texture = null;
        this.started = true;
        return this.builder;
    }

    public String getName()
    {
        return this.name.get();
    }

    public BufferBuilder getBuilder()
    {
        return this.builder;
    }

    public GlUsage getUsage()
    {
        return this.usage;
    }

    public VertexFormat getFormat()
    {
        return this.format;
    }

    public VertexFormat.DrawMode getDrawMode()
    {
        return this.drawMode;
    }

    public void upload(BuiltBuffer meshData, GlBufferTarget target)
    {
        this.ensureSafeNoBuffer();

        if (RenderSystem.isOnRenderThread() && meshData != null)
        {
            int expectedSize = meshData.getBuffer().remaining();

            if (this.gpuBuffer != null)
            {
                this.gpuBuffer.close();
            }

            this.gpuBuffer = RenderSystem.getDevice().createBuffer(this.name, target, this.usage, expectedSize);

            RenderSystem.getDevice()
                        .getResourceManager()
                        .copyDataInto(this.gpuBuffer, meshData.getBuffer(), 0);

            this.bufferIndex = meshData.getDrawParameters().indexCount();
        }
    }

    public void bindTexture(Identifier id) throws RuntimeException
    {
        this.ensureSafeNoBuffer();
        this.texture = new ResourceTexture(id);
        RenderUtils.tex().registerTexture(id, this.texture);

        try (TextureContents contents = this.texture.loadContents(RenderUtils.mc().getResourceManager()))
        {
            NativeImage image = contents.image();
            MiniHUD.LOGGER.warn("NativeImage Id [{}] //  Width [{}], Height [{}] // Format: [{}]", image.imageId(), image.getWidth(), image.getHeight(), image.getFormat().name());
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("bindTexture exception; {}", err.getMessage());
            throw new RuntimeException(err);
        }

        this.texture.setFilter(TriState.FALSE, false);
        RenderSystem.setShaderTexture(0, this.texture.getGlTexture());
    }

    public void unbindTexture(@Nullable Identifier id)
    {
        if (id != null)
        {
            RenderUtils.tex().destroyTexture(id);
        }

        RenderSystem.setShaderTexture(0, null);
    }

    public void draw(BuiltBuffer meshData)
            throws RuntimeException
    {
        this.draw(null, GlBufferTarget.VERTICES, -1, meshData, new float[]{0.0F, 0.0F, 0.0F}, false, 0.0f, false);
    }

    public void draw(int color, BuiltBuffer meshData)
            throws RuntimeException
    {
        this.draw(null, GlBufferTarget.VERTICES, color, meshData, new float[]{0.0F, 0.0F, 0.0F}, false, 0.0f, false);
    }

    public void draw(@Nullable Framebuffer otherFb, GlBufferTarget target, int color, BuiltBuffer meshData, float[] offset, boolean useOffset)
            throws RuntimeException
    {
        this.draw(otherFb, target, color, meshData, new float[]{0.0F, 0.0F, 0.0F}, false, 0.0f, false);
    }

    public void draw(@Nullable Framebuffer otherFb, GlBufferTarget target,
                     int color, BuiltBuffer meshData,
                     float[] offset, boolean useOffset,
                     float lineWidth, boolean setLineWidth)
            throws RuntimeException
    {
        this.ensureSafeNoBuffer();

        if (RenderSystem.isOnRenderThread())
        {
            if (meshData == null)
            {
                this.bufferIndex = 0;
            }
            else
            {
                if (this.bufferIndex < 1)
                {
                    this.upload(meshData, target);
                }
            }

            if (this.bufferIndex > 0)
            {
                float[] rgba = new float[]{ColorHelper.getRedFloat(color), ColorHelper.getGreenFloat(color), ColorHelper.getBlueFloat(color), ColorHelper.getAlphaFloat(color)};

                RenderSystem.setShaderColor(rgba[0], rgba[1], rgba[2], rgba[3]);
                this.drawInternal(otherFb, offset, useOffset, lineWidth, setLineWidth);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }

    public void drawPost(@Nullable Framebuffer otherFb, int color, float[] offset, boolean useOffset, float lineWidth, boolean setLineWidth)
            throws RuntimeException
    {
        if (this.bufferIndex > 0)
        {
            float[] rgba = new float[]{ColorHelper.getRedFloat(color), ColorHelper.getGreenFloat(color), ColorHelper.getBlueFloat(color), ColorHelper.getAlphaFloat(color)};
            RenderSystem.setShaderColor(rgba[0], rgba[1], rgba[2], rgba[3]);
            this.drawInternal(otherFb, offset, useOffset, lineWidth, setLineWidth);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private void drawInternal(@Nullable Framebuffer otherFb, float[] offset, boolean useOffset, float lineWidth, boolean setLineWidth)
            throws RuntimeException
    {
        this.ensureSafeNoBuffer();

        if (RenderSystem.isOnRenderThread())
        {
            if (useOffset)
            {
                RenderSystem.setModelOffset(-offset[0], offset[1], -offset[2]);
            }

            Framebuffer mainFb = RenderUtils.fb();
            DrawableTexture texture1;
            DrawableTexture texture2;

            if (otherFb != null)
            {
                texture1 = otherFb.getColorAttachment();
                texture2 = otherFb.useDepthAttachment ? otherFb.getDepthAttachment() : null;
            }
            else
            {
                texture1 = mainFb.getColorAttachment();
                texture2 = mainFb.useDepthAttachment ? mainFb.getDepthAttachment() : null;
            }

            try (RenderPass pass = RenderSystem.getDevice()
                                               .getResourceManager()
                                               .createRenderPass(texture1, OptionalInt.empty(),
                                                                 texture2, OptionalDouble.empty()))
            {
                pass.bindShader(this.shader);

                for (int i = 0; i < 12; i++)
                {
                    DrawableTexture drawableTexture = RenderSystem.getShaderTexture(i);

                    if (drawableTexture != null)
                    {
                        pass.setUniform("Sampler"+i, drawableTexture);
                    }
                }

                if (setLineWidth)
                {
                    float width = lineWidth > 0.0f ? lineWidth : RenderSystem.getShaderLineWidth();
                    pass.setUniform("LineWidth", width);
                }

                pass.setIndexBuffer(this.shapeIndex.getIndexBuffer(this.bufferIndex), this.shapeIndex.getIndexType());
                pass.setVertexBuffer(0, this.gpuBuffer);
                pass.drawObjects(0, this.bufferIndex);
            }

            if (useOffset)
            {
                RenderSystem.resetModelOffset();
            }
        }
    }

    public void reset()
    {
        if (this.texture != null)
        {
            this.unbindTexture(null);
            this.texture.close();
            this.texture = null;
        }

        if (this.gpuBuffer != null)
        {
            this.gpuBuffer.close();
            this.gpuBuffer = null;
        }

        if (this.builder != null)
        {
            if (((IMixinBufferBuilder) this.builder).malilib_isBuilding() && ((IMixinBufferBuilder) this.builder).malilib_getVertexCount() != 0)
            {
                try
                {
                    BuiltBuffer meshData = this.builder.endNullable();

                    if (meshData != null)
                    {
                        meshData.close();
                    }
                }
                catch (Exception ignored)
                {
                }
            }

            this.builder = null;
        }

        if (this.alloc != null)
        {
            this.alloc.close();
            this.alloc = null;
        }

        this.bufferIndex = -1;
        this.started = false;
    }

    private void ensureBuilding(BufferBuilder builder) throws RuntimeException
    {
        if (!((IMixinBufferBuilder) builder).malilib_isBuilding())
        {
            throw new RuntimeException("Buffer Builder is not building!");
        }
        else if (((IMixinBufferBuilder) builder).malilib_getVertexCount() == 0)
        {
            throw new RuntimeException("Buffer Builder vertices are zero!");
        }
        else if (((IMixinBufferBuilder) builder).malilib_getVertexPointer() == -1L)
        {
            throw new RuntimeException("Buffer Builder has no vertices!");
        }
    }

    private void ensureSafeNoShader() throws RuntimeException
    {
        if (!this.started)
        {
            throw new RuntimeException("Context not started!");
        }
        else if (this.alloc == null)
        {
            throw new RuntimeException("Allocator not valid!");
        }
        else if (this.builder == null)
        {
            throw new RuntimeException("Buffer Builder not valid!");
        }
    }

    private void ensureSafeNoBuffer() throws RuntimeException
    {
        this.ensureSafeNoShader();

        if (this.shader == null)
        {
            throw new RuntimeException("Shader Pipeline not valid!");
        }
    }

    private void ensureSafeTexture()
    {
        this.ensureSafeNoBuffer();

        if (this.texture == null)
        {
            throw new RuntimeException("A Texture Object is expected to be bound");
        }
    }
}
