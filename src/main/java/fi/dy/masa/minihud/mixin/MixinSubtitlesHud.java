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
    @Inject(method = "render",
            at = @At(value = "INVOKE",
                     target = "Lorg/joml/Matrix3x2fStack;scale(FF)Lorg/joml/Matrix3x2f;"), remap = false)
    private void nudgeSubtitleOverlay(DrawContext context, CallbackInfo ci)
    {
        int offset = RenderHandler.getInstance().getSubtitleOffset();

        if (offset != 0)
        {
            context.getMatrices().translate(0, offset);
        }
    }
}
