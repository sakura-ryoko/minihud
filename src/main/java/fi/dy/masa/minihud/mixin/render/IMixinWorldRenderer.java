package fi.dy.masa.minihud.mixin.render;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelRenderer.class)
public interface IMixinWorldRenderer
{
    @Invoker("countRenderedSections")
    int minihud_getRenderedChunksInvoker();
}
