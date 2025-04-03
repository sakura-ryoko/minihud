package fi.dy.masa.minihud.renderer;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonObject;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.Frustum;
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

    public void render(Entity entity, Matrix4f posMatrix, Matrix4f projMatrix, MinecraftClient mc, Camera camera, Frustum frustum, Fog fog, Profiler profiler)
    {
        profiler.push("render_container");
        this.update(camera.getPos(), entity, mc, profiler);
//        this.draw(camera.getPos(), posMatrix, projMatrix, mc, frustum, fog, profiler);
        this.draw(camera.getPos(), profiler);
        profiler.pop();
    }

    protected void update(Vec3d cameraPos, Entity entity, MinecraftClient mc, Profiler profiler)
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
                    renderer.update(cameraPos, entity, mc, profiler);
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

    protected void draw(Vec3d cameraPos, Profiler profiler)
    {
        profiler.swap("render_draw");

        if (this.countActive > 0)
        {
//            RenderUtils.culling(false);
//            RenderUtils.depthTest(true);
//            RenderUtils.depthMask(false);
//            RenderUtils.polygonOffset(-3f, -3f);
//            RenderUtils.polygonOffset(true);
//            RenderUtils.blend(true);
//            RenderUtils.color(1f, 1f, 1f, 1f);

            Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();

            for (IOverlayRenderer renderer : this.renderers)
            {
                profiler.push("draw_"+renderer.getName());

//                if (renderer.shouldRender(mc))
                if (renderer.hasData())
                {
                    Vec3d updatePos = renderer.getUpdatePosition();

                    matrix4fstack.pushMatrix();
                    matrix4fstack.translate((float) (updatePos.x - cameraPos.x), (float) (updatePos.y - cameraPos.y), (float) (updatePos.z - cameraPos.z));
//                    renderer.resortQuads(cameraPos);
                    renderer.draw(cameraPos);
                    matrix4fstack.popMatrix();
                }
                else
                {
                    renderer.reset();
                }

                profiler.pop();
            }

//            RenderUtils.polygonOffset(0f, 0f);
//            RenderUtils.polygonOffset(false);
//            RenderUtils.color(1f, 1f, 1f, 1f);
//            RenderUtils.blend(false);
//            RenderUtils.depthTest(true);
//            RenderUtils.culling(true);
//            RenderUtils.depthMask(true);
        }
    }

    protected void reset()
    {
        for (OverlayRendererBase renderer : this.renderers)
        {
            renderer.reset();
        }
    }

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
