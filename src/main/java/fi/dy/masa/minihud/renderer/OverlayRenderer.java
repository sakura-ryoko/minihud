package fi.dy.masa.minihud.renderer;

import org.joml.Matrix4fc;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;

import fi.dy.masa.malilib.util.EntityUtils;

public class OverlayRenderer
{
    private static long loginTime;
    private static boolean canRender;

    public static void resetRenderTimeout()
    {
        canRender = false;
        loginTime = System.currentTimeMillis();
    }

    public static void extractOverlays(Minecraft mc, DeltaTracker deltaTracker, Camera camera, float ticks, ProfilerFiller profiler)
    {
        Entity entity = EntityUtils.getCameraEntity();

        if (entity == null)
        {
            return;
        }

        if (canRender == false)
        {
            // Don't render before the player has been placed in the actual proper position,
            // otherwise some of the renderers mess up.
            // The magic 8.5, 65, 8.5 comes from the WorldClient constructor
            if (System.currentTimeMillis() - loginTime >= 5000 || entity.getX() != 8.5 || entity.getY() != 65 || entity.getZ() != 8.5)
            {
                canRender = true;
            }
            else
            {
                return;
            }
        }

        RenderContainer.INSTANCE.extract(entity, mc, deltaTracker, camera, ticks, profiler);
    }

    public static void renderOverlays(Matrix4fc modelViewMatrix, Minecraft mc, Frustum frustum, CameraRenderState camera, ProfilerFiller profiler)
    {
        if (canRender)
        {
            RenderContainer.INSTANCE.render(modelViewMatrix, mc, frustum, camera, profiler);
        }
    }

    public static void reset()
    {
        RenderContainer.INSTANCE.reset();
    }
}
