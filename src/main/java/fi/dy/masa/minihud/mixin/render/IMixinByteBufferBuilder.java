package fi.dy.masa.minihud.mixin.render;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ByteBufferBuilder.class)
public interface IMixinByteBufferBuilder
{
    @Invoker("ensureCapacity")
    void minihud_ensureCapacity(final long requiredCapacity);
}
