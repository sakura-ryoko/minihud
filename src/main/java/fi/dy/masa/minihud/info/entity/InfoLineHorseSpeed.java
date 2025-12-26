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

public class InfoLineHorseSpeed extends InfoLine
{
    private static final String HORSE_KEY = Reference.MOD_ID+".info_line.horse_speed";

    // Linear conversion: max_speed = HORSE_SPEED_CONVERSION_FACTOR * base_speed + HORSE_SPEED_CONVERSION_OFFSET
    // Calculated from data points (best linear fit):
    //   baseSpeed = 0.1125 → maxSpeed = 4.85682890 m/s
    //   baseSpeed = 0.225  → maxSpeed = 9.71365773 m/s
    //   baseSpeed = 0.3375 → maxSpeed = 14.57048738 m/s
    private static final double HORSE_SPEED_CONVERSION_FACTOR = 43.171815466666658;
    private static final double HORSE_SPEED_CONVERSION_OFFSET = -0.000000339999999;

    public InfoLineHorseSpeed(InfoToggle type)
    {
        super(type);
    }

    public InfoLineHorseSpeed()
    {
        super(InfoToggle.HORSE_SPEED);
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
            entityType.equals(EntityType.CAMEL_HUSK) ||
            entityType.equals(EntityType.DONKEY) ||
            entityType.equals(EntityType.HORSE) ||
            entityType.equals(EntityType.LLAMA) ||
            entityType.equals(EntityType.MULE) ||
            entityType.equals(EntityType.SKELETON_HORSE) ||
            entityType.equals(EntityType.TRADER_LLAMA) ||
            entityType.equals(EntityType.ZOMBIE_HORSE))
        {
            Pair<Double, Double> horsePair = DataEntityUtils.getSpeedAndJumpStrength(data);
            double speed = horsePair.getLeft();

            if (speed > 0d)
            {
                speed = speed * HORSE_SPEED_CONVERSION_FACTOR + HORSE_SPEED_CONVERSION_OFFSET;
                list.add(this.translate(HORSE_KEY, horseType, speed));
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
            double speed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED);

            if (speed > 0d)
            {
                speed = speed * HORSE_SPEED_CONVERSION_FACTOR + HORSE_SPEED_CONVERSION_OFFSET;
                list.add(this.translate(HORSE_KEY, horseType, speed));
                this.succeeded = true;
            }
        }

        return list;
    }
}
