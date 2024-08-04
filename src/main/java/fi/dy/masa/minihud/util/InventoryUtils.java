package fi.dy.masa.minihud.util;

import net.minecraft.inventory.Inventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fi.dy.masa.minihud.event.RenderHandler;

public class InventoryUtils
{
    public static Inventory getInventory(World world, BlockPos pos)
    {
        Inventory inv = fi.dy.masa.malilib.util.InventoryUtils.getInventory(world, pos);

        if ((inv == null || inv.isEmpty()) && DataStorage.getInstance().hasIntegratedServer() == false)
        {
            RenderHandler.getInstance().requestBlockEntityAt(world, pos);
        }

        return inv;
    }
}
