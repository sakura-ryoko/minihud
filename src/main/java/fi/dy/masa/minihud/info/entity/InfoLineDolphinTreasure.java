package fi.dy.masa.minihud.info.entity;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.DolphinEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

import fi.dy.masa.malilib.util.nbt.NbtEntityUtils;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.util.MiscUtils;

public class InfoLineDolphinTreasure extends InfoLine
{
    private static final String DOLPHIN_KEY = Reference.MOD_ID+".info_line.dolphin_treasure";

    public InfoLineDolphinTreasure(InfoToggle type)
    {
        super(type);
    }

    public InfoLineDolphinTreasure()
    {
        super(InfoToggle.DOLPHIN_TREASURE);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@NotNull InfoLine.Context ctx)
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
    public List<Entry> parseNbt(@NotNull World world, @NotNull EntityType<?> entityType, @NotNull NbtCompound nbt)
    {
        Pair<Integer, Boolean> dolphiPair = NbtEntityUtils.getDolphinDataFromNbt(nbt);
        List<Entry> list = new ArrayList<>();

        if (dolphiPair != null && entityType.equals(EntityType.DOLPHIN))
        {
            int dryTime = dolphiPair.getLeft();

            /*
            if (dryTime == 2400)
            {
                // Submerged
                if (hasTreasure)
                {
                    this.translate(DOLPHIN_KEY, treasure.toShortString());
                }
            }
            else
             */
            if (dryTime > 0)
            {
                    list.add(this.translate(DOLPHIN_KEY+".drying_no_treasure",
                                            MiscUtils.formatDuration((dryTime / 20) * 1000L)));
            }
            else if (dryTime < 0)
            {
                // Drying Out and taking Damage
                list.add(this.translate(DOLPHIN_KEY+".dying_no_treasure",
                                        MiscUtils.formatDuration(((dryTime * (-1)) / 20) * 1000L)));
            }
        }

        return list;
    }

    @Override
    public List<Entry> parseEnt(@NotNull World world, @NotNull Entity ent)
    {
        List<Entry> list = new ArrayList<>();

        if (ent instanceof DolphinEntity dolphin)
        {
            int dryTime = dolphin.getMoistness();

            /*
            if (dryTime == 2400)
            {
                // Submerged
                if (hasTreasure)
                {
                    list.add(this.translate(DOLPHIN_KEY, treasure.toShortString()));
                }
            }
            else
             */
            if (dryTime > 0)
            {
                list.add(this.translate(DOLPHIN_KEY+".drying_no_treasure",
                                        MiscUtils.formatDuration((dryTime / 20) * 1000L)));
            }
            else if (dryTime < 0)
            {
                // Drying Out and taking Damage
                list.add(this.translate(DOLPHIN_KEY+".dying_no_treasure",
                                        MiscUtils.formatDuration(((dryTime * (-1)) / 20) * 1000L)));
            }
        }

        return list;
    }
}
