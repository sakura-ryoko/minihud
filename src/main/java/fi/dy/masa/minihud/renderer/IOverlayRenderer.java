package fi.dy.masa.minihud.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public interface IOverlayRenderer
{
    /**
     * Return's a profiler friendly name for this renderer.
     */
    String getName();

    /**
     * Returns the camera position when the renderer was last updated
     */
    Vec3 getUpdatePosition();

    /**
     * Sets the camera position when the renderer was last updated
     */
    void setUpdatePosition(Vec3 cameraPosition);

    /**
     * Should this renderer draw anything at the moment, ie. is it enabled for example
     */
    boolean shouldRender(Minecraft mc);

    /**
     * Return true, if this renderer should get re-drawn/updated
     */
    boolean needsUpdate(Entity entity, Minecraft mc);

    /**
     * Re-draw the buffer contents, if needed
     * @param cameraPos The position of the camera when the method is called.
     * The camera position should be subtracted from any world coordinates for the vertex positions.
     * During the draw() call the MatrixStack will be translated by the camera position,
     * minus the difference between the camera position during the update() call,
     * and the camera position during the draw() call.
     * @param entity The current camera entity
     */
    void update(Vec3 cameraPos, Entity entity, Minecraft mc, ProfilerFiller profiler);

    /**
     * Returns true if the Renderer is ready to render data
     * @return (True|False)
     */
    boolean hasData();

    /**
     * Render contents to Buffers
     */
    void render(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler);

    /**
     * Draw Render buffers to Screen
     */
    void draw(Vec3 cameraPos);

    /**
     * Reset renderer's internal data
     */
    void reset();
}
