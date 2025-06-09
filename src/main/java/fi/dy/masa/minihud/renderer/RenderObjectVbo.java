package fi.dy.masa.minihud.renderer;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.TextureContents;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;

import fi.dy.masa.malilib.mixin.render.IMixinAbstractTexture;
import fi.dy.masa.malilib.mixin.render.IMixinBufferBuilder;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.minihud.MiniHUD;

/**
 * This was primarily copied from RenderContext, but this way
 * the RenderContainer has full control from within MiniHUD; and
 * it is not AutoClosable so that we can dynamically re-allocate
 * it using the RenderContainer system.
 * For other non-RenderContainer purposes; use RenderContext.
 */
public class RenderObjectVbo
{
    private Supplier<String> name;
    private RenderPipeline shader;
    private BufferUsage usage;
    private GpuBuffer vertexBuffer;
    @Nullable private GpuBuffer indexBuffer;
    private RenderSystem.ShapeIndexBuffer shapeIndex;
    private VertexFormat.IndexType indexType;
    private BufferAllocator alloc;
    private BufferBuilder builder;
    private VertexFormat format;
    private VertexFormat.DrawMode drawMode;
    private ResourceTexture texture;
    @Nullable private BuiltBuffer.SortState sortState;
    private int textureId;
    private float[] offset;
    private float lineWidth;
    private int color;
    private boolean started;
    private int indexCount;

    protected RenderObjectVbo(Supplier<String> name, RenderPipeline shader, BufferUsage usage)
    {
        this.name = name;
        this.alloc = new BufferAllocator(shader.getVertexFormat().getVertexSize() * 4);
        this.builder = new BufferBuilder(this.alloc, shader.getVertexFormatMode(), shader.getVertexFormat());
        this.shapeIndex = RenderSystem.getSequentialBuffer(shader.getVertexFormatMode());
        this.indexType = this.shapeIndex.getIndexType();
        this.format = shader.getVertexFormat();
        this.drawMode = shader.getVertexFormatMode();
        this.shader = shader;
        this.usage = usage;
        this.vertexBuffer = null;
        this.indexBuffer = null;
        this.sortState = null;
        this.indexCount = -1;
        // We don't need to reset this, in case we need to re-use the texture
//        this.texture = null;
        this.textureId = -1;
        this.offset = new float[]{0f, 0f, 0f};
        this.color = -1;
        this.lineWidth = 1.0f;
        this.started = true;
    }

    public BufferBuilder start(Supplier<String> name, RenderPipeline shader, BufferUsage usage)
    {
        this.reset();
        this.name = name;
        this.alloc = new BufferAllocator(shader.getVertexFormat().getVertexSize() * 4);
        this.builder = new BufferBuilder(this.alloc, shader.getVertexFormatMode(), shader.getVertexFormat());
        this.shapeIndex = RenderSystem.getSequentialBuffer(shader.getVertexFormatMode());
        this.indexType = this.shapeIndex.getIndexType();
        this.format = shader.getVertexFormat();
        this.drawMode = shader.getVertexFormatMode();
        this.shader = shader;
        this.usage = usage;
        this.vertexBuffer = null;
        this.indexBuffer = null;
        this.sortState = null;
        this.indexCount = -1;
        // We don't need to reset this, in case we need to re-use the texture
//        this.texture = null;
        this.textureId = -1;
        this.offset = new float[]{0f, 0f, 0f};
        this.color = -1;
        this.lineWidth = 1.0f;
        this.started = true;
        return this.builder;
    }

    protected boolean isStarted() { return this.started; }

    public String getName()
    {
        return this.name.get();
    }

    protected BufferBuilder getBuilder()
    {
        return this.builder;
    }

    public BufferUsage getUsage()
    {
        return this.usage;
    }

    public VertexFormat getVertexFormat()
    {
        return this.format;
    }

    public VertexFormat.DrawMode getDrawMode()
    {
        return this.drawMode;
    }

    public VertexFormat getShaderFormat()
    {
        if (this.shader != null)
        {
            return this.shader.getVertexFormat();
        }

        return this.format;
    }

    public VertexFormat.DrawMode getShaderDrawMode()
    {
        if (this.shader != null)
        {
            return this.shader.getVertexFormatMode();
        }

        return this.drawMode;
    }

    /**
     * BUILDER PHASE --
     * -
     * This is to simply ensure that the builder is stored again
     * @param builder ()
     * @return ()
     */
    protected RenderObjectVbo setBuilder(BufferBuilder builder) throws RuntimeException
    {
        this.ensureBuilding(builder);
        this.builder = builder;
        return this;
    }

    protected RenderObjectVbo lineWidth(float width)
    {
        this.lineWidth = Math.clamp(width, 0.0f, 25.0f);
        return this;
    }

    protected RenderObjectVbo offset(float[] value)
    {
        if (value.length != 3)
        {
            value = new float[]{0f, 0f, 0f};
        }

        this.offset[0] = value[0];
        this.offset[1] = value[1];
        this.offset[2] = value[2];

        return this;
    }

    protected RenderObjectVbo color(int color)
    {
        this.color = color;
        return this;
    }

    /**
     * UPLOAD PHASE --
     * -
     * This uploads the BufferBuilder to the GpuBuffer for Drawing
     */
    protected void upload() throws RuntimeException
    {
        this.upload(false);
    }

    protected void upload(boolean shouldResort) throws RuntimeException
    {
        this.ensureSafeNoShader();
        this.ensureBuilding(this.builder);

        try (BuiltBuffer meshData = this.builder.endNullable())
        {
            if (meshData != null)
            {
                this.upload(meshData, shouldResort);
            }
            else
            {
                throw new RuntimeException("Empty Mesh Data!");
            }
        }
    }

    protected void upload(BufferBuilder builder) throws RuntimeException
    {
        this.upload(builder, false);
    }

    protected void upload(BufferBuilder builder, boolean shouldResort) throws RuntimeException
    {
        this.ensureSafeNoShader();
        this.ensureBuilding(builder);
        this.builder = builder;

        try (BuiltBuffer meshData = this.builder.endNullable())
        {
            if (meshData != null)
            {
                this.upload(meshData, shouldResort);
            }
            else
            {
                throw new RuntimeException("Empty Mesh Data!");
            }
        }
    }

    public void upload(BuiltBuffer meshData, boolean shouldResort) throws RuntimeException
    {
        this.ensureSafeNoBuffer();

        if (RenderSystem.isOnRenderThread() && meshData != null)
        {
            int expectedSize = meshData.getBuffer().remaining();

            if (this.vertexBuffer != null)
            {
                this.vertexBuffer.close();
            }

//            if (this.indexBuffer != null)
//            {
//                this.indexBuffer.close();
//                this.indexBuffer = null;
//            }

            if (this.vertexBuffer == null)
            {
                this.vertexBuffer = RenderSystem.getDevice().createBuffer(() -> this.name.get()+" VertexBuffer", BufferType.VERTICES, this.usage, expectedSize);
            }
            else if (this.vertexBuffer.size() < expectedSize)
            {
                this.vertexBuffer.close();
                this.vertexBuffer = RenderSystem.getDevice().createBuffer(() -> this.name.get()+" VertexBuffer", BufferType.VERTICES, this.usage, expectedSize);
            }

            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

            if (!this.vertexBuffer.isClosed())
            {
                encoder.writeToBuffer(this.vertexBuffer, meshData.getBuffer(), 0);
            }
            else
            {
                throw new RuntimeException("Vertex Buffer is closed!");
            }

            // Resorting
            if (shouldResort && meshData.getSortedBuffer() != null)
            {
                if (this.indexBuffer != null && this.indexBuffer.size() >= meshData.getSortedBuffer().remaining())
                {
                    if (!this.indexBuffer.isClosed())
                    {
                        encoder.writeToBuffer(this.indexBuffer, meshData.getSortedBuffer(), 0);
                    }
                }
                else
                {
                    if (this.indexBuffer != null)
                    {
                        this.indexBuffer.close();
                    }

                    this.indexBuffer = RenderSystem.getDevice().createBuffer(() -> this.name.get()+" IndexBuffer", BufferType.INDICES, this.usage, meshData.getSortedBuffer());
                }
            }
            else if (this.indexBuffer != null)
            {
                this.indexBuffer.close();
                this.indexBuffer = null;
            }

            this.indexCount = meshData.getDrawParameters().indexCount();
            this.indexType = meshData.getDrawParameters().indexType();
//            meshData.close();
        }
    }

    /**
     * UPLOAD/RESORTING PHASE --
     * -
     * This uploads the IndexBuffer for resorting
     */
    protected VertexSorter createVertexSorter(float x, float y, float z)
    {
        return VertexSorter.byDistance(x, y, z);
    }

    public VertexSorter createVertexSorter(Vec3d pos)
    {
        return this.createVertexSorter(pos, BlockPos.ORIGIN);
    }

    protected VertexSorter createVertexSorter(Camera camera)
    {
        return this.createVertexSorter(camera.getPos(), BlockPos.ORIGIN);
    }

    protected VertexSorter createVertexSorter(Camera camera, BlockPos origin)
    {
        return this.createVertexSorter(camera.getPos(), origin);
    }

    protected VertexSorter createVertexSorter(Vec3d pos, BlockPos origin)
    {
        return VertexSorter.byDistance((float)(pos.x - (double)origin.getX()), (float)(pos.y - (double) origin.getY()), (float)(pos.z - (double) origin.getZ()));
    }

    public void startResorting(@Nonnull BuiltBuffer meshData, @Nonnull VertexSorter sorter) throws RuntimeException
    {
        this.ensureSafeNoBuffer();

        if (RenderSystem.isOnRenderThread())
        {
            this.sortState = meshData.sortQuads(this.alloc, sorter);
            this.resortTranslucent(sorter);
        }
    }

    protected boolean shouldResort()
    {
        return this.sortState != null;
    }

    protected void resortTranslucent(@Nonnull VertexSorter sorter) throws RuntimeException
    {
        this.ensureSafeNoBuffer();

        if (RenderSystem.isOnRenderThread())
        {
            if (this.sortState == null)
            {
                throw new RuntimeException("Sort State is empty!");
            }

            BufferAllocator.CloseableBuffer result = this.sortState.sortAndStore(this.alloc, sorter);

            if (result != null)
            {
                this.uploadIndex(result);
                result.close();
            }
            else
            {
                throw new RuntimeException("Unable to Store Sorting Data in Result Buffer!");
            }
        }
    }

    protected void uploadIndex(@Nonnull BufferAllocator.CloseableBuffer buffer) throws RuntimeException
    {
        this.ensureSafeNoBuffer();

        if (RenderSystem.isOnRenderThread())
        {
            if (this.indexBuffer == null)
            {
                this.indexBuffer = RenderSystem.getDevice().createBuffer(() -> this.name.get()+" IndexBuffer", BufferType.INDICES, BufferUsage.STATIC_WRITE, buffer.getBuffer());
            }
            else
            {
                if (!this.indexBuffer.isClosed())
                {
                    RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.indexBuffer, buffer.getBuffer(), 0);
                }
            }
        }
    }

    /**
     * BIND TEXTURE PHASE --
     * -
     * Performs the Texture Binding/Unbind for the "Shader Texture" layer
     */
    protected void bindTexture(Identifier id, int textureId, int width, int height) throws Exception
    {
        this.ensureSafeNoBuffer();

        if (textureId < 0 || textureId > 12)
        {
            throw new RuntimeException("Invalid textureId of: " + textureId + " for texture: " + id.toString());
        }

        try
        {
            // Verify that we potentially have the correct texture by checking various values
            while (!this.isTextureValid(width, height))
            {
                this.texture = (ResourceTexture) RenderUtils.tex().getTexture(id);

                if (this.isTextureValid(width, height))
                {
                    if (this.texture != null)
                    {
                        this.texture.setFilter(TriState.DEFAULT, false);
                        RenderSystem.setShaderTexture(textureId, this.texture.getGlTexture());
                    }

                    break;
                }
            }
        }
        catch (Exception err)
        {
            throw new RuntimeException("Exception reading Texture ["+id.toString()+"]: "+err.getMessage());
        }

        if (this.texture != null)
        {
            // Simple texture rebind since we already have a valid texture
            this.textureId = textureId;
            RenderSystem.setShaderTexture(this.textureId, this.texture.getGlTexture());
            return;
        }

        // General failure & cleanup
        MiniHUD.LOGGER.error("bindTexture: Error uploading texture [{}]", id.toString());

        if (this.texture != null)
        {
            this.texture.close();
        }

        this.texture = null;
        this.textureId = -1;
    }

    private boolean isTextureValid(int width, int height)
    {
        if (this.texture == null)
        {
            return false;
        }

        try (TextureContents content = this.texture.loadContents(RenderUtils.mc().getResourceManager()))
        {
            NativeImage image = content.image();

            if (image == null || image.getWidth() != width || image.getHeight() != height)
            {
                this.texture.close();
                this.texture = null;
                return false;
            }
        }
        catch (Exception e)
        {
            this.texture.close();
            this.texture = null;
            return false;
        }

        if (((IMixinAbstractTexture) this.texture).malilib_getGlTexture() == null ||
            this.texture.getGlTexture().isClosed())
        {
            this.texture.close();
            this.texture = null;
            return false;
        }

        return true;
    }

//    private @Nullable NativeImageBackedTexture loadFile(Identifier texture)
//    {
//        try
//        {
//            InputStream inputStream = RenderUtils.mc().getResourceManager().open(texture);
//
//            try (NativeImage image = NativeImage.read(inputStream))
//            {
//                return new NativeImageBackedTexture(texture::toString, image.getWidth(), image.getHeight(), false);
//            }
//            catch (Exception err)
//            {
//                MiniHUD.LOGGER.error("Failed to read texture: '{}'; Exception: {}", texture.toString(), err.getMessage());
//            }
//        }
//        catch (Exception err)
//        {
//            MiniHUD.LOGGER.error("Error opening input stream for texture: '{}'; Exception: {}", texture.toString(), err.getMessage());
//        }
//
//        return null;
//    }

//    private void dumpTexture(Identifier id)
//    {
//        Path dir = FileUtils.getConfigDirectoryAsPath().resolve(Reference.MOD_ID).resolve("textures");
//
//        try (TextureContents content = this.texture.loadContents(RenderUtils.mc().getResourceManager()))
//        {
//            if (!Files.isDirectory(dir))
//            {
//                Files.createDirectory(dir);
//            }
//
//            content.image().writeTo(dir.resolve(FileNameUtils.generateSimpleSafeFileName(id.toString())));
//        }
//        catch (Exception err)
//        {
//            MiniHUD.LOGGER.error("bindTexture: Error saving debug texture for [{}]", id.toString());
//        }
//    }

//    private void dumpTextureManager()
//    {
//        Path dir = FileUtils.getConfigDirectoryAsPath().resolve(Reference.MOD_ID).resolve("textures/manager_dump");
//
//        try
//        {
//            if (!Files.isDirectory(dir))
//            {
//                Files.createDirectory(dir);
//            }
//
//            RenderUtils.tex().dumpDynamicTextures(dir);
//        }
//        catch (Exception ignored) {}
//    }

    protected void unbindTexture(@Nullable Identifier id)
    {
        if (id != null)
        {
            RenderUtils.tex().destroyTexture(id);
        }

        if (this.texture != null)
        {
            RenderUtils.tex().destroyTexture(this.texture.getId());
        }

        RenderSystem.setShaderTexture(0, null);
    }

    /**
     * DRAW PHASE --
     * -
     * Performs the Renderer draw to the specified Frame Buffer
     */
    protected void draw() throws RuntimeException
    {
        this.draw(false);
    }

    protected void draw(boolean shouldResort) throws RuntimeException
    {
        this.ensureSafeNoBuffer();
        this.ensureBuilding(this.builder);
        BuiltBuffer meshData = this.builder.endNullable();

        if (meshData != null)
        {
            this.draw(meshData, shouldResort);
            meshData.close();
        }
    }

    protected void draw(BuiltBuffer meshData) throws RuntimeException
    {
        this.ensureSafeNoBuffer();
        this.draw(null, meshData, false, false, false);
    }

    protected void draw(BuiltBuffer meshData, boolean shouldResort) throws RuntimeException
    {
        this.ensureSafeNoBuffer();
        this.draw(null, meshData, shouldResort, false, false);
    }

    protected void draw(BuiltBuffer meshData, boolean shouldResort, boolean setLineWidth) throws RuntimeException
    {
        this.ensureSafeNoBuffer();
        this.draw(null, meshData, shouldResort,false, setLineWidth);
    }

    protected void draw(@Nullable Framebuffer otherFb, BuiltBuffer meshData, boolean shouldResort) throws RuntimeException
    {
        this.ensureSafeNoBuffer();
        this.draw(otherFb, meshData, shouldResort, false, false);
    }

    protected void draw(@Nullable Framebuffer otherFb, BuiltBuffer meshData, boolean shouldResort, boolean setLineWidth) throws RuntimeException
    {
        this.ensureSafeNoBuffer();
        this.draw(otherFb, meshData, shouldResort, false, setLineWidth);
    }

    protected void draw(@Nullable Framebuffer otherFb, BuiltBuffer meshData, boolean shouldResort,
                        boolean useOffset, boolean setLineWidth) throws RuntimeException
    {
        this.ensureSafeNoBuffer();

        if (RenderSystem.isOnRenderThread())
        {
            if (meshData == null)
            {
                this.indexCount = 0;
            }
            else
            {
                if (this.indexCount < 1)
                {
                    //MiniHUD.LOGGER.warn("RenderContext#draw() [{}] --> upload()", this.name.get());
                    this.upload(meshData, shouldResort);
                }
            }

            if (this.indexCount > 0)
            {
                float[] rgba = new float[]{ColorHelper.getRedFloat(this.color), ColorHelper.getGreenFloat(this.color), ColorHelper.getBlueFloat(this.color), ColorHelper.getAlphaFloat(this.color)};

                RenderSystem.setShaderColor(rgba[0], rgba[1], rgba[2], rgba[3]);
                this.drawInternal(otherFb, useOffset, setLineWidth);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }

    protected void drawPost(@Nullable Framebuffer otherFb, boolean useOffset, boolean setLineWidth)
            throws RuntimeException
    {
        if (this.indexCount > 0)
        {
            float[] rgba = new float[]{ColorHelper.getRedFloat(this.color), ColorHelper.getGreenFloat(this.color), ColorHelper.getBlueFloat(this.color), ColorHelper.getAlphaFloat(this.color)};

            //MiniHUD.LOGGER.warn("RenderContext#drawPost() [{}] --> drawInternal()", this.name.get());
            RenderSystem.setShaderColor(rgba[0], rgba[1], rgba[2], rgba[3]);
            this.drawInternal(otherFb, useOffset, setLineWidth);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private void drawInternal(@Nullable Framebuffer otherFb, boolean useOffset, boolean setLineWidth)
            throws RuntimeException
    {
        this.ensureSafeNoBuffer();

        if (RenderSystem.isOnRenderThread())
        {
            if (useOffset)
            {
                RenderSystem.setModelOffset(this.offset[0], this.offset[1], this.offset[2]);
            }

            Framebuffer mainFb = RenderUtils.fb();
            GpuTexture texture1;
            GpuTexture texture2;

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

            //MiniHUD.LOGGER.warn("RenderContext#drawInternal() [{}] --> new renderPass", this.name.get());
            GpuBuffer indexBuffer = this.shapeIndex.getIndexBuffer(this.indexCount);

            try (RenderPass pass = RenderSystem.getDevice()
                                               .createCommandEncoder()
                                               .createRenderPass(texture1, OptionalInt.empty(),
                                                                 texture2, OptionalDouble.empty()))
            {
                //MiniHUD.LOGGER.warn("RenderContext#drawInternal() [{}] renderPass --> setPipeline() [{}]", this.name.get(), this.shader.getLocation().toString());
                pass.setPipeline(this.shader);

                if (this.textureId > -1 && this.textureId < 12 && this.texture != null)
                {
//                    MiniHUD.LOGGER.warn("RenderContext#drawInternal() [{}] renderPass --> bindSampler({}) [{}]", this.name.get(), this.textureId, this.texture.getGlTexture().getLabel());
                    pass.bindSampler("Sampler"+this.textureId, this.texture.getGlTexture());
                }

//                for (int i = 0; i < 12; i++)
//                {
//                    GpuTexture drawableTexture = RenderSystem.getShaderTexture(i);
//
//                    if (drawableTexture != null)
//                    {
////                        MiniHUD.LOGGER.warn("RenderContext#drawInternal() [{}] renderPass --> bindSampler() [{}]", this.name.get(), i);
//                        pass.bindSampler("Sampler"+i, drawableTexture);
//                    }
//                }

                if (setLineWidth)
                {
                    float width = this.lineWidth > 0.0f ? this.lineWidth : RenderSystem.getShaderLineWidth();
                    //MiniHUD.LOGGER.warn("RenderContext#drawInternal() [{}] renderPass --> setUniform() // lineWidth [{}]", this.name.get(), width);
                    pass.setUniform("LineWidth", width);
                }

                //MiniHUD.LOGGER.warn("RenderContext#drawInternal() [{}] renderPass --> setVertexBuffer() [0]", this.name.get());
                pass.setVertexBuffer(0, this.vertexBuffer);

                //MiniHUD.LOGGER.warn("RenderContext#drawInternal() [{}] renderPass --> setIndexBuffer() [{}]", this.name.get(), this.bufferIndex);
                if (this.indexBuffer == null)
                {
                    pass.setIndexBuffer(indexBuffer, this.shapeIndex.getIndexType());
                }
                else
                {
                    pass.setIndexBuffer(this.indexBuffer, this.indexType);
                }

                //MiniHUD.LOGGER.warn("RenderContext#drawInternal() [{}] renderPass --> drawIndexed() [0, {}]", this.name.get(), this.bufferIndex);
                pass.drawIndexed(0, this.indexCount);
            }

            //MiniHUD.LOGGER.warn("RenderContext#drawInternal() [{}] --> END", this.name.get());

            if (useOffset)
            {
                RenderSystem.resetModelOffset();
            }
        }
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

    protected void reset()
    {
        if (this.vertexBuffer != null)
        {
            this.vertexBuffer.close();
            this.vertexBuffer = null;
        }

        if (this.indexBuffer != null)
        {
            this.indexBuffer.close();
            this.indexBuffer = null;
        }

        if (this.sortState != null)
        {
            this.sortState = null;
        }

        if (this.builder != null)
        {
            if (((IMixinBufferBuilder) this.builder).malilib_isBuilding() &&
                ((IMixinBufferBuilder) this.builder).malilib_getVertexCount() != 0)
            {
                try
                {
                    BuiltBuffer meshData = this.builder.endNullable();

                    if (meshData != null)
                    {
                        meshData.close();
                    }
                }
                catch (Exception ignored) { }
            }

            this.builder = null;
        }

        if (this.alloc != null)
        {
            this.alloc.close();
            this.alloc = null;
        }

        this.indexCount = -1;
        this.indexType = null;
        this.textureId = -1;
        this.offset = new float[]{0f, 0f, 0f};
        this.color = -1;
        this.lineWidth = 1.0f;
        this.started = false;
    }

    protected void close()
    {
        if (this.texture != null)
        {
            this.unbindTexture(this.texture.getId());
            this.texture.close();
            this.texture = null;
        }

        this.reset();
    }
}
