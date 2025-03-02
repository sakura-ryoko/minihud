package fi.dy.masa.minihud.renderer;

import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.config.StructureToggle;
import fi.dy.masa.minihud.util.DataStorage;
import fi.dy.masa.minihud.util.MiscUtils;
import fi.dy.masa.minihud.util.StructureData;
import fi.dy.masa.minihud.util.StructureType;

public class OverlayRendererStructures extends OverlayRendererBase
{
    public static final OverlayRendererStructures INSTANCE = new OverlayRendererStructures();
    private boolean wasEmpty = true;

    private OverlayRendererStructures()
    {
    }

    @Override
    public String getName()
    {
        return "Structures";
    }

    @Override
    public boolean shouldRender(MinecraftClient mc)
    {
        if (RendererToggle.OVERLAY_STRUCTURE_MAIN_TOGGLE.getBooleanValue() == false)
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
    public void update(Vec3d cameraPos, Entity entity, MinecraftClient mc)
    {
        int maxRange = (mc.options.getViewDistance().getValue() + 4) * 16;
        List<StructureData> data = this.getStructuresToRender(this.lastUpdatePos, maxRange);

        if (data.isEmpty() == false)
        {
            if (this.wasEmpty)
            {
                this.allocateGlResources();
            }

            RenderObjectBase renderQuads = this.renderObjects.get(0);
            RenderObjectBase renderLines = this.renderObjects.get(1);
            /*
            BUFFER_1 = TESSELLATOR_1.begin(renderQuads.getGlMode(), VertexFormats.POSITION_COLOR);
            BUFFER_2 = TESSELLATOR_2.begin(renderLines.getGlMode(), VertexFormats.POSITION_COLOR);
             */

            BufferBuilder builder1 = CONTEXT_1.startNoShader(VertexFormats.POSITION_COLOR, renderQuads.getGlMode());
            BufferBuilder builder2 = CONTEXT_2.startNoShader(VertexFormats.POSITION_COLOR, renderLines.getGlMode());
            CONTEXT_1.setShader(MaLiLibPipelines.POSITION_COLOR_SIMPLE);
            CONTEXT_2.setShader(MaLiLibPipelines.POSITION_COLOR_SIMPLE);

            this.renderStructureBoxes(data, cameraPos, builder1, builder2);

            CONTEXT_1 = CONTEXT_1.setBuilder(builder1);
            CONTEXT_2 = CONTEXT_2.setBuilder(builder2);

            renderQuads.uploadData(builder1);
            renderLines.uploadData(builder2);

            this.wasEmpty = false;
        }
        else
        {
            this.deleteGlResources();
            this.wasEmpty = true;
        }
    }

    private void renderStructureBoxes(List<StructureData> wrappedData, Vec3d cameraPos,
                                      BufferBuilder builder1, BufferBuilder builder2)
    {
        for (StructureData data : wrappedData)
        {
            StructureToggle toggle = data.getStructureType().getToggle();
            Color4f mainColor = toggle.getColorMain().getColor();
            Color4f componentColor = toggle.getColorComponents().getColor();
            this.renderStructure(data, mainColor, componentColor, cameraPos, builder1, builder2);
        }
    }

    private void renderStructure(StructureData structure, Color4f mainColor, Color4f componentColor, Vec3d cameraPos,
                                 BufferBuilder builder1, BufferBuilder builder2)
    {
        fi.dy.masa.malilib.render.RenderUtils.drawBox(structure.getBoundingBox(), cameraPos, mainColor, builder1, builder2);

        ImmutableList<IntBoundingBox> components = structure.getComponents();

        if (components.isEmpty() == false)
        {
            if (components.size() > 1 || MiscUtils.areBoxesEqual(components.get(0), structure.getBoundingBox()) == false)
            {
                for (IntBoundingBox bb : components)
                {
                    fi.dy.masa.malilib.render.RenderUtils.drawBox(bb, cameraPos, componentColor, builder1, builder2);
                }
            }
        }
    }

    private List<StructureData> getStructuresToRender(BlockPos playerPos, int maxRange)
    {
        ArrayListMultimap<StructureType, StructureData> structures = DataStorage.getInstance().getCopyOfStructureDataWithinRange(playerPos, maxRange);
        List<StructureData> data = new ArrayList<>();

        for (StructureType type : structures.keySet())
        {
            if (type.isEnabled() == false)
            {
                continue;
            }

            data.addAll(structures.get(type));
        }

        return data;
    }
}
