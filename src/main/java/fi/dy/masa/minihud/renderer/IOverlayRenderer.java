package fi.dy.masa.minihud.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;

public interface IOverlayRenderer
{
    /**
     * Return's a profiler friendly name for this renderer.
     */
    String getName();

    /**
     * Returns the camera position when the renderer was last updated
     */
    Vec3d getUpdatePosition();

    /**
     * Sets the camera position when the renderer was last updated
     */
    void setUpdatePosition(Vec3d cameraPosition);

    /**
     * Should this renderer draw anything at the moment, ie. is it enabled for example
     */
    boolean shouldRender(MinecraftClient mc);

    /**
     * Return true, if this renderer should get re-drawn/updated
     */
    boolean needsUpdate(Entity entity, MinecraftClient mc);

    /**
     * Re-draw the buffer contents, if needed
     * @param cameraPos The position of the camera when the method is called.
     * The camera position should be subtracted from any world coordinates for the vertex positions.
     * During the draw() call the MatrixStack will be translated by the camera position,
     * minus the difference between the camera position during the update() call,
     * and the camera position during the draw() call.
     * @param entity The current camera entity
     */
    void update(Vec3d cameraPos, Entity entity, MinecraftClient mc, Profiler profiler);

    /**
     * Returns true if the Renderer is ready to render data
     * @return (True|False)
     */
    boolean hasData();

    /**
     * Render contents to Buffers
     */
    void render(Vec3d cameraPos, MinecraftClient mc, Profiler profiler);

    /**
     * Draw Render buffers to Screen
     */
    void draw(Vec3d cameraPos);

    /**
     * Reset renderer's internal data
     */
    void reset();
}
