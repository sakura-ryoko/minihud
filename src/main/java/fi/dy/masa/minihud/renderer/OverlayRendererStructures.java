package fi.dy.masa.minihud.renderer;

import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;

import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.data.Color4f;
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

    private OverlayRendererStructures()
    {
        this.structures = new ArrayList<>();
        this.hasData = false;
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
    public void render(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        this.allocateBuffers();
        this.renderStructureMain(cameraPos, mc, profiler);
        this.renderStructureComponents(cameraPos, mc, profiler);
    }

    private void renderStructureMain(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        profiler.push("structure main");
        RenderObjectVbo ctx = this.renderObjects.getFirst();
        BufferBuilder builder = ctx.start(() -> "Structure Main", this.renderThrough ? MaLiLibPipelines.getPositionColorSimple() : MaLiLibPipelines.POSITION_COLOR_MASA_GREATER_DEPTH, BufferUsage.STATIC_WRITE);
        MatrixStack matrices = new MatrixStack();

        matrices.push();
        MatrixStack.Entry e = matrices.peek();

        for (StructureData structure : this.structures)
        {
            StructureToggle toggle = structure.getStructureType().getToggle();
            Color4f mainColor = toggle.getColorMain().getColor();
            IntBoundingBox bb = structure.getBoundingBox();

            RenderUtils.drawBoxNoOutlines(bb, cameraPos, mainColor, builder, e);
        }

        try
        {
            ctx.upload(builder.endNullable(), BufferType.VERTICES);
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererStructures#renderStructureMain(): Exception; {}", err.getMessage());
        }

        matrices.pop();
        profiler.pop();
    }

    private void renderStructureComponents(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        // ShaderPipelines.DEBUG_QUADS
        profiler.push("structure components");
        RenderObjectVbo ctx = this.renderObjects.get(1);
        BufferBuilder builder = ctx.start(() -> "Structure Components", this.renderThrough ? MaLiLibPipelines.getPositionColorSimple() : MaLiLibPipelines.POSITION_COLOR_MASA_LESSER_DEPTH, BufferUsage.STATIC_WRITE);
        MatrixStack matrices = new MatrixStack();

        matrices.push();
        MatrixStack.Entry e = matrices.peek();

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
                        RenderUtils.drawBoxNoOutlines(bb, cameraPos, componentColor, builder, e);
                    }
                }
            }
        }

        try
        {
            ctx.upload(builder.endNullable(), BufferType.VERTICES);
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererStructures#renderStructureComponents(): Exception; {}", err.getMessage());
        }

        matrices.pop();
        profiler.pop();
    }

    @Override
    public void reset()
    {
        super.reset();
        this.structures.clear();
    }

//    private void renderStructureBoxes(List<StructureData> wrappedData, Vec3d cameraPos,
//                                      BufferBuilder builder1, BufferBuilder builder2)
//    {
//        for (StructureData data : wrappedData)
//        {
//            StructureToggle toggle = data.getStructureType().getToggle();
//            Color4f mainColor = toggle.getColorMain().getColor();
//            Color4f componentColor = toggle.getColorComponents().getColor();
//            this.renderStructure(data, mainColor, componentColor, cameraPos, builder1, builder2);
//        }
//    }
//
//    private void renderStructure(StructureData structure, Color4f mainColor, Color4f componentColor, Vec3d cameraPos,
//                                 BufferBuilder builder1, BufferBuilder builder2)
//    {
//        fi.dy.masa.malilib.render.RenderUtils.drawBox(structure.getBoundingBox(), cameraPos, mainColor, builder1, builder2);
//
//        ImmutableList<IntBoundingBox> components = structure.getComponents();
//
//        if (components.isEmpty() == false)
//        {
//            if (components.size() > 1 || MiscUtils.areBoxesEqual(components.get(0), structure.getBoundingBox()) == false)
//            {
//                for (IntBoundingBox bb : components)
//                {
//                    fi.dy.masa.malilib.render.RenderUtils.drawBox(bb, cameraPos, componentColor, builder1, builder2);
//                }
//            }
//        }
//    }

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
