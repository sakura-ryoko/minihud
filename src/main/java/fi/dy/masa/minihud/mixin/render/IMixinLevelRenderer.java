package fi.dy.masa.minihud.mixin.render;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface IMixinLevelRenderer
{
    @Accessor("levelRenderState")
    LevelRenderState minihud_getRenderStates();
}
