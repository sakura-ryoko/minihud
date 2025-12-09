package fi.dy.masa.minihud.mixin.debug;

import net.minecraft.client.multiplayer.ClientDebugSubscriber;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPacketListener.class)
public interface IMixinClientPlayNetworkHandler
{
	@Accessor("debugSubscriber")
	ClientDebugSubscriber minihud_getDebugManager();
}
