package fi.dy.masa.minihud.mixin.debug;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.util.DebugInfoUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;

@Mixin(DebugRenderer.class)
public abstract class MixinDebugRenderer
{
    @Inject(method = "render", at = @At("RETURN"))
    private void renderDebugRenderers(PoseStack matrices, Frustum frustum,
                                      MultiBufferSource.BufferSource vertexConsumers, double cameraX, double cameraY,
                                      double cameraZ, boolean bl, CallbackInfo ci)
    {
        if (Configs.Generic.MAIN_RENDERING_TOGGLE.getBooleanValue())
        {
            DebugInfoUtils.renderVanillaDebug(matrices, frustum, vertexConsumers, cameraX, cameraY, cameraZ);
        }
    }
}
