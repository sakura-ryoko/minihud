package fi.dy.masa.minihud.info.entity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import fi.dy.masa.malilib.util.data.DataEntityUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;
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
    public List<Entry> parse(@NotNull InfoLineContext ctx)
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
    public List<Entry> parseData(@NotNull Level world, @NotNull EntityType<?> entityType, @NotNull CompoundData data)
    {
        Pair<Integer, Boolean> dolphiPair = DataEntityUtils.getDolphinData(data);
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
    public List<Entry> parseEnt(@NotNull Level world, @NotNull Entity ent)
    {
        List<Entry> list = new ArrayList<>();

        if (ent instanceof Dolphin dolphin)
        {
            int dryTime = dolphin.getMoistnessLevel();

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
