package fi.dy.masa.minihud.mixin.network;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.NbtQueryResponseS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.data.EntitiesDataManager;
import fi.dy.masa.minihud.data.HudDataManager;
import fi.dy.masa.minihud.mixin.world.IMixinChunkDeltaUpdateS2CPacket;
import fi.dy.masa.minihud.util.DataStorage;
import fi.dy.masa.minihud.util.NotificationUtils;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler
{
    @Inject(method = "onBlockUpdate", at = @At("RETURN"))
    private void minihud_markChunkChangedBlockChange(net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket packet, CallbackInfo ci)
    {
        NotificationUtils.onBlockChange(packet.getPos(), packet.getState());
    }

    @Inject(method = "onChunkData", at = @At("RETURN"))
    private void minihud_markChunkChangedFullChunk(net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket packet, CallbackInfo ci)
    {
        NotificationUtils.onChunkData(packet.getChunkX(), packet.getChunkZ(), packet.getChunkData());
    }

    @Inject(method = "onChunkDeltaUpdate", at = @At("RETURN"))
    private void minihud_markChunkChangedMultiBlockChange(net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket packet, CallbackInfo ci)
    {
        net.minecraft.util.math.ChunkSectionPos pos = ((IMixinChunkDeltaUpdateS2CPacket) packet).minihud_getChunkSectionPos();
        NotificationUtils.onMultiBlockChange(pos, packet);
    }

    @Inject(method = "onGameMessage", at = @At("RETURN"))
    private void minihud_onGameMessage(net.minecraft.network.packet.s2c.play.GameMessageS2CPacket packet, CallbackInfo ci)
    {
        DataStorage.getInstance().onChatMessage(packet.content());
    }

    @Inject(method = "onPlayerListHeader", at = @At("RETURN"))
    private void minihud_onHandlePlayerListHeaderFooter(net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket packetIn, CallbackInfo ci)
    {
        DataStorage.getInstance().handleCarpetServerTPSData(packetIn.footer());
        DataStorage.getInstance().getMobCapData().parsePlayerListFooterMobCapData(packetIn.footer());
    }

    @Inject(method = "onWorldTimeUpdate", at = @At("RETURN"))
    private void minihud_onTimeUpdate(net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket packetIn, CallbackInfo ci)
    {
//        DataStorage.getInstance().onServerTimeUpdate(packetIn.time());
    }

    @Inject(method = "onPlayerSpawnPosition", at = @At("RETURN"))
    private void minihud_onSetSpawn(net.minecraft.network.packet.s2c.play.PlayerSpawnPositionS2CPacket packet, CallbackInfo ci)
    {
        HudDataManager.getInstance().setWorldSpawn(packet.getPos());
    }

    @Inject(method = "onGameJoin", at = @At("RETURN"))
    private void minihud_onPostGameJoin(GameJoinS2CPacket packet, CallbackInfo ci)
    {
        DataStorage.getInstance().setSimulationDistance(packet.simulationDistance());
    }

    @Inject(method = "onNbtQueryResponse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/DataQueryHandler;handleQueryResponse(ILnet/minecraft/nbt/NbtCompound;)Z"))
    private void minihud_onQueryResponse(NbtQueryResponseS2CPacket packet, CallbackInfo ci)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue())
        {
            EntitiesDataManager.getInstance().handleVanillaQueryNbt(packet.getTransactionId(), packet.getNbt());
        }
    }

    @Inject(method = "onCommandTree", at = @At("RETURN"))
    private void minihud_onCommandTree(CallbackInfo ci)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue())
        {
            // when the player becomes OP, the server sends the command tree to the client
            EntitiesDataManager.getInstance().resetOpCheck();
        }
    }
}
