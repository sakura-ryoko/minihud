package fi.dy.masa.minihud.event;

import net.minecraft.client.MinecraftClient;

import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import fi.dy.masa.minihud.data.HudDataManager;

public class ClientTickHandler implements IClientTickHandler
{
    @Override
    public void onClientTick(MinecraftClient mc)
    {
        if (mc.world != null && mc.player != null)
        {
            RenderHandler.getInstance().updateData(mc);
            HudDataManager.getInstance().onClientTickPost(mc);
        }
    }
}
