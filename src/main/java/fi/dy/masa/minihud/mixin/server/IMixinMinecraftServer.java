package fi.dy.masa.minihud.mixin.server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = MinecraftServer.class)
public interface IMixinMinecraftServer
{
    @Invoker("doRunTask")
    void minihud_send(TickTask task);
}
