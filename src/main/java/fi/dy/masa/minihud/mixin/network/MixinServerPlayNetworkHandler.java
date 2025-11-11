package fi.dy.masa.minihud.mixin.network;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.util.DataStorage;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin(ServerPlayNetworkHandler.class)
public class MixinServerPlayNetworkHandler
{
    @Redirect(method = "onQueryBlockNbt",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/server/network/ServerPlayerEntity;hasPermissionLevel(I)Z"))
    private boolean minihud_redirectQueryBlockNbt(ServerPlayerEntity instance, int i)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP_OPEN_TO_LAN.getBooleanValue() &&
			DataStorage.getInstance().hasIntegratedServer())
        {
            return true;
        }

        return instance.hasPermissionLevel(2);
    }

    @Redirect(method = "onQueryEntityNbt",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/server/network/ServerPlayerEntity;hasPermissionLevel(I)Z"))
    private boolean minihud_redirectQueryEntityNbt(ServerPlayerEntity instance, int i)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP_OPEN_TO_LAN.getBooleanValue() &&
			DataStorage.getInstance().hasIntegratedServer())
        {
            return true;
        }

        return instance.hasPermissionLevel(2);
    }
}
