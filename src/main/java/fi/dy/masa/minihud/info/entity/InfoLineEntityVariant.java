package fi.dy.masa.minihud.info.entity;

import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.text.Text;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.entity.passive.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.nbt.NbtEntityUtils;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;

public class InfoLineEntityVariant extends InfoLine
{
    private static final String VARIANT_KEY = Reference.MOD_ID+".info_line.entity_variant";

    public InfoLineEntityVariant(InfoToggle type)
    {
        super(type);
    }

    public InfoLineEntityVariant()
    {
        super(InfoToggle.ENTITY_VARIANT);
    }

    @Override
    public @Nullable Entry parse(@Nonnull Context ctx)
    {
        if (Configs.Generic.INFO_LINES_USES_NBT.getBooleanValue() &&
            ctx.hasLiving() && ctx.hasNbt())
        {
            EntityType<?> entityType = NbtEntityUtils.getEntityTypeFromNbt(ctx.nbt());
            if (entityType == null) return null;

            return this.parseNbt(ctx.world(), entityType, ctx.nbt());
        }

        return ctx.ent() != null ? this.parseEnt(ctx.world(), ctx.ent()) : null;
    }

    @Override
    public @Nullable Entry parseNbt(@Nonnull World world, @Nonnull EntityType<?> entityType, @Nonnull NbtCompound nbt)
    {
        if (entityType.equals(EntityType.AXOLOTL))
        {
            AxolotlEntity.Variant variant = NbtEntityUtils.getAxolotlVariantFromNbt(nbt);

            if (variant != null)
            {
                return this.translate(VARIANT_KEY+".axolotl", variant.getId());
            }
        }
        else if (entityType.equals(EntityType.CAT))
        {
            Pair<RegistryKey<CatVariant>, DyeColor> catPair = NbtEntityUtils.getCatVariantFromNbt(nbt, world.getRegistryManager());

            if (catPair.getLeft() != null)
            {
                return this.translate(VARIANT_KEY+".cat", catPair.getLeft().getValue().getPath(), catPair.getRight().getName());
            }
        }
        else if (entityType.equals(EntityType.COW))
        {
            RegistryKey<CowVariant> variant = NbtEntityUtils.getCowVariantFromNbt(nbt, world.getRegistryManager());

            if (variant != null)
            {
                return this.translate(VARIANT_KEY+".cow", variant.getValue().getPath());
            }
        }
        else if (entityType.equals(EntityType.CHICKEN))
        {
            RegistryKey<ChickenVariant> variant = NbtEntityUtils.getChickenVariantFromNbt(nbt, world.getRegistryManager());

            if (variant != null)
            {
                return this.translate(VARIANT_KEY+".chicken", variant.getValue().getPath());
            }
        }
        else if (entityType.equals(EntityType.MOOSHROOM))
        {
            MooshroomEntity.Variant mooType = NbtEntityUtils.getMooshroomVariantFromNbt(nbt);

            if (mooType != null)
            {
                return this.translate(VARIANT_KEY + ".mooshroom", mooType.asString());
            }
        }
        else if (entityType.equals(EntityType.FOX))
        {
            FoxEntity.Variant foxType = NbtEntityUtils.getFoxVariantFromNbt(nbt);

            if (foxType != null)
            {
                return this.translate(VARIANT_KEY+".fox", foxType.asString());
            }
        }
        else if (entityType.equals(EntityType.FROG))
        {
            RegistryKey<FrogVariant> variant = NbtEntityUtils.getFrogVariantFromNbt(nbt, world.getRegistryManager());

            if (variant != null)
            {
                return this.translate(VARIANT_KEY+".frog", variant.getValue().getPath());
            }
        }
        else if (entityType.equals(EntityType.HORSE))
        {
            Pair<HorseColor, HorseMarking> horsePair = NbtEntityUtils.getHorseVariantFromNbt(nbt);

            if (horsePair.getLeft() != null)
            {
                return this.translate(VARIANT_KEY+".horse", horsePair.getLeft().asString(), horsePair.getRight().name().toLowerCase());
            }
        }
        else if (entityType.equals(EntityType.LLAMA) || entityType.equals(EntityType.TRADER_LLAMA))
        {
            Pair<LlamaEntity.Variant, Integer> llamaPair = NbtEntityUtils.getLlamaTypeFromNbt(nbt);

            if (llamaPair.getLeft() != null)
            {
                return this.translate(VARIANT_KEY+".llama", llamaPair.getLeft().asString(), llamaPair.getRight());
            }
        }
        else if (entityType.equals(EntityType.PAINTING))
        {
            Pair<Direction, PaintingVariant> paintingPair = NbtEntityUtils.getPaintingDataFromNbt(nbt, world.getRegistryManager());

            if (paintingPair.getRight() != null)
            {
                Optional<net.minecraft.text.Text> title = paintingPair.getRight().title();
                Optional<net.minecraft.text.Text> author = paintingPair.getRight().author();

                if (title.isPresent() && author.isPresent())
                {
                    return this.translate(VARIANT_KEY+".painting.both", title.get().getString(), author.get().getString());
                }
                else if (title.isPresent())
                {
                    return this.translate(VARIANT_KEY+".painting.title_only", title.get().getString());
                }
                else if (author.isPresent())
                {
                    return this.translate(VARIANT_KEY+".painting.author_only", author.get().getString());
                }
            }
        }
        else if (entityType.equals(EntityType.PARROT))
        {
            ParrotEntity.Variant variant = NbtEntityUtils.getParrotVariantFromNbt(nbt);

            if (variant != null)
            {
                return this.translate(VARIANT_KEY+".parrot", variant.asString());
            }
        }
        else if (entityType.equals(EntityType.PIG))
        {
            RegistryKey<PigVariant> variant = NbtEntityUtils.getPigVariantFromNbt(nbt, world.getRegistryManager());

            if (variant != null)
            {
                return this.translate(VARIANT_KEY+".pig", variant.getValue().getPath());
            }
        }
        else if (entityType.equals(EntityType.RABBIT))
        {
            RabbitEntity.Variant rabbitType = NbtEntityUtils.getRabbitTypeFromNbt(nbt);

            if (rabbitType != null)
            {
                return this.translate(VARIANT_KEY+".rabbit", rabbitType.asString());
            }
        }
        else if (entityType.equals(EntityType.SALMON))
        {
            SalmonEntity.Variant salmonVariant = NbtEntityUtils.getSalmonVariantFromNbt(nbt);

            if (salmonVariant != null)
            {
                return this.translate(VARIANT_KEY+".salmon", salmonVariant.asString());
            }
        }
        else if (entityType.equals(EntityType.SHEEP))
        {
            DyeColor color = NbtEntityUtils.getSheepColorFromNbt(nbt);

            if (color != null)
            {
                return this.translate(VARIANT_KEY+".sheep", color.getName());
            }
        }
        else if (entityType.equals(EntityType.TROPICAL_FISH))
        {
            TropicalFishEntity.Pattern variant = NbtEntityUtils.getFishVariantFromNbt(nbt);

            if (variant != null)
            {
                return this.translate(VARIANT_KEY+".tropical_fish", variant.asString());
            }
        }
        else if (entityType.equals(EntityType.WOLF))
        {
            Pair<RegistryKey<WolfVariant>, DyeColor> wolfPair = NbtEntityUtils.getWolfVariantFromNbt(nbt);

            if (wolfPair.getLeft() != null)
            {
                return this.translate(VARIANT_KEY+".wolf", wolfPair.getLeft().getValue().getPath(), wolfPair.getRight().getName());
            }
        }

        return null;
    }

    @Override
    public @Nullable Entry parseEnt(@Nonnull World world, @Nonnull Entity ent)
    {
        switch (ent)
        {
            case AxolotlEntity axolotl ->
            {
                return this.translate(VARIANT_KEY + ".axolotl", axolotl.getVariant().name());
            }
            case CatEntity cat ->
            {
                RegistryKey<CatVariant> variant = cat.getVariant().getKey().orElse(CatVariants.BLACK);
                return this.translate(VARIANT_KEY + ".cat", variant.getValue().getPath(), cat.getCollarColor().getName());
            }
            case ChickenEntity chicken ->
            {
                RegistryKey<ChickenVariant> variant = chicken.getVariant().getKey().orElse(ChickenVariants.DEFAULT);
                return this.translate(VARIANT_KEY + ".chicken", variant.getValue().getPath());
            }
            case CowEntity cow ->
            {
                RegistryKey<CowVariant> variant = cow.getVariant().getKey().orElse(CowVariants.DEFAULT);
                return this.translate(VARIANT_KEY + ".cow", variant.getValue().getPath());
            }
            case MooshroomEntity mooshroom ->
            {
                return this.translate(VARIANT_KEY + ".mooshroom", mooshroom.getVariant().asString());
            }
            case FoxEntity fox ->
            {
                return this.translate(VARIANT_KEY + ".fox", fox.getVariant().asString());
            }
            case FrogEntity frog ->
            {
                RegistryKey<FrogVariant> variant = frog.getVariant().getKey().orElse(FrogVariants.TEMPERATE);
                return this.translate(VARIANT_KEY + ".frog", variant.getValue().getPath());
            }
            case HorseEntity horse ->
            {
                return this.translate(VARIANT_KEY + ".horse", horse.getHorseColor().asString(), horse.getMarking().name().toLowerCase());
            }
            case LlamaEntity llama ->
            {
                return this.translate(VARIANT_KEY + ".llama", llama.getVariant().asString(), llama.getStrength());
            }
            case PaintingEntity painting ->
            {
                PaintingVariant paintingVariant = painting.getVariant().value();

                if (paintingVariant != null)
                {
                    Optional<Text> title = paintingVariant.title();
                    Optional<Text> author = paintingVariant.author();

                    if (title.isPresent() && author.isPresent())
                    {
                        return this.translate(VARIANT_KEY + ".painting.both", title.get().getString(), author.get().getString());
                    }
                    else if (title.isPresent())
                    {
                        return this.translate(VARIANT_KEY + ".painting.title_only", title.get().getString());
                    }
                    else if (author.isPresent())
                    {
                        return this.translate(VARIANT_KEY + ".painting.author_only", author.get().getString());
                    }
                }
            }
            case ParrotEntity parrot ->
            {
                return this.translate(VARIANT_KEY + ".parrot", parrot.getVariant().asString());
            }
            case PigEntity pig ->
            {
                RegistryKey<PigVariant> pigVariant = pig.getVariant().getKey().orElse(PigVariants.DEFAULT);
                return this.translate(VARIANT_KEY + ".pig", pigVariant.getValue().getPath());
            }
            case RabbitEntity rabbit ->
            {
                return this.translate(VARIANT_KEY + ".rabbit", rabbit.getVariant().asString());
            }
            case SalmonEntity salmon ->
            {
                return this.translate(VARIANT_KEY + ".salmon", salmon.getVariant().asString());
            }
            case SheepEntity sheep ->
            {
                return this.translate(VARIANT_KEY + ".sheep", sheep.getColor().getName());
            }
            case TropicalFishEntity fish ->
            {
                return this.translate(VARIANT_KEY + ".tropical_fish", fish.getVariety().asString());
            }
            case WolfEntity wolf ->
            {
                Pair<RegistryKey<WolfVariant>, DyeColor> wolfPair = EntityUtils.getWolfVariantFromNbt(wolf);
                return this.translate(VARIANT_KEY + ".wolf", wolfPair.getLeft().getValue().getPath(), wolfPair.getRight().getName());
            }
            default ->
            {
                return null;
            }
        }

        return null;
    }
}
