package fi.dy.masa.minihud.mixin.render;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelRenderer.class)
public interface IMixinWorldRenderer
{
    @Invoker("countRenderedSections")
    int minihud_getRenderedChunksInvoker();

    @Accessor("levelRenderState")
    LevelRenderState minihud_getRenderStates();
}
