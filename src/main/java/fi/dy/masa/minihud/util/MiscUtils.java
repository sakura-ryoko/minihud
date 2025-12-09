package fi.dy.masa.minihud.util;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import org.apache.commons.lang3.math.Fraction;

import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.data.DataBlockUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.time.DurationFormat;
import fi.dy.masa.malilib.util.time.TimeFormat;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.data.HudDataManager;
import fi.dy.masa.minihud.mixin.block.IMixinAbstractFurnaceBlockEntity;

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

    public static boolean isOverworld(Level world)
    {
        return world.dimension() == Level.OVERWORLD;
    }

    public static boolean isStructureWithinRange(@Nullable BoundingBox bb, BlockPos playerPos, int maxRange)
    {
        return bb != null &&
                playerPos.getX() >= (bb.minX() - maxRange) &&
                playerPos.getX() <= (bb.maxX() + maxRange) &&
                playerPos.getZ() >= (bb.minZ() - maxRange) &&
                playerPos.getZ() <= (bb.maxZ() + maxRange);
    }

    public static boolean isStructureWithinRange(@Nullable IntBoundingBox bb, BlockPos playerPos, int maxRange)
    {
        return bb != null &&
                playerPos.getX() >= (bb.minX() - maxRange) &&
                playerPos.getX() <= (bb.maxX() + maxRange) &&
                playerPos.getZ() >= (bb.minZ() - maxRange) &&
                playerPos.getZ() <= (bb.maxZ() + maxRange);
    }

    public static boolean areBoxesEqual(IntBoundingBox bb1, IntBoundingBox bb2)
    {
        return bb1.minX() == bb2.minX() && bb1.minY() == bb2.minY() && bb1.minZ() == bb2.minZ() &&
               bb1.maxX() == bb2.maxX() && bb1.maxY() == bb2.maxY() && bb1.maxZ() == bb2.maxZ();
    }

    public static int getSpawnableChunksCount(@Nonnull ServerLevel world)
    {
        return world.getChunkSource().chunkMap.getDistanceManager().getNaturalSpawnChunkCount();
    }

    public static void addAxolotlTooltip(ItemStack stack, Consumer<Component> lines)
    {
        Axolotl.Variant variant = stack.getComponents().getOrDefault(DataComponents.AXOLOTL_VARIANT, Axolotl.Variant.LUCY);

//        MiniHUD.LOGGER.error("addAxolotlTooltip(): NBT: [{}]", nbt.toString());
        int variantId = variant.getId();
//        AxolotlEntity.Variant variant = AxolotlEntity.Variant.byIndex(variantId);
        String variantName = variant.getName();
        MutableComponent labelText = Component.translatable("minihud.label.axolotl_tooltip.label");
        MutableComponent valueText = Component.translatable("minihud.label.axolotl_tooltip.value", variantName, variantId);

        if (variantId < AXOLOTL_COLORS.length)
        {
            valueText.setStyle(Style.EMPTY.withColor(AXOLOTL_COLORS[variantId]));
        }

        lines.accept(labelText.append(valueText));
    }

    public static void addBeeTooltip(ItemStack stack, Consumer<Component> lines)
    {
        Bees bees = stack.getComponents().getOrDefault(DataComponents.BEES, Bees.EMPTY);
        List<BeehiveBlockEntity.Occupant> beeList = bees.bees();

        if (beeList != null && beeList.isEmpty() == false)
        {
            int count = beeList.size();
            int babyCount = 0;

            for (BeehiveBlockEntity.Occupant beeOccupant : beeList)
            {
                TypedEntityData<EntityType<?>> beeData = beeOccupant.entityData();
                CompoundTag beeTag = beeData.copyTagWithoutId();
                int beeTicks = beeOccupant.ticksInHive();
                Optional<Component> beeName = Optional.empty();
                int beeAge = -1;

                if (beeTag.contains("CustomName"))
                {
                    Tag nbtName = beeTag.get("CustomName");

                    if (nbtName != null)
                    {
                        DataResult<Component> dr = ComponentSerialization.CODEC.parse(DataStorage.getInstance().getWorldRegistryManager().createSerializationContext(NbtOps.INSTANCE), nbtName);

                        if (dr.isSuccess())
                        {
                            beeName = Optional.of(dr.getPartialOrThrow());
                        }
                    }
                }
                if (beeTag.contains("Age"))
                {
                    beeAge = beeTag.getIntOr("Age", 0);
                }
                if (beeAge + beeTicks < 0)
                {
                    babyCount++;
                }

                //beeName.ifPresent(text -> lines.accept(StringUtils.translateAsText("minihud.label.bee_tooltip.name", text.getString())));
                beeName.ifPresent(text -> lines.accept(Component.translatable("minihud.label.bee_tooltip.name", text)));
            }

            if (babyCount > 0)
            {
                lines.accept(StringUtils.translateAsText("minihud.label.bee_tooltip.count_babies", String.valueOf(count), String.valueOf(babyCount)));
            }
            else
            {
                lines.accept(StringUtils.translateAsText("minihud.label.bee_tooltip.count", String.valueOf(count)));
            }
        }
    }

    public static void addBundleTooltip(ItemStack stack, Consumer<Component> lines)
    {
        BundleContents bundleData = stack.get(DataComponents.BUNDLE_CONTENTS);
        final int maxCount = Configs.Generic.BUNDLE_TOOLTIPS_FILL_LEVEL.getIntegerValue();

        if (bundleData != null)
        {
            Fraction occupancy = bundleData.weight();
            int count;
            float fillPercent;

            if (maxCount != 64)
            {
                count = InventoryUtils.recalculateBundleSize(bundleData, maxCount);
                fillPercent = 100 * ((float) count / maxCount);
            }
            else
            {
                count = Mth.mulAndTruncate(occupancy, maxCount);
                fillPercent = 100 * occupancy.floatValue();
            }

            if (count > maxCount)
            {
                lines.accept(StringUtils.translateAsText("minihud.label.bundle_tooltip.count.full", count, maxCount, fillPercent));
            }
            else
            {
                lines.accept(StringUtils.translateAsText("minihud.label.bundle_tooltip.count", count, maxCount, fillPercent));
            }
        }
    }

    public static void addHoneyTooltip(ItemStack stack, Consumer<Component> lines)
    {
        BlockItemStateProperties blockItemState = stack.getComponents().get(DataComponents.BLOCK_STATE);

        if (blockItemState != null && blockItemState.isEmpty() == false)
        {
            Integer honey = blockItemState.get(BlockStateProperties.LEVEL_HONEY);
            String honeyLevel = "0";

            if (honey != null && (honey >= 0 && honey <= 5))
            {
                honeyLevel = String.valueOf(honey);
            }

            lines.accept(StringUtils.translateAsText("minihud.label.honey_info.level", honeyLevel));
        }
    }

    public static void addCustomModelTooltip(ItemStack stack, Consumer<Component> lines)
    {
        CustomModelData data = stack.get(DataComponents.CUSTOM_MODEL_DATA);

        if (data != null)
        {
            // Only display the first entry of any type
            Float aFloat = data.getFloat(0);
            Boolean aFlag = data.getBoolean(0);
            String aString = data.getString(0);
            Integer aColor = data.getColor(0);

            if (aFloat != null)
            {
                lines.accept(StringUtils.translateAsText("minihud.label.custom_model_data_tooltip.float", aFloat));
            }
            if (aFlag != null)
            {
                lines.accept(StringUtils.translateAsText("minihud.label.custom_model_data_tooltip.flag", aFlag));
            }
            if (aString != null)
            {
                lines.accept(StringUtils.translateAsText("minihud.label.custom_model_data_tooltip.string", aString));
            }
            if (aColor != null)
            {
                lines.accept(StringUtils.translateAsText("minihud.label.custom_model_data_tooltip.color", aColor));
            }
        }
    }

    public static void addFoodTooltip(ItemStack stack, Consumer<Component> lines)
    {
        FoodProperties data = stack.get(DataComponents.FOOD);

        if (data != null)
        {
            lines.accept(StringUtils.translateAsText("minihud.label.food_tooltip", ((float) data.nutrition() / 2) , data.saturation()));
        }
    }

    public static void addLodestoneTooltip(ItemStack stack, Consumer<Component> lines)
    {
        LodestoneTracker data = stack.get(DataComponents.LODESTONE_TRACKER);

        if (data != null && data.target().isPresent())
        {
            GlobalPos pos = data.target().get();
            lines.accept(StringUtils.translateAsText("minihud.label.lodestone_tooltip", pos.dimension().identifier().getPath(), pos.pos().toShortString()));
        }
    }

    public static int getFurnaceXpAmount(ServerLevel world, AbstractFurnaceBlockEntity be)
    {
        Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> recipes = ((IMixinAbstractFurnaceBlockEntity) be).minihud_getUsedRecipes();
        double xp = 0.0;

        if (recipes == null || recipes.isEmpty())
        {
            return -1;
        }

        for (Reference2IntMap.Entry<ResourceKey<Recipe<?>>> entry : recipes.reference2IntEntrySet())
        {
            RecipeHolder<?> recipeEntry = world.recipeAccess().byKey(entry.getKey()).orElse(null);

            if (recipeEntry != null)
            {
                xp += entry.getIntValue() * ((AbstractCookingRecipe) recipeEntry.value()).experience();
            }
        }

        return (int) xp;
    }

    public static int getFurnaceXpAmount(ServerLevel world, @Nonnull CompoundData data)
    {
        Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> recipes = DataBlockUtils.getRecipesUsed(data);
        double xp = 0.0;

        if (recipes.isEmpty())
        {
            return -1;
        }

        for (Reference2IntMap.Entry<ResourceKey<Recipe<?>>> entry : recipes.reference2IntEntrySet())
        {
            RecipeHolder<?> recipeEntry = world.recipeAccess().byKey(entry.getKey()).orElse(null);

            if (recipeEntry != null)
            {
                xp += entry.getIntValue() * ((AbstractCookingRecipe) recipeEntry.value()).experience();
            }
        }

        return (int) xp;
    }

    // Servux Synced Recipe Manager required
    public static int getFurnaceXpAmount(AbstractFurnaceBlockEntity be)
    {
        Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> recipes = ((IMixinAbstractFurnaceBlockEntity) be).minihud_getUsedRecipes();
        double xp = 0.0;

        if (recipes == null || recipes.isEmpty() || HudDataManager.getInstance().getPreparedRecipes() == null)
        {
            return -1;
        }

        for (Reference2IntMap.Entry<ResourceKey<Recipe<?>>> entry : recipes.reference2IntEntrySet())
        {
            RecipeHolder<?> recipeEntry = HudDataManager.getInstance().getPreparedRecipes().byKey(entry.getKey());

            if (recipeEntry != null)
            {
                xp += entry.getIntValue() * ((AbstractCookingRecipe) recipeEntry.value()).experience();
            }
        }

        return (int) xp;
    }

    public static int getFurnaceXpAmount(@Nonnull CompoundData data)
    {
        Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> recipes = DataBlockUtils.getRecipesUsed(data);
        double xp = 0.0;

        if (recipes.isEmpty() || HudDataManager.getInstance().getPreparedRecipes() == null)
        {
            return -1;
        }

        for (Reference2IntMap.Entry<ResourceKey<Recipe<?>>> entry : recipes.reference2IntEntrySet())
        {
            RecipeHolder<?> recipeEntry = HudDataManager.getInstance().getPreparedRecipes().byKey(entry.getKey());

            if (recipeEntry != null)
            {
                xp += entry.getIntValue() * ((AbstractCookingRecipe) recipeEntry.value()).experience();
            }
        }

        return (int) xp;
    }

    public static String formatDateNow()
    {
        return formatDateFromEpoch(-1);
    }

    public static String formatDateFromEpoch(long epochMs)
    {
        TimeFormat type = (TimeFormat) Configs.Generic.DATE_FORMAT_TYPE.getOptionListValue();

        if (epochMs < 0)
        {
            return type.formatNow(Configs.Generic.DATE_FORMAT_STRING.getStringValue());
        }

        return type.formatTo(epochMs, Configs.Generic.DATE_FORMAT_STRING.getStringValue());
    }

    public static long getEpochMsFromString(String time)
    {
        TimeFormat type = (TimeFormat) Configs.Generic.DATE_FORMAT_TYPE.getOptionListValue();

        return type.formatFrom(time, Configs.Generic.DATE_FORMAT_STRING.getStringValue());
    }

    public static String formatDuration(long duration)
    {
        DurationFormat type = (DurationFormat) Configs.Generic.DURATION_FORMAT_TYPE.getOptionListValue();
        return type.format(duration, Configs.Generic.DURATION_FORMAT_STRING.getStringValue());
    }
}
