package fi.dy.masa.minihud.renderer;

import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.mixin.render.IMixinBufferBuilder;

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
    private Supplier<ShaderProgram> shader;       // todo 1.21.3+ ShaderProgramKey, 1.21.5+ RenderPipeline
    private VertexBuffer.Usage usage;             // todo 1.21.3+ GlUsage, 1.21.8+ (Removed)
    private VertexBuffer vertexBuffer;            // todo 1.21.5+ (Removed)
    private BufferAllocator alloc;
    private BufferBuilder builder;
    private VertexFormat format;
    private VertexFormat.DrawMode drawMode;
    @Nullable private BuiltBuffer.SortState sortState;
    private boolean started;
    private int vertexCount;
    private int indexCount;

    protected RenderObjectVbo(Supplier<String> name, VertexFormat.DrawMode glMode, VertexFormat format, Supplier<ShaderProgram> shader, VertexBuffer.Usage usage)
            throws RuntimeException
    {
        this.name = name;
        this.format = format;
        this.drawMode = glMode;
        this.shader = shader;
        this.usage = usage;
        this.ensureSafeShaderFormat();
        this.alloc = new BufferAllocator(format.getVertexSizeByte() * 4);
        this.builder = new BufferBuilder(this.alloc, glMode, format);
        this.vertexBuffer = null;
        this.sortState = null;
        this.vertexCount = -1;
        this.indexCount = -1;
        this.started = true;
    }

    protected boolean isStarted() { return this.started; }

    public String getName()
    {
        return this.name.get();
    }

    protected VertexBuffer.Usage getUsage()
    {
        return this.usage;
    }

    protected VertexFormat getVertexFormat()
    {
        return this.format;
    }

    protected VertexFormat.DrawMode getDrawMode()
    {
        return this.drawMode;
    }

    protected Supplier<ShaderProgram> getShader()
    {
        return this.shader;
    }

    protected String getShaderId()
    {
        if (this.shader != null)
        {
            return this.shader.get().getName();
        }

        return Identifier.of(Reference.MOD_ID, "no_shader").toString();
    }

    protected VertexFormat getShaderFormat()
    {
        if (this.shader != null)
        {
            return this.shader.get().getFormat();
        }

        return this.format;
    }

    protected VertexFormat.DrawMode getGlDrawMode()
    {
        return this.drawMode;
    }

    /**
     * BUILDER PHASE --
     * -
     * This is to simply ensure that the builder is stored again
     * @return ()
     */

    public BufferBuilder start(Supplier<String> name, VertexFormat.DrawMode glMode, VertexFormat format, Supplier<ShaderProgram> shader, VertexBuffer.Usage usage)
            throws RuntimeException
    {
        this.reset();
        this.name = name;
        this.format = format;
        this.drawMode = glMode;
        this.shader = shader;
        this.usage = usage;
        this.ensureSafeShaderFormat();
        this.alloc = new BufferAllocator(format.getVertexSizeByte() * 4);
        this.builder = new BufferBuilder(this.alloc, glMode, format);
        this.vertexBuffer = null;
        this.sortState = null;
        this.vertexCount = -1;
        this.indexCount = -1;
        this.started = true;

        if (Reference.RENDER_DEBUG)
        {
            MiniHUD.LOGGER.info("RenderObjectVbo: start [{}], DrawMode: [{}], Format: [{}/size: {}], Shader: [{}]",
                                this.name.get(), this.drawMode.name(),
                                this.format.toString(), this.format.getVertexSizeByte(),
                                this.shader.get().getName());
        }

        return this.builder;
    }

    protected BufferBuilder getBuilder()
    {
        return this.builder;
    }

    /**
     * UPLOAD PHASE --
     * -
     * This uploads the BufferBuilder to the GpuBuffer for Drawing
     */
    protected void upload() throws RuntimeException
    {
        this.ensureSafeNoShader();
        this.ensureBuilding(this.builder);

        try (BuiltBuffer meshData = this.builder.endNullable())
        {
            if (meshData != null)
            {
                this.upload(meshData);
            }
            else
            {
                throw new RuntimeException("Empty Mesh Data!");
            }
        }
    }

//    protected void upload(BufferBuilder builder) throws RuntimeException
//    {
//        this.ensureSafeNoShader();
//        this.ensureBuilding(builder);
//        this.builder = builder;
//
//        try (BuiltBuffer meshData = this.builder.endNullable())
//        {
//            if (meshData != null)
//            {
//                this.upload(meshData);
//            }
//            else
//            {
//                throw new RuntimeException("Empty Mesh Data!");
//            }
//        }
//    }

    public void upload(BuiltBuffer meshData) throws RuntimeException
    {
        this.ensureSafeNoBuffer();

        if (RenderSystem.isOnRenderThread() && meshData != null)
        {
//            int expectedSize = meshData.getBuffer().remaining();

            if (this.vertexBuffer != null)
            {
                this.vertexBuffer.close();
                this.vertexBuffer = null;
            }

            this.vertexBuffer = new VertexBuffer(this.getUsage());

//            else if (((IMixinVertexBuffer) this.vertexBuffer).minihud_getIndexCount() < expectedSize)
//            {
//                this.vertexBuffer.close();
//                this.vertexBuffer = new VertexBuffer(this.getUsage());
//            }

            if (!this.vertexBuffer.isClosed())
            {
                this.vertexBuffer.bind();
                this.vertexBuffer.upload(meshData);
                VertexBuffer.unbind();
            }
            else
            {
                throw new RuntimeException("Vertex Buffer is closed!");
            }

            // todo the legacy `VertexBuffer` object handles most of this for us.
//            // Resorting
//            if (shouldResort && meshData.getSortedBuffer() != null)
//            {
//                if (this.indexBuffer != null && this.indexBuffer.size() >= meshData.getSortedBuffer().remaining())
//                {
//                    if (!this.indexBuffer.isClosed())
//                    {
//                        encoder.writeToBuffer(this.indexBuffer, meshData.getSortedBuffer(), 0);
//                    }
//                }
//                else
//                {
//                    if (this.indexBuffer != null)
//                    {
//                        this.indexBuffer.close();
//                    }
//
//                    this.indexBuffer = RenderSystem.getDevice().createBuffer(() -> this.name.get()+" IndexBuffer", BufferType.INDICES, this.usage, meshData.getSortedBuffer());
//                }
//            }
//            else if (this.indexBuffer != null)
//            {
//                this.indexBuffer.close();
//                this.indexBuffer = null;
//            }

            this.vertexCount = meshData.getDrawParameters().vertexCount();
            this.indexCount = meshData.getDrawParameters().indexCount();

            if (Reference.RENDER_DEBUG)
            {
                MiniHUD.LOGGER.info("RenderObjectVbo: [{}] --> upload() [vC: {}, iC: {}] // sortState: [{}]", this.name.get(), this.vertexCount, this.indexCount,
                                    meshData.getSortedBuffer() != null ? meshData.getSortedBuffer().position() : "<NUL>");
            }
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

                if (Reference.RENDER_DEBUG)
                {
                    MiniHUD.LOGGER.info("RenderObjectVbo: [{}] --> resortTranslucent()", this.name.get());
                }
            }
            else
            {
                throw new RuntimeException("Unable to Store Sorting Data in Result Buffer!");
            }
        }
    }

    protected void uploadIndex(@Nonnull BufferAllocator.CloseableBuffer buffer) throws RuntimeException
    {
        this.ensureSafeVertexBuffer();

        if (RenderSystem.isOnRenderThread())
        {
            this.vertexBuffer.bind();
            this.vertexBuffer.uploadIndexBuffer(buffer);
            VertexBuffer.unbind();

            if (Reference.RENDER_DEBUG)
            {
                MiniHUD.LOGGER.info("RenderObjectVbo: [{}] --> uploadIndex()", this.name.get());
            }
        }
    }

    /**
     * DRAW PHASE --
     * -
     * Performs the Renderer draw to the specified Frame Buffer
     */
//    protected void draw() throws RuntimeException
//    {
//        this.ensureSafeNoBuffer();
//        this.ensureBuilding(this.builder);
//        BuiltBuffer meshData = this.builder.endNullable();
//
//        if (meshData != null)
//        {
//            this.draw(meshData);
//            meshData.close();
//        }
//    }

    protected void draw(BuiltBuffer meshData) throws RuntimeException
    {
        this.ensureSafeNoBuffer();
        this.draw(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), meshData);
    }

    protected void draw(Matrix4f modelView, Matrix4f projectionView, BuiltBuffer meshData)
            throws RuntimeException
    {
        this.ensureSafeNoBuffer();

        if (RenderSystem.isOnRenderThread())
        {
            if (meshData == null)
            {
                this.vertexCount = 0;
                this.indexCount = 0;
            }
            else
            {
                if (this.vertexCount < 1 && this.indexCount < 1)
                {
                    this.upload(meshData);
                }
            }

            if (this.vertexCount > 0 && this.indexCount > 0)
            {
                this.drawInternal(modelView, projectionView);
            }
        }
    }

    protected void drawPost(Matrix4f modelView, Matrix4f projectionView)
            throws RuntimeException
    {
        if (this.vertexCount > 0 && this.indexCount > 0)
        {
            this.drawInternal(modelView, projectionView);
        }
    }

    private void drawInternal(Matrix4f modelView, Matrix4f projectionView)
            throws RuntimeException
    {
        this.ensureSafeNoBuffer();

        if (RenderSystem.isOnRenderThread())
        {
            RenderSystem.setShader(this.shader);
            this.vertexBuffer.bind();
            this.vertexBuffer.draw(modelView, projectionView, this.shader.get());
            VertexBuffer.unbind();

//            if (Reference.RENDER_DEBUG)
//            {
//                MiniHUD.LOGGER.info("RenderObjectVbo: drawInternal [{}]", this.name.get());
//            }
        }
    }

    private void ensureBuilding(BufferBuilder builder) throws RuntimeException
    {
        if (!((IMixinBufferBuilder) builder).minihud_isBuilding())
        {
            throw new RuntimeException("Buffer Builder is not building!");
        }
        else if (((IMixinBufferBuilder) builder).minihud_getVertexCount() == 0)
        {
            throw new RuntimeException("Buffer Builder vertices are zero!");
        }
        else if (((IMixinBufferBuilder) builder).minihud_getVertexPointer() == -1L)
        {
            throw new RuntimeException("Buffer Builder has no vertices!");
        }
    }

    private void ensureSafeVertexBuffer() throws RuntimeException
    {
        this.ensureSafeNoBuffer();

        if (this.vertexBuffer == null)
        {
            throw new RuntimeException("Vertex buffer not valid!");
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

    private void ensureSafeShaderFormat() throws RuntimeException
    {
        if (shader != null &&
            !this.shader.get().getFormat().equals(this.format))
        {
            throw new RuntimeException("Vertex Format does not match shader format");
        }
    }

    protected void reset()
    {
        if (this.vertexBuffer != null)
        {
            this.vertexBuffer.close();
            this.vertexBuffer = null;
        }

        if (this.sortState != null)
        {
            this.sortState = null;
        }

        if (this.builder != null)
        {
            if (((IMixinBufferBuilder) this.builder).minihud_isBuilding() &&
                ((IMixinBufferBuilder) this.builder).minihud_getVertexCount() != 0)
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

        this.vertexCount = -1;
        this.indexCount = -1;
        this.started = false;
    }

    protected void close()
    {
        this.reset();
    }
}
