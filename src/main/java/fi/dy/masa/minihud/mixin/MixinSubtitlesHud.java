package fi.dy.masa.minihud.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.SubtitlesHud;

import fi.dy.masa.minihud.event.RenderHandler;

@Mixin(SubtitlesHud.class)
public abstract class MixinSubtitlesHud
{
    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;)V",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V",
                     shift = At.Shift.BEFORE))
    private void minihud_nudgeSubtitleOverlay(DrawContext context, CallbackInfo ci)
    {
        int offset = RenderHandler.getInstance().getSubtitleOffset();

        if (offset != 0)
        {
            context.getMatrices().translate(0, offset);
        }
    }
}
