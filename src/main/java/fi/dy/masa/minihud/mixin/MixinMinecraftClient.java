package fi.dy.masa.minihud.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.minihud.info.InfoLineProfiler;
import fi.dy.masa.minihud.util.DataStorage;

@Mixin(net.minecraft.client.MinecraftClient.class)
public abstract class MixinMinecraftClient
{
	@Inject(method = "tick", at = @At("HEAD"))
    private void onClientTickPre(CallbackInfo ci)
    {
        DataStorage.getInstance().onClientTickPre((net.minecraft.client.MinecraftClient) (Object) this);
    }

	@Inject(method = "render",
			at = @At(value = "INVOKE",
					 target = "Lnet/minecraft/client/MinecraftClient;getFramebuffer()Lnet/minecraft/client/gl/Framebuffer;",
					 shift = At.Shift.BEFORE))
	private void minihud_updateGpuPercentForInfoLine1(boolean tick, CallbackInfo ci)
	{
		// Enable GPU Profiling
		InfoLineProfiler.INSTANCE.GPUStage1();
	}

	@Inject(method = "render",
			at = @At(value = "INVOKE",
					 target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V",
					 shift = At.Shift.BEFORE,
					 ordinal = 4))
	private void minihud_updateGpuPercentForInfoLine2(boolean tick, CallbackInfo ci)
	{
		InfoLineProfiler.INSTANCE.GPUStage2();
	}

	@Inject(method = "render",
			at = @At(value = "INVOKE",
					 target = "Lnet/minecraft/util/Util;getMeasuringTimeNano()J",
					 ordinal = 2))
	private void minihud_updateGpuPercentForInfoLine3(boolean tick, CallbackInfo ci)
	{
		InfoLineProfiler.INSTANCE.GPUStage3();
	}

	@Inject(method = "render",
			at = @At(value = "INVOKE",
					 target = "Lnet/minecraft/util/profiler/Profiler;push(Ljava/lang/String;)V",
					 ordinal = 5))
	private void minihud_updateGpuPercentForInfoLine4(boolean tick, CallbackInfo ci)
	{
		InfoLineProfiler.INSTANCE.GPUStage4();
	}
}
