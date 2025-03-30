package fi.dy.masa.minihud.util;

import java.util.Iterator;
import java.util.List;

import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BeesComponent;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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

    public static int recalculateBundleSize(BundleContentsComponent bundle, int maxCount)
    {
        Iterator<ItemStack> iter = bundle.stream().iterator();
        final int vanillaMax = 64;
        final int vanillaBundleAdj = 4; // Why does a nested, bundle count as 4, mojang?
        int newCount = 0;

        while (iter.hasNext())
        {
            ItemStack entry = iter.next();

            if (!entry.isEmpty())
            {
                BeesComponent beeData = entry.getOrDefault(DataComponentTypes.BEES, BeesComponent.DEFAULT);
                List<BeehiveBlockEntity.BeeData> list = beeData.bees();

                if (!list.isEmpty())
                {
                    return vanillaMax;
                }
                else if (entry.contains(DataComponentTypes.BUNDLE_CONTENTS))
                {
                    // Nesting Bundles...
                    BundleContentsComponent bundleEntry = entry.get(DataComponentTypes.BUNDLE_CONTENTS);

                    if (bundleEntry != null)
                    {
                        if (bundleEntry.isEmpty())
                        {
                            newCount += vanillaBundleAdj;
                        }
                        else
                        {
                            newCount += recalculateBundleSize(bundleEntry, maxCount) + vanillaBundleAdj;
                        }
                    }
                    else
                    {
                        newCount += Math.min(entry.getCount(), maxCount);
                    }
                }
                else if (entry.getMaxCount() != vanillaMax)
                {
                    final float fraction = (float) entry.getCount() / entry.getMaxCount();

                    if (fraction != 1.0F)
                    {
                        // Needs to be based on vanillaMax.  It's cool that we
                        // can calculate this, but no user confusion is necessary.
                        newCount += (int) (vanillaMax * fraction);
                    }
                    else
                    {
                        return vanillaMax;
                    }
                }
                else
                {
                    newCount += Math.min(entry.getCount(), maxCount);
                }
            }
        }

        return newCount;
    }

    /*
    public static Inventory getNbtInventoryHorseFix(@Nonnull NbtCompound nbt, int slotCount, @Nonnull DynamicRegistryManager registry)
    {
        DefaultedList<ItemStack> horseEquipment = NbtEntityUtils.getHorseEquipmentFromNbt(nbt, registry);
        if (slotCount > 256)
        {
            slotCount = 256;
        }

        if (!nbt.contains(NbtKeys.ITEMS))
        {
            if (!horseEquipment.getLast().isEmpty())
            {
                SimpleInventory inv = new SimpleInventory(1);
                inv.setStack(0, horseEquipment.getLast().copy());

                return inv;
            }
            else
            {
                if (nbt.contains(NbtKeys.ITEM))
                {
                    ItemStack entry = fi.dy.masa.malilib.util.InventoryUtils.fromNbtOrEmpty(registry, nbt.get(NbtKeys.ITEM));
                    SimpleInventory inv = new SimpleInventory(1);
                    inv.setStack(0, entry.copy());

                    return inv;
                }

                return null;
            }
        }
        else
        {
            if (slotCount < 0)
            {
                NbtList list = nbt.getList(NbtKeys.ITEMS, 10);
                slotCount = list.size();
            }

            SimpleInventory inv = new SimpleInventory(slotCount + 1);
            DefaultedList<ItemStack> items = DefaultedList.ofSize(slotCount, ItemStack.EMPTY);
            Inventories.readNbt(nbt, items, registry);
            inv.setStack(0, horseEquipment.getLast().copy());

            if (!items.isEmpty())
            {
                for (int i = 0; i < slotCount; ++i)
                {
                    inv.setStack(i + 1, items.get(i));
                }
            }

            return inv;
        }
    }
     */
}
