package fi.dy.masa.minihud.mixin.network;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.util.DataStorage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

@Mixin(ServerGamePacketListenerImpl.class)
public class MixinServerPlayNetworkHandler
{
    @Redirect(method = "handleBlockEntityTagQuery",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/server/level/ServerPlayer;hasPermissions(I)Z"))
    private boolean minihud_redirectQueryBlockNbt(ServerPlayer instance, int i)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP_OPEN_TO_LAN.getBooleanValue() &&
			DataStorage.getInstance().hasIntegratedServer())
        {
            return true;
        }

        return instance.hasPermissions(2);
    }

    @Redirect(method = "handleEntityTagQuery",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/server/level/ServerPlayer;hasPermissions(I)Z"))
    private boolean minihud_redirectQueryEntityNbt(ServerPlayer instance, int i)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP_OPEN_TO_LAN.getBooleanValue() &&
			DataStorage.getInstance().hasIntegratedServer())
        {
            return true;
        }

        return instance.hasPermissions(2);
    }
}
