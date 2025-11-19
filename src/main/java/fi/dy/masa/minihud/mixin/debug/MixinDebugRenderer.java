package fi.dy.masa.minihud.mixin.debug;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.util.DebugInfoUtils;

@Mixin(DebugRenderer.class)
public abstract class MixinDebugRenderer
{
    @Inject(method = "render", at = @At("RETURN"))
    private void renderDebugRenderers(Frustum frustum, double cameraX, double cameraY, double cameraZ, float tickProgress, CallbackInfo ci)
    {
        if (Configs.Generic.MAIN_RENDERING_TOGGLE.getBooleanValue())
        {
            DebugInfoUtils.renderVanillaDebug(frustum, cameraX, cameraY, cameraZ, tickProgress);
        }
    }
}
