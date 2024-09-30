package fi.dy.masa.minihud.util;

import java.util.List;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import fi.dy.masa.malilib.util.BlockUtils;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.minihud.mixin.IMixinAbstractFurnaceBlockEntity;

public class MiscUtils
{
    private static final Random RAND = new Random();
    private static final int[] AXOLOTL_COLORS = new int[] { 0xFFC7EC, 0x8C6C50, 0xFAD41B, 0xE8F7Fb, 0xB6B5FE };

    public static long bytesToMb(long bytes)
    {
        return bytes / 1024L / 1024L;
    }

    public static double intAverage(int[] values)
    {
        long sum = 0L;

        for (int value : values)
        {
            sum += value;
        }

        return (double) sum / (double) values.length;
    }

    public static long longAverage(long[] values)
    {
        long sum = 0L;

        for (long value : values)
        {
            sum += value;
        }

        return sum / values.length;
    }

    public static boolean canSlimeSpawnAt(int posX, int posZ, long worldSeed)
    {
        return canSlimeSpawnInChunk(posX >> 4, posZ >> 4, worldSeed);
    }

    public static boolean canSlimeSpawnInChunk(int chunkX, int chunkZ, long worldSeed)
    {
        long slimeSeed = 987234911L;
        long rngSeed = worldSeed +
                       (long) (chunkX * chunkX *  4987142) + (long) (chunkX * 5947611) +
                       (long) (chunkZ * chunkZ) * 4392871L + (long) (chunkZ * 389711) ^ slimeSeed;

        RAND.setSeed(rngSeed);

        return RAND.nextInt(10) == 0;
    }

    public static boolean isOverworld(World world)
    {
        return world.getDimension().natural();
    }

    public static boolean isStructureWithinRange(@Nullable BlockBox bb, BlockPos playerPos, int maxRange)
    {
        if (bb == null ||
            playerPos.getX() < (bb.getMinX() - maxRange) ||
            playerPos.getX() > (bb.getMaxX() + maxRange) ||
            playerPos.getZ() < (bb.getMinZ() - maxRange) ||
            playerPos.getZ() > (bb.getMaxZ() + maxRange))
        {
            return false;
        }

        return true;
    }

    public static boolean isStructureWithinRange(@Nullable IntBoundingBox bb, BlockPos playerPos, int maxRange)
    {
        if (bb == null ||
            playerPos.getX() < (bb.minX - maxRange) ||
            playerPos.getX() > (bb.maxX + maxRange) ||
            playerPos.getZ() < (bb.minZ - maxRange) ||
            playerPos.getZ() > (bb.maxZ + maxRange))
        {
            return false;
        }

        return true;
    }

    public static boolean areBoxesEqual(IntBoundingBox bb1, IntBoundingBox bb2)
    {
        return bb1.minX == bb2.minX && bb1.minY == bb2.minY && bb1.minZ == bb2.minZ &&
               bb1.maxX == bb2.maxX && bb1.maxY == bb2.maxY && bb1.maxZ == bb2.maxZ;
    }

    public static int getSpawnableChunksCount(@Nonnull ServerWorld world)
    {
        return world.getChunkManager().chunkLoadingManager.getTicketManager().getTickedChunkCount();
    }

    public static void addAxolotlTooltip(ItemStack stack, List<Text> lines)
    {
        NbtComponent entityData = stack.getComponents().get(DataComponentTypes.BUCKET_ENTITY_DATA);

        if (entityData != null)
        {
            NbtCompound tag = entityData.copyNbt();
            int variantId = tag.getInt(AxolotlEntity.VARIANT_KEY);
            // FIXME 1.19.3+ this is not validated now... with AIOOB it will return the entry for ID 0
            AxolotlEntity.Variant variant = AxolotlEntity.Variant.byId(variantId);
            String variantName = variant.getName();
            MutableText labelText = Text.translatable("minihud.label.axolotl_tooltip.label");
            MutableText valueText = Text.translatable("minihud.label.axolotl_tooltip.value", variantName, variantId);

            if (variantId < AXOLOTL_COLORS.length)
            {
                valueText.setStyle(Style.EMPTY.withColor(AXOLOTL_COLORS[variantId]));
            }

            lines.add(Math.min(1, lines.size()), labelText.append(valueText));
        }
    }

    public static void addBeeTooltip(ItemStack stack, List<Text> lines)
    {
        List<BeehiveBlockEntity.BeeData> beeList = stack.getComponents().get(DataComponentTypes.BEES);

        if (beeList != null && beeList.isEmpty() == false)
        {
            int count = beeList.size();
            int babyCount = 0;

            for (BeehiveBlockEntity.BeeData beeOccupant : beeList)
            {
                NbtComponent beeData = beeOccupant.entityData();
                NbtCompound beeTag = beeData.copyNbt();
                int beeTicks = beeOccupant.ticksInHive();
                String beeName = "";
                int beeAge = -1;

                if (beeTag.contains("CustomName", Constants.NBT.TAG_STRING))
                {
                    beeName = beeTag.getString("CustomName");
                }
                if (beeTag.contains("Age", Constants.NBT.TAG_INT))
                {
                    beeAge = beeTag.getInt("Age");
                }
                if (beeAge + beeTicks < 0)
                {
                    babyCount++;
                }

                if (beeName.isEmpty() == false)
                {
                    Text beeText;

                    try
                    {
                        beeText = Text.Serialization.fromJson(beeName, DataStorage.getInstance().getWorldRegistryManager());
                    }
                    catch (Exception ignored)
                    {
                        beeText = Text.of(beeName);
                    }

                    lines.add(Math.min(1, lines.size()), Text.translatable("minihud.label.bee_tooltip.name", beeText));
                }
            }
            Text text;

            if (babyCount > 0)
            {
                text = Text.translatable("minihud.label.bee_tooltip.count_babies", String.valueOf(count), String.valueOf(babyCount));
            }
            else
            {
                text = Text.translatable("minihud.label.bee_tooltip.count", String.valueOf(count));
            }

            lines.add(Math.min(1, lines.size()), text);
        }
    }

    public static void addHoneyTooltip(ItemStack stack, List<Text> lines)
    {
        BlockStateComponent blockItemState = stack.getComponents().get(DataComponentTypes.BLOCK_STATE);

        if (blockItemState != null && blockItemState.isEmpty() == false)
        {
            Integer honey = blockItemState.getValue(Properties.HONEY_LEVEL);
            String honeyLevel = "0";

            if (honey != null && (honey >= 0 && honey <= 5))
            {
                honeyLevel = String.valueOf(honey);
            }

            lines.add(Math.min(1, lines.size()), Text.translatable("minihud.label.honey_info.level", honeyLevel));
        }
    }

    public static int getFurnaceXpAmount(World world, AbstractFurnaceBlockEntity be)
    {
        Object2IntOpenHashMap<Identifier> recipes = ((IMixinAbstractFurnaceBlockEntity) be).minihud_getUsedRecipes();
        double xp = 0.0;

        for (Object2IntMap.Entry<Identifier> entry : recipes.object2IntEntrySet())
        {
            RecipeEntry<?> recipeEntry = world.getRecipeManager().get(entry.getKey()).orElse(null);

            if (recipeEntry != null && recipeEntry.value() instanceof AbstractCookingRecipe recipe)
            {
                xp += entry.getIntValue() * recipe.getExperience();
            }
        }

        return (int) xp;
    }

    public static int getFurnaceXpAmount(World world, @Nonnull NbtCompound nbt)
    {
        Object2IntOpenHashMap<Identifier> recipes = BlockUtils.getRecipesUsedFromNbt(nbt);
        double xp = 0.0;

        for (Object2IntMap.Entry<Identifier> entry : recipes.object2IntEntrySet())
        {
            RecipeEntry<?> recipeEntry = world.getRecipeManager().get(entry.getKey()).orElse(null);

            if (recipeEntry != null && recipeEntry.value() instanceof AbstractCookingRecipe recipe)
            {
                xp += entry.getIntValue() * recipe.getExperience();
            }
        }

        return (int) xp;
    }
}
