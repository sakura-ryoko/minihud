package fi.dy.masa.minihud.info.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

import fi.dy.masa.malilib.util.nbt.NbtEntityUtils;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;

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
    public List<Entry> parse(@NotNull InfoLine.Context ctx)
    {
        if (ctx.world() == null) return null;

        if (this.mc().player != null)
        {
            Entity vehicle = Objects.requireNonNull(this.mc().player).getVehicle();

            if (vehicle instanceof AbstractHorseEntity)
            {
                return this.parseEnt(ctx.world(), vehicle);
            }
        }

        if (ctx.hasLiving() && ctx.hasNbt())
        {
            EntityType<?> entityType = NbtEntityUtils.getEntityTypeFromNbt(ctx.nbt());
            if (entityType == null) return null;

            return this.parseNbt(ctx.world(), entityType, ctx.nbt());
        }

        if (ctx.ent() != null)
        {
            return this.parseEnt(ctx.world(), ctx.ent());
        }

        return null;
    }

    @Override
    public List<Entry> parseNbt(@NotNull World world, @NotNull EntityType<?> entityType, @NotNull NbtCompound nbt)
    {
        List<Entry> list = new ArrayList<>();
        String horseType = entityType.getName().getString();

        if (entityType.equals(EntityType.CAMEL) ||
            entityType.equals(EntityType.DONKEY) ||
            entityType.equals(EntityType.HORSE) ||
            entityType.equals(EntityType.LLAMA) ||
            entityType.equals(EntityType.MULE) ||
            entityType.equals(EntityType.SKELETON_HORSE) ||
            entityType.equals(EntityType.TRADER_LLAMA) ||
            entityType.equals(EntityType.ZOMBIE_HORSE))
//            entityType.equals(EntityType.CAMEL_HUSK))
        {
            Pair<Double, Double> healthPair = NbtEntityUtils.getHealthFromNbt(nbt);
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
    public List<Entry> parseEnt(@NotNull World world, @NotNull Entity ent)
    {
        List<Entry> list = new ArrayList<>();

        if (ent instanceof AbstractHorseEntity horse)
        {
            String horseType = horse.getType().getName().getString();
            double maxHealth = horse.getAttributeValue(EntityAttributes.MAX_HEALTH);

            if (maxHealth > 0d)
            {
                list.add(this.translate(HORSE_KEY, horseType, maxHealth));
                this.succeeded = true;
            }
        }

        return list;
    }
}
