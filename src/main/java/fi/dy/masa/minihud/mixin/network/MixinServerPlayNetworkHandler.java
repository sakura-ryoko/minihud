package fi.dy.masa.minihud.mixin.network;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.util.DataStorage;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;

@Mixin(ServerGamePacketListenerImpl.class)
public class MixinServerPlayNetworkHandler
{
    @Redirect(method = "handleBlockEntityTagQuery",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/server/permissions/PermissionSet;hasPermission(Lnet/minecraft/server/permissions/Permission;)Z"))
    private boolean minihud_redirectQueryBlockNbt(PermissionSet instance, Permission permission)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP_OPEN_TO_LAN.getBooleanValue() &&
			DataStorage.getInstance().hasIntegratedServer())
        {
            return true;
        }

        return instance.hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    @Redirect(method = "handleEntityTagQuery",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/server/permissions/PermissionSet;hasPermission(Lnet/minecraft/server/permissions/Permission;)Z"))
    private boolean minihud_redirectQueryEntityNbt(PermissionSet instance, Permission permission)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP_OPEN_TO_LAN.getBooleanValue() &&
			DataStorage.getInstance().hasIntegratedServer())
        {
            return true;
        }

        return instance.hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }
}
