package fi.dy.masa.minihud.renderer;

import net.minecraft.client.render.BufferBuilder;
import org.joml.Matrix4f;

@Deprecated(forRemoval = true)
public abstract class RenderObjectBase
{
    public RenderObjectBase() {}

    public abstract void uploadData(BufferBuilder buffer);

    public abstract void draw(Matrix4f matrix4f, Matrix4f projMatrix);

    public abstract void deleteGlResources();
}
