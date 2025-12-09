package fi.dy.masa.minihud.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.block.state.BlockState;

public class NotificationUtils
{
    public static void onBlockChange(BlockPos pos, BlockState stateNew)
    {
        DataStorage.getInstance().markChunkForHeightmapCheck(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public static void onMultiBlockChange(SectionPos chunkPos, ClientboundSectionBlocksUpdatePacket packet)
    {
        DataStorage.getInstance().markChunkForHeightmapCheck(chunkPos.x(), chunkPos.z());
    }

    public static void onChunkData(int chunkX, int chunkZ, ClientboundLevelChunkPacketData data)
    {
        DataStorage.getInstance().markChunkForHeightmapCheck(chunkX, chunkZ);
    }
}
