package fi.dy.masa.minihud.util;

import java.util.Iterator;
import java.util.List;

import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.component.DataComponentTypes;
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
                List<BeehiveBlockEntity.BeeData> list = entry.getOrDefault(DataComponentTypes.BEES, List.of());

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
}
