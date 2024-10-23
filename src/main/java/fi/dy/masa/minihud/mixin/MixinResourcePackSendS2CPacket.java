package fi.dy.masa.minihud.mixin;

import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import org.spongepowered.asm.mixin.Mixin;

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
