package fi.dy.masa.minihud.info.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;
import fi.dy.masa.malilib.util.data.DataEntityUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;
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
    public List<Entry> parse(@Nonnull InfoLineContext ctx)
    {
        if (ctx.world() == null) return null;

        if (ctx.hasData())
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
        String zombieType = entityType.getDescription().getString();
        List<Entry> list = new ArrayList<>();
        int conversionTimer = -1;

        if (entityType.equals(EntityType.ZOMBIE_VILLAGER))
        {
            Pair<Integer, UUID> zombieDoctor = DataEntityUtils.getZombieConversionTimer(data);
            conversionTimer = zombieDoctor.getLeft();
        }
        else if (entityType.equals(EntityType.ZOMBIE))
        {
            Pair<Integer, Integer> zombieDoctor = DataEntityUtils.getDrownedConversionTimer(data);
            conversionTimer = zombieDoctor.getLeft();
        }
        else if (entityType.equals(EntityType.SKELETON))
        {
            conversionTimer = DataEntityUtils.getStrayConversionTime(data);
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
    public List<Entry> parseEnt(@Nonnull Level world, @Nonnull Entity ent)
    {
        String zombieType = ent.getType().getDescription().getString();
        List<Entry> list = new ArrayList<>();
        int conversionTimer;

        switch (ent)
        {
            case ZombieVillager zombie ->
                    conversionTimer = ((IMixinZombieVillagerEntity) zombie).minihud_conversionTimer();
            case Zombie zombert ->
                    conversionTimer = ((IMixinZombieEntity) zombert).minihud_ticksUntilWaterConversion();
            case Skeleton skeleton ->
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
