package fi.dy.masa.minihud.mixin.network;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.*;
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

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener
{
    @Inject(method = "handleBlockUpdate", at = @At("RETURN"))
    private void minihud_markChunkChangedBlockChange(ClientboundBlockUpdatePacket packet, CallbackInfo ci)
    {
        NotificationUtils.onBlockChange(packet.getPos(), packet.getBlockState());
    }

    @Inject(method = "handleLevelChunkWithLight", at = @At("RETURN"))
    private void minihud_markChunkChangedFullChunk(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci)
    {
        NotificationUtils.onChunkData(packet.getX(), packet.getZ(), packet.getChunkData());
    }

    @Inject(method = "handleChunkBlocksUpdate", at = @At("RETURN"))
    private void minihud_markChunkChangedMultiBlockChange(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci)
    {
        net.minecraft.core.SectionPos pos = ((IMixinChunkDeltaUpdateS2CPacket) packet).minihud_getChunkSectionPos();
        NotificationUtils.onMultiBlockChange(pos, packet);
    }

    @Inject(method = "handleSystemChat", at = @At("RETURN"))
    private void minihud_onGameMessage(ClientboundSystemChatPacket packet, CallbackInfo ci)
    {
        DataStorage.getInstance().onChatMessage(packet.content());
    }

    @Inject(method = "handleTabListCustomisation", at = @At("RETURN"))
    private void minihud_onHandlePlayerListHeaderFooter(ClientboundTabListPacket packetIn, CallbackInfo ci)
    {
        DataStorage.getInstance().handleCarpetServerTPSData(packetIn.footer());
        DataStorage.getInstance().getMobCapData().parsePlayerListFooterMobCapData(packetIn.footer());
    }

    @Inject(method = "handleSetTime", at = @At("RETURN"))
    private void minihud_onTimeUpdate(ClientboundSetTimePacket clientboundSetTimePacket, CallbackInfo ci)
    {
//        DataStorage.getInstance().onServerTimeUpdate(packetIn.time());
    }

    @Inject(method = "handleSetSpawn", at = @At("RETURN"))
    private void minihud_onSetSpawn(ClientboundSetDefaultSpawnPositionPacket packet, CallbackInfo ci)
    {
//        MiniHUD.LOGGER.error("onPlayerSpawnPosition() [PACKET] --> [{}]", packet.respawnData().globalPos().toString());
        HudDataManager.getInstance().setWorldSpawn(packet.respawnData().globalPos());
    }

    @Inject(method = "handleLogin", at = @At("RETURN"))
    private void minihud_onPostGameJoin(ClientboundLoginPacket packet, CallbackInfo ci)
    {
        DataStorage.getInstance().setSimulationDistance(packet.simulationDistance());
    }

    @Inject(method = "handleTagQueryPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/DebugQueryHandler;handleResponse(ILnet/minecraft/nbt/CompoundTag;)Z"))
    private void minihud_onQueryResponse(ClientboundTagQueryPacket packet, CallbackInfo ci)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue())
        {
            EntitiesDataManager.getInstance().handleVanillaQueryNbt(packet.getTransactionId(), packet.getTag());
        }
    }

    @Inject(method = "handleCommands", at = @At("RETURN"))
    private void minihud_onCommandTree(CallbackInfo ci)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue())
        {
            // when the player becomes OP, the server sends the command tree to the client
            EntitiesDataManager.getInstance().resetOpCheck();
        }
    }
}
