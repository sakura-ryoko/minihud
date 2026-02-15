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

	// This tends to be problematic.
//	@Inject(method = "runTick",
//			at = @At(value = "INVOKE",
//					 target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;",
//					 shift = At.Shift.BEFORE))
//	private void minihud_updateGpuPercentForInfoLine1(boolean tick, CallbackInfo ci)
//	{
//		// Enable GPU Profiling
//		InfoLineProfiler.INSTANCE.GPUStage1();
//	}
//
//	@Inject(method = "runTick",
//			at = @At(value = "INVOKE",
//					 target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
//					 shift = At.Shift.BEFORE,
//					 ordinal = 7))
//	private void minihud_updateGpuPercentForInfoLine2(boolean tick, CallbackInfo ci)
//	{
//		InfoLineProfiler.INSTANCE.GPUStage2();
//	}
//
//	@Inject(method = "runTick",
//			at = @At(value = "INVOKE",
//					 target = "Lnet/minecraft/util/Util;getNanos()J",
//					 ordinal = 2))
//	private void minihud_updateGpuPercentForInfoLine3(boolean tick, CallbackInfo ci)
//	{
//		InfoLineProfiler.INSTANCE.GPUStage3();
//	}
//
//	@Inject(method = "runTick",
//			at = @At(value = "INVOKE",
//					 target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V",
//					 ordinal = 3))
//	private void minihud_updateGpuPercentForInfoLine4(boolean tick, CallbackInfo ci)
//	{
//		InfoLineProfiler.INSTANCE.GPUStage4();
//	}
}
