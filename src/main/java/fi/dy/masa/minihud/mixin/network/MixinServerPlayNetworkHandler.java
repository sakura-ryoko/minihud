package fi.dy.masa.minihud.mixin.network;

import net.minecraft.command.DefaultPermissions;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.util.DataStorage;

@Mixin(ServerPlayNetworkHandler.class)
public class MixinServerPlayNetworkHandler
{
    @Redirect(method = "onQueryBlockNbt",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/command/permission/PermissionPredicate;hasPermission(Lnet/minecraft/command/permission/Permission;)Z"))
    private boolean minihud_redirectQueryBlockNbt(PermissionPredicate instance, Permission permission)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP_OPEN_TO_LAN.getBooleanValue() &&
			DataStorage.getInstance().hasIntegratedServer())
        {
            return true;
        }

        return instance.hasPermission(DefaultPermissions.GAMEMASTERS);
    }

    @Redirect(method = "onQueryEntityNbt",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/command/permission/PermissionPredicate;hasPermission(Lnet/minecraft/command/permission/Permission;)Z"))
    private boolean minihud_redirectQueryEntityNbt(PermissionPredicate instance, Permission permission)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP_OPEN_TO_LAN.getBooleanValue() &&
			DataStorage.getInstance().hasIntegratedServer())
        {
            return true;
        }

        return instance.hasPermission(DefaultPermissions.GAMEMASTERS);
    }
}
