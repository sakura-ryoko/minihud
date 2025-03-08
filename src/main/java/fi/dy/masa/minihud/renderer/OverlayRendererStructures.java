package fi.dy.masa.minihud.renderer;

import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;

import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.malilib.render.RenderContext;
import fi.dy.masa.minihud.MiniHUD;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
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
import net.minecraft.util.profiler.Profiler;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

public class OverlayRendererStructures extends OverlayRendererBase
{
    public static final OverlayRendererStructures INSTANCE = new OverlayRendererStructures();
    private List<StructureData> structures;

    private OverlayRendererStructures()
    {
        this.structures = new ArrayList<>();
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
    public void update(Vec3d cameraPos, Entity entity, MinecraftClient mc)
    {
        int maxRange = (mc.options.getViewDistance().getValue() + 4) * 16;
        this.structures = this.getStructuresToRender(this.lastUpdatePos, maxRange);

//        if (data.isEmpty() == false)
//        {
//            if (this.wasEmpty)
//            {
//                this.allocateGlResources();
//            }

//            RenderObjectBase renderQuads = this.renderObjects.get(0);
//            RenderObjectBase renderLines = this.renderObjects.get(1);

//            BufferBuilder builder1 = CONTEXT_1.startNoShader(VertexFormats.POSITION_COLOR, renderQuads.getGlMode());
//            BufferBuilder builder2 = CONTEXT_2.startNoShader(VertexFormats.POSITION_COLOR, renderLines.getGlMode());
//            CONTEXT_1.setShader(MaLiLibPipelines.POSITION_COLOR_SIMPLE);
//            CONTEXT_2.setShader(MaLiLibPipelines.POSITION_COLOR_SIMPLE);

//            this.renderStructureBoxes(data, cameraPos, builder1, builder2);

//            CONTEXT_1 = CONTEXT_1.setBuilder(builder1);
//            CONTEXT_2 = CONTEXT_2.setBuilder(builder2);
//
//            renderQuads.uploadData(builder1);
//            renderLines.uploadData(builder2);

//            this.wasEmpty = false;
//        }
//        else
//        {
//            this.deleteGlResources();
//            this.wasEmpty = true;
//        }
    }

    @Override
    public boolean hasData()
    {
        return !this.structures.isEmpty();
    }

    @Override
    public void render(Camera camera, Matrix4f matrix4f, Matrix4f projMatrix, MinecraftClient mc, Profiler profiler)
    {
        if (this.hasData())
        {
            this.renderStructureMain(camera, matrix4f, projMatrix, mc, profiler);
            this.renderStructureComponents(camera, matrix4f, projMatrix, mc, profiler);
        }
    }

    private void renderStructureMain(Camera camera, Matrix4f matrix4f, Matrix4f projMatrix, MinecraftClient mc, Profiler profiler)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        profiler.push("structure main");

        RenderContext ctx = new RenderContext(() -> "Structure Main", MaLiLibPipelines.POSITION_COLOR_SIMPLE, GlUsage.STATIC_WRITE);
        BufferBuilder builder = ctx.getBuilder();

        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        Vec3d updatePos = this.getUpdatePosition();
        Vec3d cameraPos = camera.getPos();

        this.preRender();
        matrix4fstack.pushMatrix();
        matrix4fstack.translate((float) (updatePos.x - cameraPos.x), (float) (updatePos.y - cameraPos.y), (float) (updatePos.z - cameraPos.z));

        for (StructureData structure : this.structures)
        {
            StructureToggle toggle = structure.getStructureType().getToggle();
            Color4f mainColor = toggle.getColorMain().getColor();
            IntBoundingBox bb = structure.getBoundingBox();

            float minX = (float)((double)bb.minX - cameraPos.x);
            float minY = (float)((double)bb.minY - cameraPos.y);
            float minZ = (float)((double)bb.minZ - cameraPos.z);
            float maxX = (float)((double)(bb.maxX + 1) - cameraPos.x);
            float maxY = (float)((double)(bb.maxY + 1) - cameraPos.y);
            float maxZ = (float)((double)(bb.maxZ + 1) - cameraPos.z);

            fi.dy.masa.malilib.render.RenderUtils.drawBoxAllSidesBatchedQuads(minX, minY, minZ, maxX, maxY, maxZ, mainColor, builder);
        }

        try
        {
            ctx.drawColor(builder.endNullable());
            ctx.close();
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererStructures#renderStructureMain(): Exception; {}", err.getMessage());
        }

        this.postRender();
        matrix4fstack.popMatrix();
        profiler.pop();
    }

    private void renderStructureComponents(Camera camera, Matrix4f matrix4f, Matrix4f projMatrix, MinecraftClient mc, Profiler profiler)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        profiler.push("structure components");
        RenderContext ctx = new RenderContext(() -> "Structure Components", MaLiLibPipelines.POSITION_COLOR_SIMPLE, GlUsage.STATIC_WRITE);
        BufferBuilder builder = ctx.getBuilder();

        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        Vec3d updatePos = this.getUpdatePosition();
        Vec3d cameraPos = camera.getPos();

        this.preRender();
        matrix4fstack.pushMatrix();
        matrix4fstack.translate((float) (updatePos.x - cameraPos.x), (float) (updatePos.y - cameraPos.y), (float) (updatePos.z - cameraPos.z));

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
                        float minX = (float)((double)bb.minX - cameraPos.x);
                        float minY = (float)((double)bb.minY - cameraPos.y);
                        float minZ = (float)((double)bb.minZ - cameraPos.z);
                        float maxX = (float)((double)(bb.maxX + 1) - cameraPos.x);
                        float maxY = (float)((double)(bb.maxY + 1) - cameraPos.y);
                        float maxZ = (float)((double)(bb.maxZ + 1) - cameraPos.z);

                        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllSidesBatchedQuads(minX, minY, minZ, maxX, maxY, maxZ, componentColor, builder);
                    }
                }
            }
        }

        try
        {
            ctx.drawColor(builder.endNullable());
            ctx.close();
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererStructures#renderStructureComponents(): Exception; {}", err.getMessage());
        }

        this.postRender();
        matrix4fstack.popMatrix();
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
