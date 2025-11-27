package fi.dy.masa.minihud.mixin.debug;

import net.minecraft.client.network.ClientDebugSubscriptionManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayNetworkHandler.class)
public interface IMixinClientPlayNetworkHandler
{
	@Accessor("debugSubscriptionManager")
	ClientDebugSubscriptionManager minihud_getDebugManager();
}
