package fi.dy.masa.minihud.event;

import net.minecraft.client.Minecraft;

import fi.dy.masa.malilib.interfaces.IClientTickHandler;

public class ClientTickHandler implements IClientTickHandler
{
    @Override
    public void onClientTick(Minecraft mc)
    {
        if (mc.level != null && mc.player != null)
        {
            RenderHandler.getInstance().updateData(mc);
        }
    }
}
