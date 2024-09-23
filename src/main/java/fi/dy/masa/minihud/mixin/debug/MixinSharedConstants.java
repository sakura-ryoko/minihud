package fi.dy.masa.minihud.mixin.debug;

import net.minecraft.SharedConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.minihud.config.Configs;

@Mixin(SharedConstants.class)
public abstract class MixinSharedConstants
{
    @Shadow @Mutable public static boolean isDevelopment;

    public MixinSharedConstants() {}

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void minihud_enableDevelopmentMode(CallbackInfo ci)
    {
        if (Configs.Generic.DEBUG_DEVELOPMENT_MODE.getBooleanValue())
        {
            isDevelopment = true;
        }
    }
}
