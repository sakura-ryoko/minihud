package fi.dy.masa.minihud.event;

import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import net.minecraft.client.MinecraftClient;

import fi.dy.masa.minihud.data.HudDataStorage;
import fi.dy.masa.minihud.util.DataStorage;

public class ClientTickHandler implements IClientTickHandler
{
    @Override
    public void onClientTick(MinecraftClient mc)
    {
        if (mc.world != null && mc.player != null)
        {
            RenderHandler.getInstance().updateData(mc);
            HudDataStorage.getInstance().onClientTickPost(mc);
        }
    }
}
