package fi.dy.masa.minihud.util;

import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Bees;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import fi.dy.masa.minihud.event.RenderHandler;

public class InventoryUtils
{
    public static int recalculateBundleSize(BundleContents bundle, int maxCount)
    {
        Iterator<ItemStack> iter = bundle.itemCopyStream().iterator();
        final int vanillaMax = 64;
        final int vanillaBundleAdj = 4; // Why does a nested, bundle count as 4, mojang?
        int newCount = 0;

        while (iter.hasNext())
        {
            ItemStack entry = iter.next();

            if (!entry.isEmpty())
            {
                Bees beeData = entry.getOrDefault(DataComponents.BEES, Bees.EMPTY);
                List<BeehiveBlockEntity.Occupant> list = beeData.bees();

                if (!list.isEmpty())
                {
                    return vanillaMax;
                }
                else if (entry.has(DataComponents.BUNDLE_CONTENTS))
                {
                    // Nesting Bundles...
                    BundleContents bundleEntry = entry.get(DataComponents.BUNDLE_CONTENTS);

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
                else if (entry.getMaxStackSize() != vanillaMax)
                {
                    final float fraction = (float) entry.getCount() / entry.getMaxStackSize();

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
