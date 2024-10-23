package fi.dy.masa.minihud.mixin;

import net.minecraft.client.network.ClientCommonNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = ClientCommonNetworkHandler.class, priority = 999)
public abstract class MixinClientCommonNetworkHandler
{
    /*
    @Shadow @Final protected net.minecraft.client.MinecraftClient client;

    @Inject(method = "onCustomPayload(Lnet/minecraft/network/packet/s2c/common/CustomPayloadS2CPacket;)V",
            at = @At("HEAD"), cancellable = true)
    private void minihud_onCustomPayloadFix(CustomPayloadS2CPacket packet, CallbackInfo ci)
    {
        CustomPayload payload = packet.payload();

        MiniHUD.logger.error("CustomPayload ID: [{}] // Class: [{}]", payload.getId().id(), payload.getClass().getCanonicalName());
        if (payload instanceof DebugBrainCustomPayload)
        {
            NetworkThreadUtils.forceMainThread(packet, (ClientCommonPacketListener) this, this.client);
            DebugBrainCustomPayload brainCustomPayload = (DebugBrainCustomPayload) payload;
            this.client.debugRenderer.villageDebugRenderer.addBrain(brainCustomPayload.brainDump());
            ci.cancel();
        }
    }
     */
}
