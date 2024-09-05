package fi.dy.masa.minihud.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = MinecraftServer.class)
public interface IMixinMinecraftServer
{
    @Invoker("executeTask")
    void minihud_send(ServerTask task);
}
