package fi.dy.masa.minihud.mixin.debug;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;

import fi.dy.masa.minihud.renderer.LavaDebugRenderer;

@Mixin(DebugRenderer.class)
public abstract class MixinDebugRenderer
{
    @Inject(method = "emitGizmos", at = @At("TAIL"))
    private void minihud_emitLavaGizmos(Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick, CallbackInfo ci)
    {
        LavaDebugRenderer.emitGizmos();
    }
}
