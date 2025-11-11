package fi.dy.masa.minihud.info.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.frog.FrogVariant;
import net.minecraft.world.entity.animal.frog.FrogVariants;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.Markings;
import net.minecraft.world.entity.animal.horse.Variant;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant;
import net.minecraft.world.entity.animal.wolf.WolfVariant;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.nbt.NbtEntityUtils;
import fi.dy.masa.minihud.Reference;
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
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@Nonnull Context ctx)
    {
        if (ctx.world() == null) return null;

        if (ctx.hasLiving() && ctx.hasNbt())
        {
            EntityType<?> entityType = NbtEntityUtils.getEntityTypeFromNbt(ctx.nbt());
            if (entityType == null) return null;

            return this.parseNbt(ctx.world(), entityType, ctx.nbt());
        }

        return ctx.ent() != null ? this.parseEnt(ctx.world(), ctx.ent()) : null;
    }

    @Override
    public List<Entry> parseNbt(@Nonnull Level world, @Nonnull EntityType<?> entityType, @Nonnull CompoundTag nbt)
    {
        List<Entry> list = new ArrayList<>();

        if (entityType.equals(EntityType.AXOLOTL))
        {
            Axolotl.Variant variant = NbtEntityUtils.getAxolotlVariantFromNbt(nbt);

            if (variant != null)
            {
                list.add(this.translate(VARIANT_KEY+".axolotl", variant.getName()));
            }
        }
        else if (entityType.equals(EntityType.CAT))
        {
            Pair<ResourceKey<CatVariant>, DyeColor> catPair = NbtEntityUtils.getCatVariantFromNbt(nbt, world.registryAccess());

            if (catPair.getLeft() != null)
            {
                list.add(this.translate(VARIANT_KEY+".cat", catPair.getLeft().location().getPath(), catPair.getRight().getName()));
            }
        }
        else if (entityType.equals(EntityType.COW))
        {
            ResourceKey<CowVariant> variant = NbtEntityUtils.getCowVariantFromNbt(nbt, world.registryAccess());

            if (variant != null)
            {
                list.add(this.translate(VARIANT_KEY+".cow", variant.location().getPath()));
            }
        }
        else if (entityType.equals(EntityType.CHICKEN))
        {
            ResourceKey<ChickenVariant> variant = NbtEntityUtils.getChickenVariantFromNbt(nbt, world.registryAccess());

            if (variant != null)
            {
                list.add(this.translate(VARIANT_KEY+".chicken", variant.location().getPath()));
            }
        }
        else if (entityType.equals(EntityType.MOOSHROOM))
        {
            MushroomCow.Variant mooType = NbtEntityUtils.getMooshroomVariantFromNbt(nbt);

            if (mooType != null)
            {
                list.add(this.translate(VARIANT_KEY + ".mooshroom", mooType.getSerializedName()));
            }
        }
        else if (entityType.equals(EntityType.FOX))
        {
            Fox.Variant foxType = NbtEntityUtils.getFoxVariantFromNbt(nbt);

            if (foxType != null)
            {
                list.add(this.translate(VARIANT_KEY+".fox", foxType.getSerializedName()));
            }
        }
        else if (entityType.equals(EntityType.FROG))
        {
            ResourceKey<FrogVariant> variant = NbtEntityUtils.getFrogVariantFromNbt(nbt, world.registryAccess());

            if (variant != null)
            {
                list.add(this.translate(VARIANT_KEY+".frog", variant.location().getPath()));
            }
        }
        else if (entityType.equals(EntityType.HORSE))
        {
            Pair<Variant, Markings> horsePair = NbtEntityUtils.getHorseVariantFromNbt(nbt);

            if (horsePair.getLeft() != null)
            {
                list.add(this.translate(VARIANT_KEY+".horse", horsePair.getLeft().getSerializedName(), horsePair.getRight().name().toLowerCase()));
            }
        }
        else if (entityType.equals(EntityType.LLAMA) || entityType.equals(EntityType.TRADER_LLAMA))
        {
            Pair<Llama.Variant, Integer> llamaPair = NbtEntityUtils.getLlamaTypeFromNbt(nbt);

            if (llamaPair.getLeft() != null)
            {
                list.add(this.translate(VARIANT_KEY+".llama", llamaPair.getLeft().getSerializedName(), llamaPair.getRight()));
            }
        }
        else if (entityType.equals(EntityType.PAINTING))
        {
            Pair<Direction, PaintingVariant> paintingPair = NbtEntityUtils.getPaintingDataFromNbt(nbt, world.registryAccess());

            if (paintingPair.getRight() != null)
            {
                Optional<net.minecraft.network.chat.Component> title = paintingPair.getRight().title();
                Optional<net.minecraft.network.chat.Component> author = paintingPair.getRight().author();

                if (title.isPresent() && author.isPresent())
                {
                    list.add(this.translate(VARIANT_KEY+".painting.both", title.get().getString(), author.get().getString()));
                }
                else if (title.isPresent())
                {
                    list.add(this.translate(VARIANT_KEY+".painting.title_only", title.get().getString()));
                }
                else
                {
                    author.ifPresent(text -> list.add(this.translate(VARIANT_KEY + ".painting.author_only", text.getString())));
                }
            }
        }
        else if (entityType.equals(EntityType.PARROT))
        {
            Parrot.Variant variant = NbtEntityUtils.getParrotVariantFromNbt(nbt);

            if (variant != null)
            {
                list.add(this.translate(VARIANT_KEY+".parrot", variant.getSerializedName()));
            }
        }
        else if (entityType.equals(EntityType.PIG))
        {
            ResourceKey<PigVariant> variant = NbtEntityUtils.getPigVariantFromNbt(nbt, world.registryAccess());

            if (variant != null)
            {
                list.add(this.translate(VARIANT_KEY+".pig", variant.location().getPath()));
            }
        }
        else if (entityType.equals(EntityType.RABBIT))
        {
            Rabbit.Variant rabbitType = NbtEntityUtils.getRabbitTypeFromNbt(nbt);

            if (rabbitType != null)
            {
                list.add(this.translate(VARIANT_KEY+".rabbit", rabbitType.getSerializedName()));
            }
        }
        else if (entityType.equals(EntityType.SALMON))
        {
            Salmon.Variant salmonVariant = NbtEntityUtils.getSalmonVariantFromNbt(nbt);

            if (salmonVariant != null)
            {
                list.add(this.translate(VARIANT_KEY+".salmon", salmonVariant.getSerializedName()));
            }
        }
        else if (entityType.equals(EntityType.SHEEP))
        {
            DyeColor color = NbtEntityUtils.getSheepColorFromNbt(nbt);

            if (color != null)
            {
                list.add(this.translate(VARIANT_KEY+".sheep", color.getName()));
            }
        }
        else if (entityType.equals(EntityType.TROPICAL_FISH))
        {
            TropicalFish.Pattern variant = NbtEntityUtils.getFishVariantFromNbt(nbt);

            if (variant != null)
            {
                list.add(this.translate(VARIANT_KEY+".tropical_fish", variant.getSerializedName()));
            }
        }
        else if (entityType.equals(EntityType.WOLF))
        {
            Pair<ResourceKey<WolfVariant>, DyeColor> wolfPair = NbtEntityUtils.getWolfVariantFromNbt(nbt, world.registryAccess());
            ResourceKey<WolfSoundVariant> soundType = NbtEntityUtils.getWolfSoundTypeFromNbt(nbt, world.registryAccess());

            if (wolfPair.getLeft() != null)
            {
                if (soundType != null)
                {
                    list.add(this.translate(VARIANT_KEY + ".wolf.sound_type", wolfPair.getLeft().location().getPath(), soundType.location().getPath(), wolfPair.getRight().getName()));
                }
                else
                {
                    list.add(this.translate(VARIANT_KEY + ".wolf", wolfPair.getLeft().location().getPath(), wolfPair.getRight().getName()));
                }
            }
        }

        return list;
    }

    @Override
    public List<Entry> parseEnt(@Nonnull Level world, @Nonnull Entity ent)
    {
        List<Entry> list = new ArrayList<>();

        switch (ent)
        {
            case Axolotl axolotl -> list.add(this.translate(VARIANT_KEY + ".axolotl", axolotl.getVariant().getName()));
            case Cat cat ->
            {
                ResourceKey<CatVariant> variant = cat.getVariant().unwrapKey().orElse(CatVariants.BLACK);
                list.add(this.translate(VARIANT_KEY + ".cat", variant.location().getPath(), cat.getCollarColor().getName()));
            }
            case Chicken chicken -> list.add(this.translate(VARIANT_KEY + ".chicken", chicken.getVariant().unwrapKey().orElse(ChickenVariants.DEFAULT).location().getPath()));
            case Cow cow -> list.add(this.translate(VARIANT_KEY + ".cow", cow.getVariant().unwrapKey().orElse(CowVariants.DEFAULT).location().getPath()));
            case MushroomCow mooshroom -> list.add(this.translate(VARIANT_KEY + ".mooshroom", mooshroom.getVariant().getSerializedName()));
            case Fox fox -> list.add(this.translate(VARIANT_KEY + ".fox", fox.getVariant().getSerializedName()));
            case Frog frog -> list.add(this.translate(VARIANT_KEY + ".frog", frog.getVariant().unwrapKey().orElse(FrogVariants.TEMPERATE).location().getPath()));
            case Horse horse -> list.add(this.translate(VARIANT_KEY + ".horse", horse.getVariant().getSerializedName(), horse.getMarkings().name().toLowerCase()));
            case Llama llama -> list.add(this.translate(VARIANT_KEY + ".llama", llama.getVariant().getSerializedName(), llama.getStrength()));
            case Painting painting ->
            {
                PaintingVariant paintingVariant = painting.getVariant().value();

                if (paintingVariant != null)
                {
                    Optional<Component> title = paintingVariant.title();
                    Optional<Component> author = paintingVariant.author();

                    if (title.isPresent() && author.isPresent())
                    {
                        list.add(this.translate(VARIANT_KEY + ".painting.both", title.get().getString(), author.get().getString()));
                    }
                    else if (title.isPresent())
                    {
                        list.add(this.translate(VARIANT_KEY + ".painting.title_only", title.get().getString()));
                    }
                    else
                    {
                        author.ifPresent(text -> list.add(this.translate(VARIANT_KEY + ".painting.author_only", text.getString())));
                    }
                }
            }
            case Parrot parrot -> list.add(this.translate(VARIANT_KEY + ".parrot", parrot.getVariant().getSerializedName()));
            case Pig pig -> list.add(this.translate(VARIANT_KEY + ".pig", pig.getVariant().unwrapKey().orElse(PigVariants.DEFAULT).location().getPath()));
            case Rabbit rabbit -> list.add(this.translate(VARIANT_KEY + ".rabbit", rabbit.getVariant().getSerializedName()));
            case Salmon salmon -> list.add(this.translate(VARIANT_KEY + ".salmon", salmon.getVariant().getSerializedName()));
            case Sheep sheep -> list.add(this.translate(VARIANT_KEY + ".sheep", sheep.getColor().getName()));
            case TropicalFish fish -> list.add(this.translate(VARIANT_KEY + ".tropical_fish", fish.getPattern().getSerializedName()));
            case Wolf wolf ->
            {
                Pair<ResourceKey<WolfVariant>, DyeColor> wolfPair = EntityUtils.getWolfVariantFromComponents(wolf);
                ResourceKey<WolfSoundVariant> soundType = EntityUtils.getWolfSoundTypeFromComponents(wolf);

                if (soundType != null)
                {
                    list.add(this.translate(VARIANT_KEY + ".wolf.sound_type", wolfPair.getLeft().location().getPath(), soundType.location().getPath(), wolfPair.getRight().getName()));
                }
                else
                {
                    list.add(this.translate(VARIANT_KEY + ".wolf", wolfPair.getLeft().location().getPath(), wolfPair.getRight().getName()));
                }
            }
            default -> {}
        }

        return list;
    }
}
