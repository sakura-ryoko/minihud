package fi.dy.masa.minihud.info;

import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.nbt.NbtEntityUtils;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.Configs;
import net.minecraft.class_10730;
import net.minecraft.class_10731;
import net.minecraft.class_10733;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.entity.passive.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class InfoLineEntityVariant extends InfoLine
{
    private static final String VARIANT_KEY = Reference.MOD_ID+".info_line.entity_variant";

    @Override
    public @Nullable Text parse(@Nonnull Context ctx)
    {
        if (Configs.Generic.INFO_LINES_USES_NBT.getBooleanValue() &&
            ctx.hasLivingEntity() && ctx.hasNbt())
        {
            EntityType<?> entityType = NbtEntityUtils.getEntityTypeFromNbt(ctx.nbt());
            if (entityType == null) return null;

            if (entityType.equals(EntityType.AXOLOTL))
            {
                AxolotlEntity.Variant variant = NbtEntityUtils.getAxolotlVariantFromNbt(ctx.nbt());

                if (variant != null)
                {
                    return new Text(VARIANT_KEY+".axolotl", variant.getId());
                }
            }
            else if (entityType.equals(EntityType.CAT))
            {
                Pair<RegistryKey<CatVariant>, DyeColor> catPair = NbtEntityUtils.getCatVariantFromNbt(ctx.nbt(), ctx.world().getRegistryManager());

                if (catPair.getLeft() != null)
                {
                    return new Text(VARIANT_KEY+".cat", catPair.getLeft().getValue().getPath(), catPair.getRight().getName());
                }
            }
            else if (entityType.equals(EntityType.COW))
            {
                RegistryKey<class_10731> variant = NbtEntityUtils.getCowVariantFromNbt(ctx.nbt(), ctx.world().getRegistryManager());

                if (variant != null)
                {
                    return new Text(VARIANT_KEY+".cow", variant.getValue().getPath());
                }
            }
            else if (entityType.equals(EntityType.MOOSHROOM))
            {
                return new Text(VARIANT_KEY+".cow", "mooshroom");
            }
            else if (entityType.equals(EntityType.FOX))
            {
                FoxEntity.Variant foxType = NbtEntityUtils.getFoxVariantFromNbt(ctx.nbt());

                if (foxType != null)
                {
                    return new Text(VARIANT_KEY+".fox", foxType.asString());
                }
            }
            else if (entityType.equals(EntityType.FROG))
            {
                RegistryKey<FrogVariant> variant = NbtEntityUtils.getFrogVariantFromNbt(ctx.nbt(), ctx.world().getRegistryManager());

                if (variant != null)
                {
                    return new Text(VARIANT_KEY+".frog", variant.getValue().getPath());
                }
            }
            else if (entityType.equals(EntityType.HORSE))
            {
                Pair<HorseColor, HorseMarking> horsePair = NbtEntityUtils.getHorseVariantFromNbt(ctx.nbt());

                if (horsePair.getLeft() != null)
                {
                    return new Text(VARIANT_KEY+".horse", horsePair.getLeft().asString(), horsePair.getRight().name().toLowerCase());
                }
            }
            else if (entityType.equals(EntityType.LLAMA) || entityType.equals(EntityType.TRADER_LLAMA))
            {
                Pair<LlamaEntity.Variant, Integer> llamaPair = NbtEntityUtils.getLlamaTypeFromNbt(ctx.nbt());

                if (llamaPair.getLeft() != null)
                {
                    return new Text(VARIANT_KEY+".llama", llamaPair.getLeft().asString(), llamaPair.getRight());
                }
            }
            else if (entityType.equals(EntityType.PAINTING))
            {
                Pair<Direction, PaintingVariant> paintingPair = NbtEntityUtils.getPaintingDataFromNbt(ctx.nbt(), ctx.world().getRegistryManager());

                if (paintingPair.getRight() != null)
                {
                    Optional<net.minecraft.text.Text> title = paintingPair.getRight().title();
                    Optional<net.minecraft.text.Text> author = paintingPair.getRight().author();

                    if (title.isPresent() && author.isPresent())
                    {
                        return new Text(VARIANT_KEY+".painting.both", title.get().getString(), author.get().getString());
                    }
                    else if (title.isPresent())
                    {
                        return new Text(VARIANT_KEY+".painting.title_only", title.get().getString());
                    }
                    else if (author.isPresent())
                    {
                        return new Text(VARIANT_KEY+".painting.author_only", author.get().getString());
                    }
                }
            }
            else if (entityType.equals(EntityType.PARROT))
            {
                ParrotEntity.Variant variant = NbtEntityUtils.getParrotVariantFromNbt(ctx.nbt());

                if (variant != null)
                {
                    return new Text(VARIANT_KEY+".parrot", variant.asString());
                }
            }
            else if (entityType.equals(EntityType.PIG))
            {
                RegistryKey<PigVariant> variant = NbtEntityUtils.getPigVariantFromNbt(ctx.nbt(), ctx.world().getRegistryManager());

                if (variant != null)
                {
                    return new Text(VARIANT_KEY+".pig", variant.getValue().getPath());
                }
            }
            else if (entityType.equals(EntityType.RABBIT))
            {
                RabbitEntity.Variant rabbitType = NbtEntityUtils.getRabbitTypeFromNbt(ctx.nbt());

                if (rabbitType != null)
                {
                    return new Text(VARIANT_KEY+".rabbit", rabbitType.asString());
                }
            }
            else if (entityType.equals(EntityType.SALMON))
            {
                SalmonEntity.Variant salmonVariant = NbtEntityUtils.getSalmonVariantFromNbt(ctx.nbt());

                if (salmonVariant != null)
                {
                    return new Text(VARIANT_KEY+".salmon", salmonVariant.asString());
                }
            }
            else if (entityType.equals(EntityType.SHEEP))
            {
                DyeColor color = NbtEntityUtils.getSheepColorFromNbt(ctx.nbt());

                if (color != null)
                {
                    return new Text(VARIANT_KEY+".sheep", color.getName());
                }
            }
            else if (entityType.equals(EntityType.TROPICAL_FISH))
            {
                TropicalFishEntity.Pattern variant = NbtEntityUtils.getFishVariantFromNbt(ctx.nbt());

                if (variant != null)
                {
                    return new Text(VARIANT_KEY+".tropical_fish", variant.asString());
                }
            }
            else if (entityType.equals(EntityType.WOLF))
            {
                Pair<RegistryKey<WolfVariant>, DyeColor> wolfPair = NbtEntityUtils.getWolfVariantFromNbt(ctx.nbt());

                if (wolfPair.getLeft() != null)
                {
                    return new Text(VARIANT_KEY+".wolf", wolfPair.getLeft().getValue().getPath(), wolfPair.getRight().getName());
                }
            }
        }
        else if (ctx.ent() instanceof AxolotlEntity axolotl)
        {
            return new Text(VARIANT_KEY+".axolotl", axolotl.getVariant().name());
        }
        else if (ctx.ent() instanceof CatEntity cat)
        {
            RegistryKey<CatVariant> variant = cat.getVariant().getKey().orElse(CatVariants.BLACK);
            return new Text(VARIANT_KEY+".cat", variant.getValue().getPath(), cat.getCollarColor().getName());
        }
        else if (ctx.ent() instanceof class_10730 cow)
        {
            RegistryKey<class_10731> variant = cow.method_67349().getKey().orElse(class_10733.field_56438);
            return new Text(VARIANT_KEY+".cow", variant.getValue().getPath());
        }
        else if (ctx.ent() instanceof MooshroomEntity)
        {
            return new Text(VARIANT_KEY+".cow", "mooshroom");
        }
        else if (ctx.ent() instanceof FoxEntity fox)
        {
            return new Text(VARIANT_KEY+".fox", fox.getVariant().asString());
        }
        else if (ctx.ent() instanceof FrogEntity frog)
        {
            RegistryKey<FrogVariant> variant = frog.getVariant().getKey().orElse(FrogVariants.TEMPERATE);
            return new Text(VARIANT_KEY+".frog", variant.getValue().getPath());
        }
        else if (ctx.ent() instanceof HorseEntity horse)
        {
            return new Text(VARIANT_KEY+".horse", horse.getHorseColor().asString(), horse.getMarking().name().toLowerCase());
        }
        else if (ctx.ent() instanceof LlamaEntity llama)
        {
            return new Text(VARIANT_KEY+".llama", llama.getVariant().asString(), llama.getStrength());
        }
        else if (ctx.ent() instanceof PaintingEntity painting)
        {
            PaintingVariant paintingVariant = painting.getVariant().value();

            if (paintingVariant != null)
            {
                Optional<net.minecraft.text.Text> title = paintingVariant.title();
                Optional<net.minecraft.text.Text> author = paintingVariant.author();

                if (title.isPresent() && author.isPresent())
                {
                    return new Text(VARIANT_KEY+".painting.both", title.get().getString(), author.get().getString());
                }
                else if (title.isPresent())
                {
                    return new Text(VARIANT_KEY+".painting.title_only", title.get().getString());
                }
                else if (author.isPresent())
                {
                    return new Text(VARIANT_KEY+".painting.author_only", author.get().getString());
                }
            }
        }
        else if (ctx.ent() instanceof ParrotEntity parrot)
        {
            return new Text(VARIANT_KEY+".parrot", parrot.getVariant().asString());
        }
        else if (ctx.ent() instanceof PigEntity pig)
        {
            return new Text(VARIANT_KEY+".pig", pig.getVariant().getKey().get().getValue().getPath());
        }
        else if (ctx.ent() instanceof RabbitEntity rabbit)
        {
            return new Text(VARIANT_KEY+".rabbit", rabbit.getVariant().asString());
        }
        else if (ctx.ent() instanceof SalmonEntity salmon)
        {
            return new Text(VARIANT_KEY+".salmon", salmon.getVariant().asString());
        }
        else if (ctx.ent() instanceof SheepEntity sheep)
        {
            return new Text(VARIANT_KEY+".sheep", sheep.getColor().getName());
        }
        else if (ctx.ent() instanceof TropicalFishEntity fish)
        {
            return new Text(VARIANT_KEY+".tropical_fish", fish.getVariety().asString());
        }
        else if (ctx.ent() instanceof WolfEntity wolf)
        {
            Pair<RegistryKey<WolfVariant>, DyeColor> wolfPair = EntityUtils.getWolfVariantFromNbt(wolf);
            return new Text(VARIANT_KEY+".wolf", wolfPair.getLeft().getValue().getPath(), wolfPair.getRight().getName());
        }
        return null;
    }
}
