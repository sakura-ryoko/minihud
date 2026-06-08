package fi.dy.masa.minihud.mixin.render;

import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelExtractor.class)
public interface IMixinLevelExtractor
{
	@Invoker("countRenderedSections")
	int minihud_getRenderedChunksInvoker();
}
