package fi.dy.masa.minihud.mixin.hud;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.minihud.event.RenderHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.SubtitleOverlay;

@Mixin(SubtitleOverlay.class)
public abstract class MixinSubtitleOverlay
{
    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V",
                     shift = At.Shift.BEFORE))
    private void minihud_nudgeSubtitleOverlay(GuiGraphics context, CallbackInfo ci)
    {
        int offset = RenderHandler.getInstance().getSubtitleOffset();

        if (offset != 0)
        {
            context.pose().translate(0, offset);
        }
    }
}
