package fi.dy.masa.minihud.mixin.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.SubtitleOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.minihud.event.RenderHandler;

@Mixin(SubtitleOverlay.class)
public abstract class MixinSubtitleOverlay
{
    @Inject(method = "extractRenderState",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V",
                     shift = At.Shift.BEFORE))
    private void minihud_nudgeSubtitleOverlay(GuiGraphicsExtractor graphics, CallbackInfo ci)
    {
        int offset = RenderHandler.getInstance().getSubtitleOffset();

        if (offset != 0)
        {
            graphics.pose().translate(0, offset);
        }
    }
}
