package fi.dy.masa.minihud.info.entity;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import fi.dy.masa.malilib.util.data.DataEntityUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;

public class InfoLineHomePos extends InfoLine
{
    private static final String HOME_KEY = Reference.MOD_ID+".info_line.home_pos";

    public InfoLineHomePos(InfoToggle type)
    {
        super(type);
    }

    public InfoLineHomePos()
    {
        super(InfoToggle.ENTITY_HOME_POS);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@NotNull InfoLine.Context ctx)
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
    public List<Entry> parseData(@NotNull World world, @NotNull EntityType<?> entityType, @NotNull CompoundData data)
    {
        List<Entry> list = new ArrayList<>();
        Pair<BlockPos, Integer> pair = DataEntityUtils.getHomePos(data);

        if (pair.getLeft() != BlockPos.ORIGIN && pair.getRight() != -1)
        {
            list.add(this.translate(HOME_KEY,
                                    pair.getLeft().toShortString(),
                                    pair.getRight()
            ));
        }

        return list;
    }

    @Override
    public List<Entry> parseEnt(@NotNull World world, @NotNull Entity ent)
    {
        List<Entry> list = new ArrayList<>();

        if (ent instanceof MobEntity mob && mob.hasPositionTarget())
        {
            list.add(this.translate(HOME_KEY,
                                    mob.getPositionTarget().toShortString(),
                                    mob.getPositionTargetRange()
            ));
        }

        return list;
    }
}
