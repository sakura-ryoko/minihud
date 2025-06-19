package fi.dy.masa.minihud.info.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

import fi.dy.masa.malilib.util.nbt.NbtEntityUtils;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.mixin.entity.IMixinSkeletonEntity;
import fi.dy.masa.minihud.mixin.entity.IMixinZombieEntity;
import fi.dy.masa.minihud.mixin.entity.IMixinZombieVillagerEntity;
import fi.dy.masa.minihud.util.MiscUtils;

public class InfoLineZombieConversion extends InfoLine
{
    private static final String ZOMBIE_KEY = Reference.MOD_ID+".info_line.zombie_conversion";

    public InfoLineZombieConversion(InfoToggle type)
    {
        super(type);
    }

    public InfoLineZombieConversion()
    {
        this(InfoToggle.ZOMBIE_CONVERSION);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@Nonnull Context ctx)
    {
        if (ctx.hasNbt())
        {
            EntityType<?> entityType = NbtEntityUtils.getEntityTypeFromNbt(ctx.nbt());
            if (entityType == null) return null;

            return this.parseNbt(ctx.world(), entityType, ctx.nbt());
        }

        return ctx.ent() != null ? this.parseEnt(ctx.world(), ctx.ent()) : null;
    }

    @Override
    public List<Entry> parseNbt(@Nonnull World world, @Nonnull EntityType<?> entityType, @Nonnull NbtCompound nbt)
    {
        String zombieType = entityType.getName().getString();
        List<Entry> list = new ArrayList<>();
        int conversionTimer = -1;

        if (entityType.equals(EntityType.ZOMBIE_VILLAGER))
        {
            Pair<Integer, UUID> zombieDoctor = NbtEntityUtils.getZombieConversionTimerFromNbt(nbt);
            conversionTimer = zombieDoctor.getLeft();
        }
        else if (entityType.equals(EntityType.ZOMBIE))
        {
            Pair<Integer, Integer> zombieDoctor = NbtEntityUtils.getDrownedConversionTimerFromNbt(nbt);
            conversionTimer = zombieDoctor.getLeft();
        }
        else if (entityType.equals(EntityType.SKELETON))
        {
            conversionTimer = NbtEntityUtils.getStrayConversionTimeFromNbt(nbt);
        }

        if (conversionTimer > 0)
        {
            list.add(this.translate(ZOMBIE_KEY,
                                    zombieType,
                                    MiscUtils.formatDuration((conversionTimer / 20) * 1000L)));
        }

        return list;
    }

    @Override
    public List<Entry> parseEnt(@Nonnull World world, @Nonnull Entity ent)
    {
        String zombieType = ent.getType().getName().getString();
        List<Entry> list = new ArrayList<>();
        int conversionTimer;

        switch (ent)
        {
            case ZombieVillagerEntity zombie ->
                    conversionTimer = ((IMixinZombieVillagerEntity) zombie).minihud_conversionTimer();
            case ZombieEntity zombert ->
                    conversionTimer = ((IMixinZombieEntity) zombert).minihud_ticksUntilWaterConversion();
            case SkeletonEntity skeleton ->
                    conversionTimer = ((IMixinSkeletonEntity) skeleton).minihud_conversionTime();
            default ->
                    conversionTimer = -1;
        }

        if (conversionTimer > 0)
        {
            list.add(this.translate(ZOMBIE_KEY,
                                    zombieType,
                                    MiscUtils.formatDuration((conversionTimer / 20) * 1000L)));
        }

        return list;
    }
}
