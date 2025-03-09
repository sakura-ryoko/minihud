package fi.dy.masa.minihud.renderer;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderPass;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.DrawableTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;

import fi.dy.masa.malilib.mixin.render.IMixinBufferBuilder;
import fi.dy.masa.malilib.render.RenderUtils;

/**
 * This was primarily copied from RenderContext
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
        if (RenderSystem.isOnRenderThread() && meshData != null)
        {
            int expectedSize = meshData.getBuffer().remaining();

            if (this.gpuBuffer != null)
            {
                this.gpuBuffer.close();
            }

            this.gpuBuffer = RenderSystem.getDevice().createBuffer(this.name, target, this.usage, expectedSize);
            RenderSystem.getDevice().getResourceManager().copyDataInto(this.gpuBuffer, meshData.getBuffer(), 0);
            this.bufferIndex = meshData.getDrawParameters().indexCount();
        }
    }

    public void draw(BuiltBuffer meshData)
            throws RuntimeException
    {
        this.draw(null, GlBufferTarget.VERTICES, -1, meshData, new float[]{0.0F, 0.0F, 0.0F}, false);
    }

    public void draw(int color, BuiltBuffer meshData)
            throws RuntimeException
    {
        this.draw(null, GlBufferTarget.VERTICES, color, meshData, new float[]{0.0F, 0.0F, 0.0F}, false);
    }

    public void draw(@Nullable Framebuffer otherFb, GlBufferTarget target, int color, BuiltBuffer meshData, float[] offset, boolean useOffset)
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
                this.drawInternal(otherFb, offset, useOffset);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }

            this.started = false;
        }
    }

    private void drawInternal(@Nullable Framebuffer otherFb, float[] offset, boolean useOffset)
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
                texture2 = otherFb.getDepthAttachment();
            }
            else
            {
                texture1 = mainFb.getColorAttachment();
                texture2 = mainFb.getDepthAttachment();
            }

            try (RenderPass pass = RenderSystem.getDevice().getResourceManager().newRenderPass(texture1, OptionalInt.empty(), texture2, OptionalDouble.empty()))
            {
                pass.bindShader(this.shader);
                pass.setIndexBuffer(this.shapeIndex.getIndexBuffer(this.bufferIndex), this.shapeIndex.getIndexType());
                pass.setVertexBuffer(0, this.gpuBuffer);
                pass.drawObjects(0, this.bufferIndex);
            }

            if (useOffset)
            {
                RenderSystem.resetModelOffset();
            }

            this.started = false;
        }
    }

    public AbstractTexture bindTexture(Identifier id)
    {
        TextureManager manager = RenderUtils.tex();
        manager.registerTexture(id);
        return manager.getTexture(id);
    }

    public void unbindTexture(Identifier id)
    {
        RenderUtils.tex().destroyTexture(id);
    }

    public void drawTex(@Nullable Framebuffer otherFb, Identifier texture, GlBufferTarget target, int color, BuiltBuffer meshData, float[] offset, boolean useOffset, ShaderPipeline shader)
            throws RuntimeException
    {
        if (offset.length != 3)
        {
            throw new RuntimeException("Offset needs to be a size of 3.");
        }
        else
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

                if (bufferIndex > 0)
                {
                    float[] rgba = new float[]{ColorHelper.getRedFloat(color), ColorHelper.getGreenFloat(color), ColorHelper.getBlueFloat(color), ColorHelper.getAlphaFloat(color)};
                    float time = (float) (Util.getMeasuringTimeMs() % 3000L) / 3000.0F;
                    RenderSystem.setShaderColor(rgba[0], rgba[1], rgba[2], rgba[3]);
                    this.drawTexInternal(otherFb, texture, time, offset, useOffset);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                }

                this.started = false;
            }
        }
    }

    private void drawTexInternal(@Nullable Framebuffer otherFb, Identifier texture, float time, float[] offset, boolean useOffset)
    {
        this.ensureSafeNoBuffer();

        if (RenderSystem.isOnRenderThread())
        {
            RenderSystem.setTextureMatrix((new Matrix4f()).translation(time, time, 0.0F));

            if (useOffset)
            {
                RenderSystem.setModelOffset(offset[0], offset[1], offset[2]);
            }

            AbstractTexture tex = this.bindTexture(texture);
            tex.setFilter(TriState.FALSE, false);
            Framebuffer mainFb = RenderUtils.fb();
            DrawableTexture texture1;
            DrawableTexture texture2;

            if (otherFb != null)
            {
                texture1 = otherFb.getColorAttachment();
                texture2 = otherFb.getDepthAttachment();
            }
            else
            {
                texture1 = mainFb.getColorAttachment();
                texture2 = mainFb.getDepthAttachment();
            }

            try (RenderPass pass = RenderSystem.getDevice().getResourceManager().newRenderPass(texture1, OptionalInt.empty(), texture2, OptionalDouble.empty()))
            {
                pass.bindShader(this.shader);
                pass.setIndexBuffer(this.shapeIndex.getIndexBuffer(4), this.shapeIndex.getIndexType());
                pass.setSamplerUniform("Sampler0", tex.getGlTexture());
                pass.setVertexBuffer(0, this.gpuBuffer);
                pass.drawObjects(0, this.bufferIndex);
            }

            RenderSystem.resetTextureMatrix();

            if (useOffset)
            {
                RenderSystem.resetModelOffset();
            }
        }
    }

    public void reset()
    {
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
}
