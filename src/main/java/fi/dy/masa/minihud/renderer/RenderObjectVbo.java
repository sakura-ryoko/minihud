package fi.dy.masa.minihud.renderer;

import java.util.*;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.*;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.malilib.mixin.render.IMixinAbstractTexture;
import fi.dy.masa.malilib.mixin.render.IMixinBufferBuilder;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.render.texture.MaLiLibComplexBinding;
import fi.dy.masa.malilib.render.texture.MaLiLibComplexTexture;
import fi.dy.masa.minihud.MiniHUD;

/**
 * This was primarily copied from {@link fi.dy.masa.malilib.render.RenderContext}, but this way
 * the RenderContainer has full control from within MiniHUD; and
 * it is not AutoClosable so that we can dynamically re-allocate
 * it using the RenderContainer system.
 * For other non-RenderContainer purposes; use {@link fi.dy.masa.malilib.render.RenderContext}.
 */
public class RenderObjectVbo
{
    private Supplier<String> name;
	private VertexFormat format;
	private PrimitiveTopology topology;
    private RenderPipeline pipeline;
    private GpuBuffer vertexBuffer;
    @Nullable private GpuBuffer indexBuffer;
    private RenderSystem.AutoStorageIndexBuffer shapeIndex;
    private IndexType indexType;
    private ByteBufferBuilder alloc;
    private BufferBuilder builder;
	private final HashMap<Integer, SimpleTexture> simpleTextures;
	private final List<MaLiLibComplexTexture> complexTextures;
	@Nullable private MeshData.SortState sortState;
	private float[] offset;
    private int color;
	private boolean withOverlay;
	private boolean withLightmap;
    private boolean started;
    private boolean uploaded;
    private int indexCount;

    protected RenderObjectVbo(Supplier<String> name, @Nonnull RenderPipeline pipeline, final int formatIndex)
    {
        this.name = name;
	    this.format = pipeline.getVertexFormatBinding(formatIndex);
	    if (this.format == null)
	    {
		    throw new IllegalArgumentException("Invalid Vertex binding format index: "+formatIndex);
	    }
	    this.topology = pipeline.getPrimitiveTopology();
	    this.alloc = new ByteBufferBuilder(this.format.getVertexSize() * 4);
	    this.builder = new BufferBuilder(this.alloc, this.topology, this.format);
	    this.shapeIndex = RenderSystem.getSequentialBuffer(this.topology);
	    this.indexType = this.shapeIndex.type();
	    this.pipeline = pipeline;
	    this.vertexBuffer = null;
	    this.indexBuffer = null;
	    this.sortState = null;
	    this.indexCount = -1;
	    this.simpleTextures = new HashMap<>();
	    this.complexTextures = new ArrayList<>();
	    this.withOverlay = false;
	    this.withLightmap = false;
	    this.offset = new float[]{0f, 0f, 0f};
	    this.color = -1;
	    this.started = true;
	    this.uploaded = false;
    }

    public BufferBuilder start(Supplier<String> name, @Nonnull RenderPipeline shader, final int formatIndex)
    {
	    this.reset();
	    this.name = name;
	    this.format = shader.getVertexFormatBinding(formatIndex);
	    if (this.format == null)
	    {
		    throw new IllegalArgumentException("Invalid Vertex binding format index: "+formatIndex);
	    }
	    this.topology = shader.getPrimitiveTopology();
	    this.alloc = new ByteBufferBuilder(this.format.getVertexSize() * 4);
	    this.builder = new BufferBuilder(this.alloc, this.topology, this.format);
	    this.shapeIndex = RenderSystem.getSequentialBuffer(this.topology);
	    this.indexType = this.shapeIndex.type();
	    this.pipeline = shader;
	    this.vertexBuffer = null;
	    this.indexBuffer = null;
	    this.sortState = null;
	    this.indexCount = -1;
	    this.withOverlay = false;
	    this.withLightmap = false;
	    this.offset = new float[]{0f, 0f, 0f};
	    this.color = -1;
	    this.started = true;
	    this.uploaded = false;
	    return this.builder;
    }

    protected boolean isStarted() { return this.started; }

    protected boolean isUploaded() { return this.uploaded; }

    public String getName()
    {
        return this.name.get();
    }

    protected BufferBuilder getBuilder()
    {
        return this.builder;
    }

	public VertexFormat getSelectedVertexFormat()
	{
		return this.format;
	}

	public VertexFormat[] getShaderFormats()
	{
		return this.pipeline.getVertexFormatBindings();
	}

	public PrimitiveTopology getShaderTopology()
	{
		return this.pipeline.getPrimitiveTopology();
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

	public RenderObjectVbo withOverlay()
	{
		this.withOverlay = true;
		return this;
	}

	public RenderObjectVbo withLightmap()
	{
		this.withLightmap = true;
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

        try (MeshData meshData = this.builder.build())
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

        try (MeshData meshData = this.builder.build())
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

    public void upload(MeshData meshData, boolean shouldResort) throws RuntimeException
    {
        this.ensureSafeNoShader();

        if (RenderSystem.isOnRenderThread() && meshData != null)
        {
            int expectedSize = meshData.vertexBuffer().remaining();

            if (this.vertexBuffer != null)
            {
                this.vertexBuffer.close();
            }

            if (this.indexBuffer != null)
            {
                this.indexBuffer.close();
                this.indexBuffer = null;
            }

	        GpuDevice device = RenderSystem.tryGetDevice();

	        if (device == null)
	        {
		        MiniHUD.LOGGER.warn("RenderObjectVbo#upload: GpuDevice is null for renderer '{}'", this.name.get());
		        return;
	        }

            if (this.vertexBuffer == null)
            {
                this.vertexBuffer = device.createBuffer(() -> this.name.get()+" VertexBuffer", 40, expectedSize);
            }
            else if (this.vertexBuffer.size() < expectedSize)
            {
                this.vertexBuffer.close();
                this.vertexBuffer = device.createBuffer(() -> this.name.get()+" VertexBuffer", 40, expectedSize);
            }

            CommandEncoder encoder = device.createCommandEncoder();

            if (!this.vertexBuffer.isClosed())
            {
                encoder.writeToBuffer(this.vertexBuffer.slice(), meshData.vertexBuffer());
            }
            else
            {
                throw new RuntimeException("Vertex Buffer is closed!");
            }

            // Resorting
            if (shouldResort && meshData.indexBuffer() != null)
            {
                if (this.indexBuffer != null && this.indexBuffer.size() >= meshData.indexBuffer().remaining())
                {
                    if (!this.indexBuffer.isClosed())
                    {
                        encoder.writeToBuffer(this.indexBuffer.slice(), meshData.indexBuffer());
                    }
                }
                else
                {
                    if (this.indexBuffer != null)
                    {
                        this.indexBuffer.close();
                    }

                    this.indexBuffer = device.createBuffer(() -> this.name.get()+" IndexBuffer", 72, meshData.indexBuffer());
                }
            }
            else if (this.indexBuffer != null)
            {
                this.indexBuffer.close();
                this.indexBuffer = null;
            }

            this.indexCount = meshData.drawState().indexCount();
            this.indexType = meshData.drawState().indexType();
            this.uploaded = true;
//            meshData.close();
        }
    }

    /**
     * INDEX RESORTING PHASE --
     * -
     * This uploads the IndexBuffer for resorting
     */
    protected VertexSorting createVertexSorter(float x, float y, float z)
    {
        return VertexSorting.byDistance(x, y, z);
    }

    public VertexSorting createVertexSorter(Vec3 pos)
    {
        return this.createVertexSorter(pos, BlockPos.ZERO);
    }

    protected VertexSorting createVertexSorter(Camera camera)
    {
        return this.createVertexSorter(camera.position(), BlockPos.ZERO);
    }

    protected VertexSorting createVertexSorter(Camera camera, BlockPos origin)
    {
        return this.createVertexSorter(camera.position(), origin);
    }

    protected VertexSorting createVertexSorter(Vec3 pos, BlockPos origin)
    {
        return VertexSorting.byDistance((float)(pos.x - (double)origin.getX()), (float)(pos.y - (double) origin.getY()), (float)(pos.z - (double) origin.getZ()));
    }

    public void startResorting(@Nonnull MeshData meshData, @Nonnull VertexSorting sorter) throws RuntimeException
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

    protected void resortTranslucent(@Nonnull VertexSorting sorter) throws RuntimeException
    {
        this.ensureSafeNoBuffer();

        if (RenderSystem.isOnRenderThread())
        {
            if (this.sortState == null)
            {
                throw new RuntimeException("Sort State is empty!");
            }

            ByteBufferBuilder.Result result = this.sortState.buildSortedIndexBuffer(this.alloc, sorter);

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

    protected void uploadIndex(@Nonnull ByteBufferBuilder.Result buffer) throws RuntimeException
    {
        this.ensureSafeNoBuffer();

        if (RenderSystem.isOnRenderThread())
        {
            GpuDevice device = RenderSystem.tryGetDevice();

            if (device == null)
            {
                MiniHUD.LOGGER.warn("RenderObjectVbo#uploadIndex: GpuDevice is null for renderer '{}'", this.name.get());
                return;
            }

            if (this.indexBuffer == null)
            {
                this.indexBuffer = device.createBuffer(() -> this.name.get()+" IndexBuffer", 72, buffer.byteBuffer());
            }
            else
            {
                if (!this.indexBuffer.isClosed())
                {
                    device.createCommandEncoder().writeToBuffer(this.indexBuffer.slice(), buffer.byteBuffer());
                }
                else
                {
                    throw new RuntimeException("Index Buffer is closed!");
                }
            }
        }
    }

	/**
	 * BIND TEXTURE PHASE --
	 * -
	 * Performs the Texture Binding/Unbind for the "Shader Texture" layer
	 * -------------------------------------------------------------------
	 * [Sampler0] - Main or Atlas Texture
	 * [Sampler1] - Overlay Texture
	 * [Sampler2] - Lightmap Texture
	 */
	protected void bindTexture(Identifier id, int textureId, int width, int height) throws RuntimeException
	{
		this.ensureSafeNoBuffer();

		if (this.needsComplexTextures())
		{
			throw new RuntimeException("Needs Complex Textures!");
		}

		if (textureId < 0 || textureId > 12)
		{
			throw new RuntimeException("Invalid textureId of: "+textureId+" for texture: "+id.toString());
		}

		try
		{
			// Verify that we potentially have the correct texture by checking various values
			while (!this.isTextureValid(textureId, width, height))
			{
				this.simpleTextures.put(textureId, (SimpleTexture) RenderUtils.tex().getTexture(id));

				if (this.isTextureValid(textureId, width, height))
				{
					break;
				}
			}
		}
		catch (Exception err)
		{
			throw new RuntimeException("Exception reading Texture ["+id.toString()+"]: "+err.getMessage());
		}

		// General failure & cleanup
		if (this.simpleTextures.containsKey(textureId))
		{
			// Simple texture rebind since we already have a valid texture
			return;
		}

		MiniHUD.LOGGER.error("bindTexture: Error uploading texture [{}]", id.toString());

		if (this.simpleTextures.containsKey(textureId))
		{
			try (SimpleTexture tex = this.simpleTextures.remove(textureId))
			{
				tex.close();
			}
			catch (Exception ignored) {}
		}
	}

	private boolean isTextureValid(int textureId, int width, int height)
	{
		if (this.simpleTextures.isEmpty() || !this.simpleTextures.containsKey(textureId))
		{
			return false;
		}

		SimpleTexture tex = this.simpleTextures.get(textureId);

		try (TextureContents content = tex.loadContents(RenderUtils.mc().getResourceManager()))
		{
			NativeImage image = content.image();

			if (image == null || image.getWidth() != width || image.getHeight() != height)
			{
				return false;
			}
		}
		catch (Exception e)
		{
			this.simpleTextures.remove(textureId).close();
			return false;
		}

		if (((IMixinAbstractTexture) tex).malilib_getGlTextureView() == null ||
			tex.getTextureView().isClosed())
		{
			this.simpleTextures.remove(textureId).close();
			return false;
		}

		return true;
	}

	protected void unbindTexture(@Nullable Identifier id)
	{
		if (id != null)
		{
			RenderUtils.tex().release(id);
		}

		List<Integer> list = new ArrayList<>();

		for (Integer key : this.simpleTextures.keySet())
		{
			if (this.simpleTextures.get(key).resourceId().equals(id))
			{
				list.add(key);
			}
		}

		for (Integer key : list)
		{
			try (SimpleTexture tex = this.simpleTextures.remove(key))
			{
				RenderUtils.tex().release(tex.resourceId());
				tex.close();
			}
			catch (Exception ignored) {}
		}
	}

	/**
	 * COMPLEX BIND TEXTURE PHASE --
	 * -
	 * Performs the Texture Binding/Unbind for a complex "Shader Texture" layer
	 * -------------------------------------------------------------------
	 * [Sampler0] - Main or Atlas Texture
	 * [Sampler1] - Overlay Texture
	 * [Sampler2] - Lightmap Texture
	 */
	public void prepareComplexTextures(List<MaLiLibComplexBinding> additional)
	{
		this.ensureSafeNoBuffer();
		Minecraft mc = Minecraft.getInstance();
		SamplerCache samplers = RenderSystem.getSamplerCache();

		this.complexTextures.clear();

		if (this.withOverlay)
		{
			OverlayTexture overlay = mc.gameRenderer.overlayTexture();
			this.complexTextures.add(new MaLiLibComplexTexture("Sampler1", overlay.getTextureView(), samplers.getClampToEdge(FilterMode.LINEAR)));
		}
		if (this.withLightmap)
		{
			GpuTextureView lightmap = mc.gameRenderer.lightmap();
			this.complexTextures.add(new MaLiLibComplexTexture("Sampler2", lightmap, samplers.getClampToEdge(FilterMode.LINEAR)));
		}

		if (!additional.isEmpty())
		{
			TextureManager tex = mc.getTextureManager();

			additional.forEach((entry) ->
			                   {
				                   AbstractTexture texture = tex.getTexture(entry.location());
				                   GpuSampler sampler = entry.sampler().get();

				                   if (((IMixinAbstractTexture) texture).malilib_getGlTextureView() != null)
				                   {
					                   this.complexTextures.add(new MaLiLibComplexTexture(entry.name(),
					                                                                      texture.getTextureView(),
					                                                                      sampler != null ? sampler : texture.getSampler())
					                   );
				                   }
			                   });
		}
	}

	private boolean needsComplexTextures()
	{
		return this.withOverlay || this.withLightmap;
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
        MeshData meshData = this.builder.build();

        if (meshData != null)
        {
            this.draw(meshData, shouldResort);
            meshData.close();
        }
    }

	protected void draw(MeshData meshData) throws RuntimeException
	{
		this.ensureSafeNoBuffer();
		this.draw(null, meshData, false, false, false);
	}

	protected void draw(MeshData meshData, boolean shouldResort) throws RuntimeException
	{
		this.ensureSafeNoBuffer();
		this.draw(null, meshData, shouldResort, false, false);
	}

	protected void draw(MeshData meshData, boolean shouldResort, boolean setColor) throws RuntimeException
	{
		this.ensureSafeNoBuffer();
		this.draw(null, meshData, shouldResort, setColor, false);
	}

	protected void draw(MeshData meshData, boolean shouldResort, boolean setColor, boolean useOffset) throws RuntimeException
	{
		this.ensureSafeNoBuffer();
		this.draw(null, meshData, shouldResort, setColor, useOffset);
	}

	protected void draw(@Nullable RenderTarget otherFb, MeshData meshData, boolean shouldResort) throws RuntimeException
	{
		this.ensureSafeNoBuffer();
		this.draw(otherFb, meshData, shouldResort, false, false);
	}

	protected void draw(@Nullable RenderTarget otherFb, MeshData meshData, boolean shouldResort,
	                 boolean setColor, boolean useOffset) throws RuntimeException
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
                    //MiniHUD.LOGGER.warn("RenderObjectVbo#draw() [{}] --> upload()", this.name.get());
                    this.upload(meshData, shouldResort);
                }
            }

            if (this.indexCount > 0)
            {
                float[] rgba = new float[]{ARGB.redFloat(this.color), ARGB.greenFloat(this.color), ARGB.blueFloat(this.color), ARGB.alphaFloat(this.color)};

                this.drawInternal(otherFb, rgba, setColor, useOffset);
            }
        }
    }

	protected void drawPost() throws RuntimeException
	{
		this.ensureSafeNoTexture();
		this.drawPost(null, false, false);
	}

	protected void drawPost(boolean setColor) throws RuntimeException
	{
		this.ensureSafeNoTexture();
		this.drawPost(null, setColor, false);
	}

	protected void drawPost(@Nullable RenderTarget otherFb) throws RuntimeException
	{
		this.ensureSafeNoTexture();
		this.drawPost(otherFb, false, false);
	}

	protected void drawPost(@Nullable RenderTarget otherFb, boolean setColor) throws RuntimeException
	{
		this.ensureSafeNoTexture();
		this.drawPost(otherFb, setColor, false);
	}

	protected void drawPost(@Nullable RenderTarget otherFb, boolean setColor, boolean useOffset) throws RuntimeException
    {
        this.ensureSafeNoTexture();

        if (this.indexCount > 0)
        {
            float[] rgba = new float[]{ARGB.redFloat(this.color), ARGB.greenFloat(this.color), ARGB.blueFloat(this.color), ARGB.alphaFloat(this.color)};
            this.drawInternal(otherFb, rgba, setColor, useOffset);
        }
    }

    private void drawInternal(@Nullable RenderTarget otherTarget, float[] rgba, boolean setColor, boolean useOffset) throws RuntimeException
    {
        this.ensureSafeNoTexture();

        if (RenderSystem.isOnRenderThread())
        {
            Vector4f colorMod = new Vector4f(1f, 1f, 1f, 1f);
            Vector3f modelOffset = new Vector3f();
            Matrix4f texMatrix = new Matrix4f();
//            float line = 0.0f;

            if (setColor)
            {
                colorMod.set(rgba);
            }

            if (useOffset)
            {
                modelOffset.set(this.offset);
            }

            GpuDevice device = RenderSystem.getDevice();

            if (device == null)
            {
                MiniHUD.LOGGER.warn("RenderObjectVbo#drawInternal: GpuDevice is null for renderer '{}'", this.name.get());
                return;
            }

            RenderTarget mainTarget = RenderUtils.mainTarget();
            GpuTextureView texture1;
            GpuTextureView texture2;

            if (otherTarget != null)
            {
                texture1 = otherTarget.getColorTextureView();
                texture2 = otherTarget.useDepth ? otherTarget.getDepthTextureView() : null;
            }
            else
            {
                texture1 = mainTarget.getColorTextureView();
                texture2 = mainTarget.useDepth ? mainTarget.getDepthTextureView() : null;
            }

            //MiniHUD.LOGGER.warn("RenderObjectVbo#drawInternal() [{}] --> new renderPass", this.name.get());
            GpuBuffer indexBuffer = this.shapeIndex.getBuffer(this.indexCount);
            GpuBufferSlice gpuSlice = RenderSystem.getDynamicUniforms()
                                                  .writeTransform(
                                                          RenderSystem.getModelViewMatrixCopy(),
                                                          colorMod,
                                                          modelOffset,
                                                          texMatrix);

            try (RenderPass pass = device.createCommandEncoder()
                     .createRenderPass(this.name,
                                       texture1, Optional.empty(),
                                       texture2, OptionalDouble.empty()))
            {
                //MiniHUD.LOGGER.warn("RenderObjectVbo#drawInternal() [{}] renderPass --> setPipeline() [{}]", this.name.get(), this.shader.getLocation().toString());
                pass.setPipeline(this.pipeline);

                ScissorState scissorState = RenderSystem.getScissorStateForRenderTypeDraws();

                if (scissorState.enabled())
                {
                    pass.enableScissor(scissorState.x(), scissorState.y(), scissorState.width(), scissorState.height());
                }

                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", gpuSlice);

                if (this.indexBuffer == null)
                {
                    pass.setIndexBuffer(indexBuffer, this.shapeIndex.type());
                }
                else
                {
                    pass.setIndexBuffer(this.indexBuffer, this.indexType);
                }

                //MiniHUD.LOGGER.warn("RenderObjectVbo#drawInternal() [{}] renderPass --> setVertexBuffer() [0]", this.name.get());
                pass.setVertexBuffer(0, this.vertexBuffer.slice());

	            if (!this.complexTextures.isEmpty())
	            {
		            for (MaLiLibComplexTexture entry : this.complexTextures)
		            {
			            pass.bindTexture(entry.name(), entry.texture(), entry.sampler());
		            }
	            }
	            else if (!this.simpleTextures.isEmpty())
	            {
		            for (int i = 0; i < this.simpleTextures.size(); i++)
		            {
			            if (this.simpleTextures.containsKey(i))
			            {
				            SimpleTexture tex = this.simpleTextures.get(i);

				            if (tex != null)
				            {
					            pass.bindTexture("Sampler"+i, tex.getTextureView(), tex.getSampler());
				            }
			            }
		            }
	            }

                //MiniHUD.LOGGER.warn("RenderObjectVbo#drawInternal() [{}] renderPass --> drawIndexed() [0, {}]", this.name.get(), this.bufferIndex);
	            pass.drawIndexed(this.indexCount, 1, 0, 0, 0);
            }

            //MiniHUD.LOGGER.warn("RenderObjectVbo#drawInternal() [{}] --> END", this.name.get());
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

        if (this.name.get().isEmpty())
        {
            this.name = () -> "RenderObjectVbo";
        }
    }

    private void ensureSafeNoBuffer() throws RuntimeException
    {
        this.ensureSafeNoShader();

        if (this.pipeline == null)
        {
            throw new RuntimeException("Shader Pipeline not valid!");
        }
    }

    private void ensureSafeNoTexture() throws RuntimeException
    {
        this.ensureSafeNoBuffer();

        if (this.vertexBuffer == null)
        {
            throw new RuntimeException("GpuBuffer not uploaded!");
        }
    }

    private void ensureSafe()
    {
        this.ensureSafeNoTexture();

	    if (this.simpleTextures.isEmpty())
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
                    MeshData meshData = this.builder.build();

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
	    this.offset = new float[]{0f, 0f, 0f};
	    this.color = -1;
        this.started = false;
        this.uploaded = false;
    }

    protected void close()
    {
	    if (!this.simpleTextures.isEmpty())
	    {
		    for (SimpleTexture tex : this.simpleTextures.values())
		    {
			    RenderUtils.tex().release(tex.resourceId());
			    tex.close();
		    }

		    this.simpleTextures.clear();
	    }

	    if (!this.complexTextures.isEmpty())
	    {
		    this.complexTextures.clear();
	    }

        this.reset();
    }
}
