package fi.dy.masa.minihud.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import fi.dy.masa.malilib.interfaces.IServerListener;
import fi.dy.masa.minihud.data.HudDataManager;
import fi.dy.masa.minihud.util.DataStorage;

public class ServerListener implements IServerListener
{
    @Override
    public void onServerStarted(MinecraftServer server)
    {
        HudDataManager.getInstance().checkWorldSeed(server);
    }

    @Override
    public void onServerIntegratedSetup(IntegratedServer server)
    {
        DataStorage.getInstance().setHasIntegratedServer(true, server);
    }
}
