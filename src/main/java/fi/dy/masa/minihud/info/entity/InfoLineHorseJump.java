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

public class InfoLineHorseJump extends InfoLine
{
    private static final String HORSE_KEY = Reference.MOD_ID+".info_line.horse_jump";
    private static final double GRAVITY = 0.08; // blocks/tick² from LivingEntity.GRAVITY

    public InfoLineHorseJump(InfoToggle type)
    {
        super(type);
    }

    public InfoLineHorseJump()
    {
        super(InfoToggle.HORSE_JUMP);
    }

    @Override
    public boolean succeededType() { return this.succeeded; }

    /**
     * Calculates the maximum jump height in blocks from a horse's jump strength
     * attribute.
     * -
     * The calculation is based on:
     * 1. Converting jump strength attribute to jump velocity using a quadratic
     * formula
     * derived from empirical measurements (0.4→1.09375, 0.7→3.09375, 1.0→5.90625
     * blocks)
     * 2. Using physics to calculate maximum height: h = v² / (2g)
     * where g = 0.08 blocks/tick² (Minecraft's gravity constant)
     *
     * @param jumpStrength The jump strength attribute value (typically 0.4 to 1.0)
     * @return The maximum jump height in blocks
     */
    private double calculateJumpHeight(double jumpStrength) {
        // Calculate jump velocity from jump strength attribute
        // Quadratic fit: v = a*jump² + b*jump + c
        // Coefficients: a ≈ -0.09333, b ≈ 1.05367, c ≈ 0.01177
        double jumpVelocity = -0.09333 * jumpStrength * jumpStrength + 1.05367 * jumpStrength + 0.01177;

        // Calculate maximum jump height using physics: h = v² / (2g)
        // Where g = 0.08 blocks/tick²
        return (jumpVelocity * jumpVelocity) / (2.0 * GRAVITY);
    }

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
            double jump = horsePair.getRight();

            if (jump > 0d)
            {
                double jumpHeight = this.calculateJumpHeight(jump);
                list.add(this.translate(HORSE_KEY, horseType, jumpHeight));
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
            double jump = horse.getAttributeValue(Attributes.JUMP_STRENGTH);

            if (jump > 0d)
            {
                double jumpHeight = this.calculateJumpHeight(jump);
                list.add(this.translate(HORSE_KEY, horseType, jumpHeight));
                this.succeeded = true;
            }
        }

        return list;
    }
}
