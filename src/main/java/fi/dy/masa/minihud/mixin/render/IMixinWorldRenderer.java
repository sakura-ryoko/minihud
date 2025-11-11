package fi.dy.masa.minihud.mixin.render;

import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(WorldRenderer.class)
public interface IMixinWorldRenderer
{
    @Invoker("getCompletedChunkCount")
    int minihud_getRenderedChunksInvoker();
}
