package fi.dy.masa.minihud.renderer;

import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;

import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.config.StructureToggle;
import fi.dy.masa.minihud.util.DataStorage;
import fi.dy.masa.minihud.util.MiscUtils;
import fi.dy.masa.minihud.util.StructureData;
import fi.dy.masa.minihud.util.StructureType;

public class OverlayRendererStructures extends OverlayRendererBase
{
    public static final OverlayRendererStructures INSTANCE = new OverlayRendererStructures();
    private List<StructureData> structures;
    private boolean hasData;
	private boolean renderOutlines;

    private OverlayRendererStructures()
    {
        this.structures = new ArrayList<>();
        this.hasData = false;
		this.useCulling = false;
		this.renderOutlines = false;
    }

    @Override
    public String getName()
    {
        return "Structures";
    }

    @Override
    public boolean shouldRender(MinecraftClient mc)
    {
        if (!RendererToggle.OVERLAY_STRUCTURE_MAIN_TOGGLE.getBooleanValue())
        {
            return false;
        }

        for (StructureType type : StructureType.VALUES)
        {
            if (type.isEnabled())
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean needsUpdate(Entity entity, MinecraftClient mc)
    {
        int hysteresis = 16;

        return DataStorage.getInstance().structureRendererNeedsUpdate() ||
               Math.abs(entity.getX() - this.lastUpdatePos.getX()) > hysteresis ||
               Math.abs(entity.getY() - this.lastUpdatePos.getY()) > hysteresis ||
               Math.abs(entity.getZ() - this.lastUpdatePos.getZ()) > hysteresis;
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity, MinecraftClient mc, Profiler profiler)
    {
        int maxRange = (mc.options.getViewDistance().getValue() + 4) * 16;
        this.structures = this.getStructuresToRender(this.lastUpdatePos, maxRange);
        this.hasData = !this.structures.isEmpty();
        this.renderThrough = Configs.Generic.STRUCTURES_RENDER_THROUGH.getBooleanValue();
		this.renderOutlines = Configs.Generic.STRUCTURES_RENDER_OUTLINES.getBooleanValue();

        if (this.hasData())
        {
            this.render(cameraPos, mc, profiler);
        }
    }

    @Override
    public boolean hasData()
    {
        return this.hasData && !this.structures.isEmpty();
    }

    @Override
    protected void allocateBuffers(boolean useOutlines)
    {
        this.clearBuffers();
        this.renderObjects.add(this.createQuadsVbo());
        this.renderObjects.add(this.createQuadsVbo());

		if (this.renderOutlines)
		{
			this.renderObjects.add(this.createOutlinesVbo());
			this.renderObjects.add(this.createOutlinesVbo());
		}
    }

    @Override
    public void render(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        this.allocateBuffers();
        this.renderStructureMain(cameraPos, mc, profiler);
        this.renderStructureComponents(cameraPos, mc, profiler);

		if (this.renderOutlines)
		{
			this.renderStructureMainOutlines(cameraPos, mc, profiler);
			this.renderStructureComponentOutlines(cameraPos, mc, profiler);
		}
    }

    private void renderStructureMain(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        profiler.push("structure_main_quads");
        RenderObjectVbo ctx = this.renderObjects.getFirst();
        BufferBuilder builder = ctx.start(
		        () -> "minihud:structure/main_quads",
		        VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR, ShaderProgramKeys.POSITION_COLOR, GlUsage.STATIC_WRITE
        );

        for (StructureData structure : this.structures)
        {
            StructureToggle toggle = structure.getStructureType().getToggle();
            Color4f mainColor = toggle.getColorMain().getColor();
            IntBoundingBox bb = structure.getBoundingBox();

            RenderUtils.drawBoxNoOutlines(bb, cameraPos, mainColor, builder);
        }

        try
        {
            BuiltBuffer meshData = builder.endNullable();

            if (meshData != null)
            {
                ctx.upload(meshData);

                if (this.shouldResort)
                {
                    ctx.startResorting(meshData, ctx.createVertexSorter(cameraPos));
                }

                meshData.close();
            }
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererStructures#renderStructureMainQuads(): Exception; {}", err.getMessage());
        }

        profiler.pop();
    }

	private void renderStructureMainOutlines(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
	{
		if (mc.world == null || mc.player == null)
		{
			return;
		}

		profiler.push("structure_main_outlines");
		RenderObjectVbo ctx = this.renderObjects.get(2);
		BufferBuilder builder = ctx.start(
				() -> "minihud:structure/main_outlines",
				VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR, ShaderProgramKeys.POSITION_COLOR, GlUsage.STATIC_WRITE
		);

		for (StructureData structure : this.structures)
		{
//			StructureToggle toggle = structure.getStructureType().getToggle();
//			Color4f mainColor = toggle.getColorMain().getColor();
			IntBoundingBox bb = structure.getBoundingBox();

			RenderUtils.drawBoxOutlines(bb, cameraPos, Color4f.WHITE, builder);
		}

		try
		{
			BuiltBuffer meshData = builder.endNullable();

			if (meshData != null)
			{
				ctx.upload(meshData);
				meshData.close();
			}
		}
		catch (Exception err)
		{
			MiniHUD.LOGGER.error("OverlayRendererStructures#renderStructureMainOutlines(): Exception; {}", err.getMessage());
		}

		profiler.pop();
	}

	private void renderStructureComponents(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        // ShaderPipelines.DEBUG_QUADS
        profiler.push("structure_component_quads");
        RenderObjectVbo ctx = this.renderObjects.get(1);
        BufferBuilder builder = ctx.start(
				() -> "minihud:structure/component_quads",
				VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR, ShaderProgramKeys.POSITION_COLOR, GlUsage.STATIC_WRITE
        );

        for (StructureData structure : this.structures)
        {
            StructureToggle toggle = structure.getStructureType().getToggle();
            Color4f componentColor = toggle.getColorComponents().getColor();
            ImmutableList<IntBoundingBox> components = structure.getComponents();

            if (!components.isEmpty())
            {
                if (components.size() > 1 || !MiscUtils.areBoxesEqual(components.getFirst(), structure.getBoundingBox()))
                {
                    for (IntBoundingBox bb : components)
                    {
                        RenderUtils.drawBoxNoOutlines(bb, cameraPos, componentColor, builder);
                    }
                }
            }
        }

        try
        {
            BuiltBuffer meshData = builder.endNullable();

            if (meshData != null)
            {
                ctx.upload(meshData);

                if (this.shouldResort)
                {
                    ctx.startResorting(meshData, ctx.createVertexSorter(cameraPos));
                }

                meshData.close();
            }
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererStructures#renderStructureComponents(): Exception; {}", err.getMessage());
        }

        profiler.pop();
    }

	private void renderStructureComponentOutlines(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
	{
		if (mc.world == null || mc.player == null)
		{
			return;
		}

		// ShaderPipelines.DEBUG_QUADS
		profiler.push("structure_component_outlines");
		RenderObjectVbo ctx = this.renderObjects.get(3);
		BufferBuilder builder = ctx.start(
				() -> "minihud:structure/component_outlines",
				VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR, ShaderProgramKeys.POSITION_COLOR, GlUsage.STATIC_WRITE
		);

		for (StructureData structure : this.structures)
		{
//			StructureToggle toggle = structure.getStructureType().getToggle();
//			Color4f componentColor = toggle.getColorComponents().getColor();
			ImmutableList<IntBoundingBox> components = structure.getComponents();

			if (!components.isEmpty())
			{
				if (components.size() > 1 || !MiscUtils.areBoxesEqual(components.getFirst(), structure.getBoundingBox()))
				{
					for (IntBoundingBox bb : components)
					{
						RenderUtils.drawBoxOutlines(bb, cameraPos, Color4f.WHITE, builder);
					}
				}
			}
		}

		try
		{
			BuiltBuffer meshData = builder.endNullable();

			if (meshData != null)
			{
				ctx.upload(meshData);
				meshData.close();
			}
		}
		catch (Exception err)
		{
			MiniHUD.LOGGER.error("OverlayRendererStructures#renderStructureComponentOutlines(): Exception; {}", err.getMessage());
		}

		profiler.pop();
	}

	@Override
    public void reset()
    {
        super.reset();
        this.structures.clear();
    }

    private List<StructureData> getStructuresToRender(BlockPos playerPos, int maxRange)
    {
        ArrayListMultimap<StructureType, StructureData> structures = DataStorage.getInstance().getCopyOfStructureDataWithinRange(playerPos, maxRange);
        List<StructureData> data = new ArrayList<>();

        for (StructureType type : structures.keySet())
        {
            if (!type.isEnabled())
            {
                continue;
            }

            data.addAll(structures.get(type));
        }

        return data;
    }
}
