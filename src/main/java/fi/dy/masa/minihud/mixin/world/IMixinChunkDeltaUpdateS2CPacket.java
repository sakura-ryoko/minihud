package fi.dy.masa.minihud.mixin.world;

import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundSectionBlocksUpdatePacket.class)
public interface IMixinChunkDeltaUpdateS2CPacket
{
    @Accessor("sectionPos")
    SectionPos minihud_getChunkSectionPos();
}
