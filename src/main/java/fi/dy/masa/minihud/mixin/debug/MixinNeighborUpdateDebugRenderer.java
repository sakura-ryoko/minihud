package fi.dy.masa.minihud.mixin.debug;

import net.minecraft.client.render.debug.NeighborUpdateDebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NeighborUpdateDebugRenderer.class)
public abstract class MixinNeighborUpdateDebugRenderer
{
    @Inject(method = "render", at = @At("HEAD"))
    public void fixDebugRendererState(CallbackInfo ci)
    {
        //RenderHandler.fixDebugRendererState();
    }
}
