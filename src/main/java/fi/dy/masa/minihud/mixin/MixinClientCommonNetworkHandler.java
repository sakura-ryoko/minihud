package fi.dy.masa.minihud.mixin;

import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.listener.ClientCommonPacketListener;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.custom.DebugBrainCustomPayload;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.minihud.MiniHUD;

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
