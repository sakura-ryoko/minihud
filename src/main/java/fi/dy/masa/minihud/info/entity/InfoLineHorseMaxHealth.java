package fi.dy.masa.minihud.info.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import fi.dy.masa.malilib.util.data.DataEntityUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

public class InfoLineHorseMaxHealth extends InfoLine
{
    private static final String HORSE_KEY = Reference.MOD_ID + ".info_line.horse_max_health";

    public InfoLineHorseMaxHealth(InfoToggle type)
    {
        super(type);
    }

    public InfoLineHorseMaxHealth()
    {
        super(InfoToggle.HORSE_MAX_HEALTH);
    }

    @Override
    public boolean succeededType() { return this.succeeded; }

    @Override
    public List<Entry> parse(@NotNull InfoLineContext ctx)
    {
        if (ctx.world() == null) return null;

        if (this.mc().player != null)
        {
            Entity vehicle = Objects.requireNonNull(this.mc().player).getVehicle();

            if (vehicle instanceof AbstractHorse)
            {
                return this.parseEnt(ctx.world(), vehicle);
            }
        }

        if (ctx.hasLiving() && ctx.hasData())
        {
            EntityType<?> entityType = DataEntityUtils.getEntityType(ctx.data());
            if (entityType == null) return null;

            return this.parseData(ctx.world(), entityType, ctx.data());
        }

        if (ctx.ent() != null)
        {
            return this.parseEnt(ctx.world(), ctx.ent());
        }

        return null;
    }

    @Override
    public List<Entry> parseData(@NotNull Level world, @NotNull EntityType<?> entityType, @NotNull CompoundData data)
    {
        List<Entry> list = new ArrayList<>();
        String horseType = entityType.getDescription().getString();

        if (entityType.equals(EntityType.CAMEL) ||
            entityType.equals(EntityType.DONKEY) ||
            entityType.equals(EntityType.HORSE) ||
            entityType.equals(EntityType.LLAMA) ||
            entityType.equals(EntityType.MULE) ||
            entityType.equals(EntityType.SKELETON_HORSE) ||
            entityType.equals(EntityType.TRADER_LLAMA) ||
            entityType.equals(EntityType.ZOMBIE_HORSE) ||
            entityType.equals(EntityType.CAMEL_HUSK))
        {
            Pair<Double, Double> healthPair = DataEntityUtils.getHealth(data);
            double maxHealth = healthPair.getRight();

            if (maxHealth > 0d)
            {
                list.add(this.translate(HORSE_KEY, horseType, maxHealth));
                this.succeeded = true;
            }
        }

        return list;
    }

    @Override
    public List<Entry> parseEnt(@NotNull Level world, @NotNull Entity ent)
    {
        List<Entry> list = new ArrayList<>();

        if (ent instanceof AbstractHorse horse)
        {
            String horseType = horse.getType().getDescription().getString();
            double maxHealth = horse.getAttributeValue(Attributes.MAX_HEALTH);

            if (maxHealth > 0d)
            {
                list.add(this.translate(HORSE_KEY, horseType, maxHealth));
                this.succeeded = true;
            }
        }

        return list;
    }
}
