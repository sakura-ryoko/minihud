package fi.dy.masa.minihud.mixin;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ResourcePackSendS2CPacket.class)
public class MixinResourcePackSendS2CPacket
{
    /*
    @Mutable
    @Shadow @Final private Optional<Text> prompt;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void minihud_fixNull(UUID uUID, String string, String string2, boolean bl, Optional<Text> optional, CallbackInfo ci)
    {
        this.prompt = optional == null ? Optional.empty() : optional;
    }
     */
}
