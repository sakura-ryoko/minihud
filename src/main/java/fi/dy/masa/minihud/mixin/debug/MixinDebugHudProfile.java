package fi.dy.masa.minihud.mixin.debug;

import net.minecraft.client.gui.hud.debug.DebugHudProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.minihud.util.DebugInfoUtils;

@Mixin(DebugHudProfile.class)
public abstract class MixinDebugHudProfile
{
	@Inject(method = "updateVisibleEntries", at = @At("TAIL"))
	private void minihud_onUpdateVisibleEntries(CallbackInfo ci)
	{
		DebugInfoUtils.onUpdateVisibleEntries((DebugHudProfile) (Object) this);
	}
}
