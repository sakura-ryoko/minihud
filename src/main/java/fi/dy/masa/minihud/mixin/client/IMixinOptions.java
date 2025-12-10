package fi.dy.masa.minihud.mixin.client;

import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Options.class)
public interface IMixinOptions
{
	@Accessor("serverRenderDistance")
	int minihud_getServerRenderDistance();
}
