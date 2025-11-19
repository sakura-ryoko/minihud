package fi.dy.masa.minihud.info.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.entity.passive.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.data.DataEntityUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
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

        if (ctx.hasLiving() && ctx.hasData())
        {
            EntityType<?> entityType = DataEntityUtils.getEntityType(ctx.data());
            if (entityType == null) return null;

            return this.parseData(ctx.world(), entityType, ctx.data());
        }

        return ctx.ent() != null ? this.parseEnt(ctx.world(), ctx.ent()) : null;
    }

    @Override
    public List<Entry> parseData(@Nonnull World world, @Nonnull EntityType<?> entityType, @Nonnull CompoundData data)
    {
        List<Entry> list = new ArrayList<>();

        if (entityType.equals(EntityType.AXOLOTL))
        {
            AxolotlEntity.Variant variant = DataEntityUtils.getAxolotlVariant(data);

            if (variant != null)
            {
                list.add(this.translate(VARIANT_KEY+".axolotl", variant.getId()));
            }
        }
        else if (entityType.equals(EntityType.CAT))
        {
            Pair<RegistryKey<CatVariant>, DyeColor> catPair = DataEntityUtils.getCatVariant(data, world.getRegistryManager());

            if (catPair.getLeft() != null)
            {
                list.add(this.translate(VARIANT_KEY+".cat", catPair.getLeft().getValue().getPath(), catPair.getRight().getId()));
            }
        }
        else if (entityType.equals(EntityType.COW))
        {
            RegistryKey<CowVariant> variant = DataEntityUtils.getCowVariant(data, world.getRegistryManager());

            if (variant != null)
            {
                list.add(this.translate(VARIANT_KEY+".cow", variant.getValue().getPath()));
            }
        }
        else if (entityType.equals(EntityType.CHICKEN))
        {
            RegistryKey<ChickenVariant> variant = DataEntityUtils.getChickenVariant(data, world.getRegistryManager());

            if (variant != null)
            {
                list.add(this.translate(VARIANT_KEY+".chicken", variant.getValue().getPath()));
            }
        }
        else if (entityType.equals(EntityType.MOOSHROOM))
        {
            MooshroomEntity.Variant mooType = DataEntityUtils.getMooshroomVariant(data);

            if (mooType != null)
            {
                list.add(this.translate(VARIANT_KEY + ".mooshroom", mooType.asString()));
            }
        }
        else if (entityType.equals(EntityType.FOX))
        {
            FoxEntity.Variant foxType = DataEntityUtils.getFoxVariant(data);

            if (foxType != null)
            {
                list.add(this.translate(VARIANT_KEY+".fox", foxType.asString()));
            }
        }
        else if (entityType.equals(EntityType.FROG))
        {
            RegistryKey<FrogVariant> variant = DataEntityUtils.getFrogVariant(data, world.getRegistryManager());

            if (variant != null)
            {
                list.add(this.translate(VARIANT_KEY+".frog", variant.getValue().getPath()));
            }
        }
        else if (entityType.equals(EntityType.HORSE))
        {
            Pair<HorseColor, HorseMarking> horsePair = DataEntityUtils.getHorseVariant(data);

            if (horsePair.getLeft() != null)
            {
                list.add(this.translate(VARIANT_KEY+".horse", horsePair.getLeft().asString(), horsePair.getRight().name().toLowerCase()));
            }
        }
        else if (entityType.equals(EntityType.LLAMA) || entityType.equals(EntityType.TRADER_LLAMA))
        {
            Pair<LlamaEntity.Variant, Integer> llamaPair = DataEntityUtils.getLlamaType(data);

            if (llamaPair.getLeft() != null)
            {
                list.add(this.translate(VARIANT_KEY+".llama", llamaPair.getLeft().asString(), llamaPair.getRight()));
            }
        }
        else if (entityType.equals(EntityType.PAINTING))
        {
            Pair<Direction, PaintingVariant> paintingPair = DataEntityUtils.getPaintingData(data, world.getRegistryManager());

            if (paintingPair.getRight() != null)
            {
                Optional<net.minecraft.text.Text> title = paintingPair.getRight().title();
                Optional<net.minecraft.text.Text> author = paintingPair.getRight().author();

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
            ParrotEntity.Variant variant = DataEntityUtils.getParrotVariant(data);

            if (variant != null)
            {
                list.add(this.translate(VARIANT_KEY+".parrot", variant.asString()));
            }
        }
        else if (entityType.equals(EntityType.PIG))
        {
            RegistryKey<PigVariant> variant = DataEntityUtils.getPigVariant(data, world.getRegistryManager());

            if (variant != null)
            {
                list.add(this.translate(VARIANT_KEY+".pig", variant.getValue().getPath()));
            }
        }
        else if (entityType.equals(EntityType.RABBIT))
        {
            RabbitEntity.Variant rabbitType = DataEntityUtils.getRabbitType(data);

            if (rabbitType != null)
            {
                list.add(this.translate(VARIANT_KEY+".rabbit", rabbitType.asString()));
            }
        }
        else if (entityType.equals(EntityType.SALMON))
        {
            SalmonEntity.Variant salmonVariant = DataEntityUtils.getSalmonVariant(data);

            if (salmonVariant != null)
            {
                list.add(this.translate(VARIANT_KEY+".salmon", salmonVariant.asString()));
            }
        }
        else if (entityType.equals(EntityType.SHEEP))
        {
            DyeColor color = DataEntityUtils.getSheepColor(data);

            if (color != null)
            {
                list.add(this.translate(VARIANT_KEY+".sheep", color.getId()));
            }
        }
        else if (entityType.equals(EntityType.TROPICAL_FISH))
        {
            TropicalFishEntity.Variant variant = DataEntityUtils.getFishVariant(data);

            if (variant != null)
            {
                list.add(this.translate(VARIANT_KEY+".tropical_fish", variant.pattern().asString(), variant.baseColor().asString(), variant.patternColor().asString()));
            }
        }
        else if (entityType.equals(EntityType.WOLF))
        {
            Pair<RegistryKey<WolfVariant>, DyeColor> wolfPair = DataEntityUtils.getWolfVariant(data, world.getRegistryManager());
            RegistryKey<WolfSoundVariant> soundType = DataEntityUtils.getWolfSoundType(data, world.getRegistryManager());

            if (wolfPair.getLeft() != null)
            {
                if (soundType != null)
                {
                    list.add(this.translate(VARIANT_KEY + ".wolf.sound_type", wolfPair.getLeft().getValue().getPath(), soundType.getValue().getPath(), wolfPair.getRight().getId()));
                }
                else
                {
                    list.add(this.translate(VARIANT_KEY + ".wolf", wolfPair.getLeft().getValue().getPath(), wolfPair.getRight().getId()));
                }
            }
        }

        return list;
    }

    @Override
    public List<Entry> parseEnt(@Nonnull World world, @Nonnull Entity ent)
    {
        List<Entry> list = new ArrayList<>();

        switch (ent)
        {
            case AxolotlEntity axolotl -> list.add(this.translate(VARIANT_KEY + ".axolotl", axolotl.getVariant().getId()));
            case CatEntity cat ->
            {
                RegistryKey<CatVariant> variant = cat.getVariant().getKey().orElse(CatVariants.BLACK);
                list.add(this.translate(VARIANT_KEY + ".cat", variant.getValue().getPath(), cat.getCollarColor().getId()));
            }
            case ChickenEntity chicken -> list.add(this.translate(VARIANT_KEY + ".chicken", chicken.getVariant().getKey().orElse(ChickenVariants.DEFAULT).getValue().getPath()));
            case CowEntity cow -> list.add(this.translate(VARIANT_KEY + ".cow", cow.getVariant().getKey().orElse(CowVariants.DEFAULT).getValue().getPath()));
            case MooshroomEntity mooshroom -> list.add(this.translate(VARIANT_KEY + ".mooshroom", mooshroom.getVariant().asString()));
            case FoxEntity fox -> list.add(this.translate(VARIANT_KEY + ".fox", fox.getVariant().asString()));
            case FrogEntity frog -> list.add(this.translate(VARIANT_KEY + ".frog", frog.getVariant().getKey().orElse(FrogVariants.TEMPERATE).getValue().getPath()));
            case HorseEntity horse -> list.add(this.translate(VARIANT_KEY + ".horse", horse.getHorseColor().asString(), horse.getMarking().name().toLowerCase()));
            case LlamaEntity llama -> list.add(this.translate(VARIANT_KEY + ".llama", llama.getVariant().asString(), llama.getStrength()));
            case PaintingEntity painting ->
            {
                PaintingVariant paintingVariant = painting.getVariant().value();

                if (paintingVariant != null)
                {
                    Optional<Text> title = paintingVariant.title();
                    Optional<Text> author = paintingVariant.author();

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
            case ParrotEntity parrot -> list.add(this.translate(VARIANT_KEY + ".parrot", parrot.getVariant().asString()));
            case PigEntity pig -> list.add(this.translate(VARIANT_KEY + ".pig", pig.getVariant().getKey().orElse(PigVariants.DEFAULT).getValue().getPath()));
            case RabbitEntity rabbit -> list.add(this.translate(VARIANT_KEY + ".rabbit", rabbit.getVariant().asString()));
            case SalmonEntity salmon -> list.add(this.translate(VARIANT_KEY + ".salmon", salmon.getVariant().asString()));
            case SheepEntity sheep -> list.add(this.translate(VARIANT_KEY + ".sheep", sheep.getColor().getId()));
            case TropicalFishEntity fish -> list.add(this.translate(VARIANT_KEY + ".tropical_fish", fish.getVariety().asString()));
            case WolfEntity wolf ->
            {
                Pair<RegistryKey<WolfVariant>, DyeColor> wolfPair = EntityUtils.getWolfVariantFromComponents(wolf);
                RegistryKey<WolfSoundVariant> soundType = EntityUtils.getWolfSoundTypeFromComponents(wolf);

                if (soundType != null)
                {
                    list.add(this.translate(VARIANT_KEY + ".wolf.sound_type", wolfPair.getLeft().getValue().getPath(), soundType.getValue().getPath(), wolfPair.getRight().getId()));
                }
                else
                {
                    list.add(this.translate(VARIANT_KEY + ".wolf", wolfPair.getLeft().getValue().getPath(), wolfPair.getRight().getId()));
                }
            }
            default -> {}
        }

        return list;
    }
}
