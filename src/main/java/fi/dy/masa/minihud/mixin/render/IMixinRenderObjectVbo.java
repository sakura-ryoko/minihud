package fi.dy.masa.minihud.mixin.render;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import fi.dy.masa.minihud.renderer.RenderObjectVbo;

@Mixin(RenderObjectVbo.class)
public interface IMixinRenderObjectVbo
{
    @Accessor("alloc")
    ByteBufferBuilder minihud_getAlloc();
}
