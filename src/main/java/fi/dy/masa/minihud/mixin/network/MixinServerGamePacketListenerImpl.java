package fi.dy.masa.minihud.mixin.network;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.util.DataStorage;

@Mixin(ServerGamePacketListenerImpl.class)
public class MixinServerGamePacketListenerImpl
{
    @WrapOperation(method = "handleBlockEntityTagQuery",
                   at = @At(value = "INVOKE",
                            target = "Lnet/minecraft/server/permissions/PermissionSet;hasPermission(Lnet/minecraft/server/permissions/Permission;)Z"
                   )
    )
    private boolean minihud_redirectQueryBlockNbt(PermissionSet instance, Permission permission, Operation<Boolean> original)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP_OPEN_TO_LAN.getBooleanValue() &&
			DataStorage.getInstance().hasIntegratedServer())
        {
            return true;
        }

        return original.call(instance, permission);
    }

    @WrapOperation(method = "handleEntityTagQuery",
                   at = @At(value = "INVOKE",
                            target = "Lnet/minecraft/server/permissions/PermissionSet;hasPermission(Lnet/minecraft/server/permissions/Permission;)Z"
                   )
    )
    private boolean minihud_redirectQueryEntityNbt(PermissionSet instance, Permission permission, Operation<Boolean> original)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP_OPEN_TO_LAN.getBooleanValue() &&
			DataStorage.getInstance().hasIntegratedServer())
        {
            return true;
        }

        return original.call(instance, permission);
    }
}
