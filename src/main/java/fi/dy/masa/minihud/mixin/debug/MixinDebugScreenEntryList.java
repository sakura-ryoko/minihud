package fi.dy.masa.minihud.mixin.debug;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.minihud.util.DebugInfoUtils;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;

@Mixin(DebugScreenEntryList.class)
public abstract class MixinDebugScreenEntryList
{
	@Inject(method = "rebuildCurrentList", at = @At("TAIL"))
	private void minihud_onUpdateVisibleEntries(CallbackInfo ci)
	{
		DebugInfoUtils.onUpdateVisibleEntries((DebugScreenEntryList) (Object) this);
	}
}
