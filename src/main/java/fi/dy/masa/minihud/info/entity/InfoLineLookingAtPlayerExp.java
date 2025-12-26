package fi.dy.masa.minihud.info.entity;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Triple;
import fi.dy.masa.malilib.util.data.DataEntityUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

public class InfoLineLookingAtPlayerExp extends InfoLine
{
    private static final String PLAYER_KEY = Reference.MOD_ID+".info_line.looking_at_player_exp";

    public InfoLineLookingAtPlayerExp(InfoToggle type)
    {
        super(type);
    }

    public InfoLineLookingAtPlayerExp()
    {
        this(InfoToggle.LOOKING_AT_PLAYER_EXP);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@Nonnull InfoLineContext ctx)
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
    public List<Entry> parseData(@Nonnull Level world, @Nonnull EntityType<?> entityType, @Nonnull CompoundData data)
    {
        List<Entry> list = new ArrayList<>();

        if (entityType.equals(EntityType.PLAYER))
        {
            Triple<Integer, Integer, Float> triple = DataEntityUtils.getPlayerExp(data);

            if (triple.getLeft() > 0)
            {
                list.add(this.translate(PLAYER_KEY, triple.getLeft(), triple.getRight(), 100 * triple.getMiddle()));
            }
        }

        return list;
    }

    @Override
    public List<Entry> parseEnt(@Nonnull Level world, @Nonnull Entity ent)
    {
        List<Entry> list = new ArrayList<>();

        if (ent instanceof ServerPlayer player)
        {
            list.add(this.translate(PLAYER_KEY, player.experienceLevel, 100 * player.experienceProgress, player.totalExperience));
        }

        return list;
    }
}
