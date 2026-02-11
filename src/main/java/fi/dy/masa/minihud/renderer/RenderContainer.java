package fi.dy.masa.minihud.renderer;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import com.google.gson.JsonObject;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import com.mojang.blaze3d.systems.RenderSystem;
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

    public void render(Entity entity, Matrix4f posMatrix, Matrix4f projMatrix, Minecraft mc, Camera camera, Frustum frustum, ProfilerFiller profiler)
    {
        profiler.push("render_container");
        this.update(camera.position(), entity, mc, profiler);
        this.draw(camera.position(), profiler);
        profiler.pop();
    }

    protected void update(Vec3 cameraPos, Entity entity, Minecraft mc, ProfilerFiller profiler)
    {
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

    protected void draw(Vec3 cameraPos, ProfilerFiller profiler)
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
                    Vec3 updatePos = renderer.getUpdatePosition();

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
