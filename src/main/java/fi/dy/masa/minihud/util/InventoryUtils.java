package fi.dy.masa.minihud.util;

import javax.annotation.Nonnull;

import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.NbtKeys;
import fi.dy.masa.minihud.event.RenderHandler;

public class InventoryUtils
{
    public static Inventory getInventory(World world, BlockPos pos)
    {
        Inventory inv = fi.dy.masa.malilib.util.InventoryUtils.getInventory(world, pos);

        if ((inv == null || inv.isEmpty()) && !DataStorage.getInstance().hasIntegratedServer())
        {
            RenderHandler.getInstance().requestBlockEntityAt(world, pos);
        }

        return inv;
    }

    public static Inventory getNbtInventoryHorseFix(@Nonnull NbtCompound nbt, int slotCount, @Nonnull RegistryWrapper.WrapperLookup registry)
    {
        ItemStack saddle = ItemStack.EMPTY;

        if (slotCount > 256)
        {
            slotCount = 256;
        }

        // Get Saddle Item for slot 0
        if (nbt.contains(NbtKeys.SADDLE))
        {
            saddle = ItemStack.fromNbtOrEmpty(registry, nbt.getCompound(NbtKeys.SADDLE));
        }
        // Shift inv ahead by 1 slot for horses (1.21 only)
        if (nbt.contains(NbtKeys.ITEMS))
        {
            // Standard 'Items' tag for most Block Entities --
            // -- Furnace, Brewing Stand, Shulker Box, Crafter, Barrel, Chest, Dispenser, Hopper, Bookshelf, Campfire
            if (slotCount < 0)
            {
                NbtList list = nbt.getList(NbtKeys.ITEMS, Constants.NBT.TAG_COMPOUND);
                slotCount = list.size();
            }

            SimpleInventory inv = new SimpleInventory(slotCount + 1);
            DefaultedList<ItemStack> items = DefaultedList.ofSize(slotCount, ItemStack.EMPTY);
            Inventories.readNbt(nbt, items, registry);

            if (items.isEmpty())
            {
                return null;
            }
            inv.setStack(0, saddle);
            for (int i = 0; i < slotCount; i++)
            {
                inv.setStack(i + 1, items.get(i));
            }

            return inv;
        }
        // Saddled only fix
        else if (!saddle.isEmpty())
        {
            SimpleInventory inv = new SimpleInventory(1);
            inv.setStack(0, saddle);
            return inv;
        }

        return null;
    }
}
