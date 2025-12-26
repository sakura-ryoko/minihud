package fi.dy.masa.minihud.info.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.chicken.ChickenVariant;
import net.minecraft.world.entity.animal.chicken.ChickenVariants;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.CowVariant;
import net.minecraft.world.entity.animal.cow.CowVariants;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.equine.Markings;
import net.minecraft.world.entity.animal.equine.Variant;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.CatVariant;
import net.minecraft.world.entity.animal.feline.CatVariants;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.frog.FrogVariant;
import net.minecraft.world.entity.animal.frog.FrogVariants;
import net.minecraft.world.entity.animal.nautilus.ZombieNautilus;
import net.minecraft.world.entity.animal.nautilus.ZombieNautilusVariant;
import net.minecraft.world.entity.animal.nautilus.ZombieNautilusVariants;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.pig.PigVariant;
import net.minecraft.world.entity.animal.pig.PigVariants;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant;
import net.minecraft.world.entity.animal.wolf.WolfVariant;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.data.DataEntityUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

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
    public List<Entry> parse(@Nonnull InfoLineContext ctx)
    {
        if (ctx.world() == null) return null;

        if (ctx.hasLiving() && ctx.hasData())
        {
            EntityType<?> entityType = DataEntityUtils.getEntityType(ctx.data());
            if (entityType == null) return null;

            return this.parseData(ctx.world(), entityType, ctx.data());
        }

        return ctx.ent() != null ? this.parseEnt(ctx.world(), ctx.ent()) : null;
    }

    @Override
    public List<Entry> parseData(@Nonnull Level world, @Nonnull EntityType<?> entityType, @Nonnull CompoundData data)
    {
        List<Entry> list = new ArrayList<>();

        if (entityType.equals(EntityType.AXOLOTL))
        {
            Axolotl.Variant variant = DataEntityUtils.getAxolotlVariant(data);

            if (variant != null)
            {
                list.add(this.translate(VARIANT_KEY+".axolotl", variant.getName()));
            }
        }
        else if (entityType.equals(EntityType.CAT))
        {
            Pair<ResourceKey<CatVariant>, DyeColor> catPair = DataEntityUtils.getCatVariant(data, world.registryAccess());

            if (catPair.getLeft() != null)
            {
                list.add(this.translate(VARIANT_KEY+".cat", catPair.getLeft().identifier().getPath(), catPair.getRight().getName()));
            }
        }
        else if (entityType.equals(EntityType.COW))
        {
            ResourceKey<CowVariant> variant = DataEntityUtils.getCowVariant(data, world.registryAccess());

            if (variant != null)
            {
                list.add(this.translate(VARIANT_KEY+".cow", variant.identifier().getPath()));
            }
        }
        else if (entityType.equals(EntityType.CHICKEN))
        {
            ResourceKey<ChickenVariant> variant = DataEntityUtils.getChickenVariant(data, world.registryAccess());

            if (variant != null)
            {
                list.add(this.translate(VARIANT_KEY+".chicken", variant.identifier().getPath()));
            }
        }
        else if (entityType.equals(EntityType.MOOSHROOM))
        {
            MushroomCow.Variant mooType = DataEntityUtils.getMooshroomVariant(data);

            if (mooType != null)
            {
                list.add(this.translate(VARIANT_KEY + ".mooshroom", mooType.getSerializedName()));
            }
        }
        else if (entityType.equals(EntityType.FOX))
        {
            Fox.Variant foxType = DataEntityUtils.getFoxVariant(data);

            if (foxType != null)
            {
                list.add(this.translate(VARIANT_KEY+".fox", foxType.getSerializedName()));
            }
        }
        else if (entityType.equals(EntityType.FROG))
        {
            ResourceKey<FrogVariant> variant = DataEntityUtils.getFrogVariant(data, world.registryAccess());

            if (variant != null)
            {
                list.add(this.translate(VARIANT_KEY+".frog", variant.identifier().getPath()));
            }
        }
        else if (entityType.equals(EntityType.HORSE))
        {
            Pair<Variant, Markings> horsePair = DataEntityUtils.getHorseVariant(data);

            if (horsePair.getLeft() != null)
            {
                list.add(this.translate(VARIANT_KEY+".horse", horsePair.getLeft().getSerializedName(), horsePair.getRight().name().toLowerCase()));
            }
        }
        else if (entityType.equals(EntityType.LLAMA) || entityType.equals(EntityType.TRADER_LLAMA))
        {
            Pair<Llama.Variant, Integer> llamaPair = DataEntityUtils.getLlamaType(data);

            if (llamaPair.getLeft() != null)
            {
                list.add(this.translate(VARIANT_KEY+".llama", llamaPair.getLeft().getSerializedName(), llamaPair.getRight()));
            }
        }
        else if (entityType.equals(EntityType.PAINTING))
        {
            Pair<Direction, PaintingVariant> paintingPair = DataEntityUtils.getPaintingData(data, world.registryAccess());

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
            Parrot.Variant variant = DataEntityUtils.getParrotVariant(data);

            if (variant != null)
            {
                list.add(this.translate(VARIANT_KEY+".parrot", variant.getSerializedName()));
            }
        }
        else if (entityType.equals(EntityType.PIG))
        {
            ResourceKey<PigVariant> variant = DataEntityUtils.getPigVariant(data, world.registryAccess());

            if (variant != null)
            {
                list.add(this.translate(VARIANT_KEY+".pig", variant.identifier().getPath()));
            }
        }
        else if (entityType.equals(EntityType.RABBIT))
        {
            Rabbit.Variant rabbitType = DataEntityUtils.getRabbitType(data);

            if (rabbitType != null)
            {
                list.add(this.translate(VARIANT_KEY+".rabbit", rabbitType.getSerializedName()));
            }
        }
        else if (entityType.equals(EntityType.SALMON))
        {
            Salmon.Variant salmonVariant = DataEntityUtils.getSalmonVariant(data);

            if (salmonVariant != null)
            {
                list.add(this.translate(VARIANT_KEY+".salmon", salmonVariant.getSerializedName()));
            }
        }
        else if (entityType.equals(EntityType.SHEEP))
        {
            DyeColor color = DataEntityUtils.getSheepColor(data);

            if (color != null)
            {
                list.add(this.translate(VARIANT_KEY+".sheep", color.getName()));
            }
        }
        else if (entityType.equals(EntityType.TROPICAL_FISH))
        {
            TropicalFish.Variant variant = DataEntityUtils.getFishVariant(data);

            if (variant != null)
            {
                list.add(this.translate(VARIANT_KEY+".tropical_fish", variant.pattern().getSerializedName(), variant.baseColor().getSerializedName(), variant.patternColor().getSerializedName()));
            }
        }
        else if (entityType.equals(EntityType.ZOMBIE_NAUTILUS))
        {
            ResourceKey<@NotNull ZombieNautilusVariant> variant = DataEntityUtils.getZombieNautilusVariantFromNbt(data, world.registryAccess());

            if (variant != null)
            {
                list.add(this.translate(VARIANT_KEY+".nautilus", variant.identifier().getPath()));
            }
        }
        else if (entityType.equals(EntityType.WOLF))
        {
            Pair<ResourceKey<WolfVariant>, DyeColor> wolfPair = DataEntityUtils.getWolfVariant(data, world.registryAccess());
            ResourceKey<WolfSoundVariant> soundType = DataEntityUtils.getWolfSoundType(data, world.registryAccess());

            if (wolfPair.getLeft() != null)
            {
                if (soundType != null)
                {
                    list.add(this.translate(VARIANT_KEY + ".wolf.sound_type", wolfPair.getLeft().identifier().getPath(), soundType.identifier().getPath(), wolfPair.getRight().getName()));
                }
                else
                {
                    list.add(this.translate(VARIANT_KEY + ".wolf", wolfPair.getLeft().identifier().getPath(), wolfPair.getRight().getName()));
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
                list.add(this.translate(VARIANT_KEY + ".cat", variant.identifier().getPath(), cat.getCollarColor().getName()));
            }
            case Chicken chicken -> list.add(this.translate(VARIANT_KEY + ".chicken", chicken.getVariant().unwrapKey().orElse(ChickenVariants.DEFAULT).identifier().getPath()));
            case Cow cow -> list.add(this.translate(VARIANT_KEY + ".cow", cow.getVariant().unwrapKey().orElse(CowVariants.DEFAULT).identifier().getPath()));
            case MushroomCow mooshroom -> list.add(this.translate(VARIANT_KEY + ".mooshroom", mooshroom.getVariant().getSerializedName()));
            case Fox fox -> list.add(this.translate(VARIANT_KEY + ".fox", fox.getVariant().getSerializedName()));
            case Frog frog -> list.add(this.translate(VARIANT_KEY + ".frog", frog.getVariant().unwrapKey().orElse(FrogVariants.TEMPERATE).identifier().getPath()));
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
            case Pig pig -> list.add(this.translate(VARIANT_KEY + ".pig", pig.getVariant().unwrapKey().orElse(PigVariants.DEFAULT).identifier().getPath()));
            case Rabbit rabbit -> list.add(this.translate(VARIANT_KEY + ".rabbit", rabbit.getVariant().getSerializedName()));
            case Salmon salmon -> list.add(this.translate(VARIANT_KEY + ".salmon", salmon.getVariant().getSerializedName()));
            case Sheep sheep -> list.add(this.translate(VARIANT_KEY + ".sheep", sheep.getColor().getName()));
            case TropicalFish fish -> list.add(this.translate(VARIANT_KEY + ".tropical_fish", fish.getPattern().getSerializedName()));
            case ZombieNautilus nautilus -> list.add(this.translate(VARIANT_KEY + ".nautilus", nautilus.getVariant().unwrapKey().orElse(ZombieNautilusVariants.DEFAULT).identifier().getPath()));
            case Wolf wolf ->
            {
                Pair<ResourceKey<WolfVariant>, DyeColor> wolfPair = EntityUtils.getWolfVariantFromComponents(wolf);
                ResourceKey<WolfSoundVariant> soundType = EntityUtils.getWolfSoundTypeFromComponents(wolf);

                if (soundType != null)
                {
                    list.add(this.translate(VARIANT_KEY + ".wolf.sound_type", wolfPair.getLeft().identifier().getPath(), soundType.identifier().getPath(), wolfPair.getRight().getName()));
                }
                else
                {
                    list.add(this.translate(VARIANT_KEY + ".wolf", wolfPair.getLeft().identifier().getPath(), wolfPair.getRight().getName()));
                }
            }
            default -> {}
        }

        return list;
    }
}
