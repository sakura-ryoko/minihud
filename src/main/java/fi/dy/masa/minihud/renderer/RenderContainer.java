package fi.dy.masa.minihud.renderer;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonObject;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;

import fi.dy.masa.malilib.util.data.json.JsonUtils;
import fi.dy.masa.malilib.util.position.PositionUtils;
import fi.dy.masa.malilib.util.position.Vec3d;

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
        this.addRenderer(OverlayRendererLightningRodRange.INSTANCE);
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

    public void extract(Entity entity, Minecraft mc, DeltaTracker deltaTracker, Camera camera, float ticks, ProfilerFiller profiler)
    {
        this.update(camera, entity, mc, profiler);
    }

    protected void update(Camera camera, Entity entity, Minecraft mc, ProfilerFiller profiler)
    {
        Vec3d position = Vec3d.of(camera.position());
        profiler.popPush("render_update");

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
                    renderer.update(position, entity, mc, profiler);
                    renderer.setUpdatePosition(position);
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

    public void render(Matrix4fc modelViewMatrix, Minecraft mc, Frustum frustum, CameraRenderState camera, ProfilerFiller profiler)
    {
        profiler.push("render_container");
        this.draw(camera, profiler);
        profiler.pop();
    }

    protected void draw(CameraRenderState camera, ProfilerFiller profiler)
    {
        profiler.popPush("render_draw");

        if (this.countActive > 0)
        {
            Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();

            for (IOverlayRenderer renderer : this.renderers)
            {
                profiler.push("draw_"+renderer.getName());

//                if (renderer.shouldRender(mc))
                if (renderer.hasData())
                {
                    Vec3d updatePos = renderer.getUpdatePosition();
                    Vec3d cameraPos = Vec3d.of(camera.pos);

                    matrix4fstack.pushMatrix();
                    matrix4fstack.translate((float) (updatePos.x - cameraPos.x), (float) (updatePos.y - cameraPos.y), (float) (updatePos.z - cameraPos.z));
                    renderer.draw(cameraPos);
                    matrix4fstack.popMatrix();
                }
                else
                {
                    renderer.reset();
                }

                profiler.pop();
            }
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
                JsonObject entry = renderer.toJson();

                if (entry != null && !entry.isEmpty())
                {
                    obj.add(id, entry);
                }
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
