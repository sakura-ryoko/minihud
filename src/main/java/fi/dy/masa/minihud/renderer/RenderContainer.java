package fi.dy.masa.minihud.renderer;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonObject;
import org.joml.Matrix4f;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;

import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.position.PositionUtils;

public class RenderContainer
{
    public static final RenderContainer INSTANCE = new RenderContainer();
    private final List<OverlayRendererBase> renderers = new ArrayList<>();
    protected int countActive;

    private RenderContainer()
    {
        this.addRenderer(OverlayRendererBeaconRange.INSTANCE);
        this.addRenderer(OverlayRendererBiomeBorders.INSTANCE);
        this.addRenderer(OverlayRendererBlockGrid.INSTANCE);
        this.addRenderer(OverlayRendererConduitRange.INSTANCE);
        this.addRenderer(OverlayRendererLightLevel.INSTANCE);
        this.addRenderer(OverlayRendererHandheldBeaconRange.INSTANCE);
        this.addRenderer(OverlayRendererRandomTickableChunks.INSTANCE_FIXED);
        this.addRenderer(OverlayRendererRandomTickableChunks.INSTANCE_PLAYER);
        this.addRenderer(OverlayRendererRegion.INSTANCE);
        this.addRenderer(OverlayRendererSlimeChunks.INSTANCE);
        this.addRenderer(OverlayRendererSpawnableColumnHeights.INSTANCE);
        this.addRenderer(OverlayRendererSpawnChunks.INSTANCE_PLAYER);
        this.addRenderer(OverlayRendererSpawnChunks.INSTANCE_REAL);
        this.addRenderer(OverlayRendererStructures.INSTANCE);
        this.addRenderer(OverlayRendererVillagerInfo.INSTANCE);
    }

    public void addRenderer(OverlayRendererBase renderer)
    {
        this.renderers.add(renderer);
    }

    public void removeRenderer(OverlayRendererBase renderer)
    {
        this.renderers.remove(renderer);
    }

    protected void updateIfNeeded(Vec3d cameraPos, Entity entity, MinecraftClient mc, Profiler profiler)
    {
        profiler.swap("render_update");

        this.countActive = 0;

        for (OverlayRendererBase renderer : this.renderers)
        {
            profiler.push("update_"+renderer.getName());

            if (renderer.shouldRender(mc))
            {
                if (renderer.needsUpdate(entity, mc))
                {
//                    MiniHUD.LOGGER.error("Container: renderer [{}] needs update!", renderer.getName());
                    renderer.lastUpdatePos = PositionUtils.getEntityBlockPos(entity);
                    renderer.update(cameraPos, entity, mc);
                    renderer.setUpdatePosition(cameraPos);
                }

                ++this.countActive;
            }
            else
            {
                renderer.reset();
            }

            profiler.pop();
        }
    }

    protected void render(Camera camera, Matrix4f posMatrix, Matrix4f projMatrix, MinecraftClient mc, Profiler profiler)
    {
        profiler.swap("render");

        for (OverlayRendererBase renderer : this.renderers)
        {
            profiler.push("render_"+renderer.getName());

            if (renderer.hasData())
            {
//                MiniHUD.LOGGER.error("Container: render [{}] execute!", renderer.getName());
                renderer.render(camera, posMatrix, projMatrix, mc, profiler);
            }
            else
            {
                renderer.reset();
            }

            profiler.pop();
        }
    }

    protected void reset()
    {
        for (OverlayRendererBase renderer : this.renderers)
        {
            renderer.reset();
        }
    }

//    protected void draw(Vec3d cameraPos, Matrix4f matrix4f, Matrix4f projMatrix, MinecraftClient mc, Profiler profiler)
//    {
//        profiler.swap("render_draw");
//
//        if (this.resourcesAllocated && this.countActive > 0)
//        {
//            /*
//            RenderSystem.disableCull();
//            RenderSystem.enableDepthTest();
//            RenderSystem.depthMask(false);
//            RenderSystem.polygonOffset(-3f, -3f);
//            RenderSystem.enablePolygonOffset();
//             */
//
//            RenderUtils.blend(true);
//            RenderUtils.color(1f, 1f, 1f, 1f);
//
//            Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
//
//            for (IOverlayRenderer renderer : this.renderers)
//            {
//                profiler.push("draw_"+renderer.getName());
//
//                if (renderer.shouldRender(mc))
//                {
//                    Vec3d updatePos = renderer.getUpdatePosition();
//
//                    matrix4fstack.pushMatrix();
//                    matrix4fstack.translate((float) (updatePos.x - cameraPos.x), (float) (updatePos.y - cameraPos.y), (float) (updatePos.z - cameraPos.z));
//                    renderer.draw(matrix4fstack.get(matrix4f), projMatrix);
//                    matrix4fstack.popMatrix();
//                }
//
//                profiler.pop();
//            }
//
//            /*
//            RenderSystem.polygonOffset(0f, 0f);
//            RenderSystem.disablePolygonOffset();
//             */
//            RenderUtils.color(1f, 1f, 1f, 1f);
//            RenderUtils.blend(false);
//            /*
//            RenderSystem.disableBlend();
//            RenderSystem.enableDepthTest();
//            RenderSystem.enableCull();
//            RenderSystem.depthMask(true);
//             */
//        }
//    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        for (OverlayRendererBase renderer : this.renderers)
        {
            String id = renderer.getSaveId();

            if (!id.isEmpty())
            {
                obj.add(id, renderer.toJson());
            }
        }

        return obj;
    }

    public void fromJson(JsonObject obj)
    {
        for (OverlayRendererBase renderer : this.renderers)
        {
            String id = renderer.getSaveId();

            if (!id.isEmpty() && JsonUtils.hasObject(obj, id))
            {
                renderer.fromJson(obj.get(id).getAsJsonObject());
            }
        }
    }
}
