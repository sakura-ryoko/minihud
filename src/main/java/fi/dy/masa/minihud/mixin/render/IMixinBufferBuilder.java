package fi.dy.masa.minihud.mixin.render;

import net.minecraft.client.render.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BufferBuilder.class)
public interface IMixinBufferBuilder
{
    @Accessor("building")
    boolean minihud_isBuilding();

    @Accessor("vertexCount")
    int minihud_getVertexCount();

    @Accessor("vertexPointer")
    long minihud_getVertexPointer();
}
