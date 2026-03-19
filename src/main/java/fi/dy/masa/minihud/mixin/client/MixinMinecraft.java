package fi.dy.masa.minihud.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.minihud.renderer.worker.WorkerDaemonHandler;
import fi.dy.masa.minihud.util.DataStorage;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft
{
	@Inject(method = "tick", at = @At("HEAD"))
    private void onClientTickPre(CallbackInfo ci)
    {
        DataStorage.getInstance().onClientTickPre((Minecraft) (Object) this);
    }

	@Inject(method = "stop", at = @At("HEAD"))
	private void minihud_onStop(CallbackInfo ci)
	{
		WorkerDaemonHandler.INSTANCE.endAll();
	}
}
