package fi.dy.masa.minihud.mixin.client;

import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameOptions.class)
public interface IMixinOptions
{
	@Accessor("serverViewDistance")
	int minihud_getServerRenderDistance();
}
