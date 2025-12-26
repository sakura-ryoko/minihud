package fi.dy.masa.minihud.info.te;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.apache.commons.lang3.tuple.Pair;
import fi.dy.masa.malilib.util.data.DataBlockUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

public class InfoLineBeeCount extends InfoLine
{
    private static final String BEES_KEY = Reference.MOD_ID+".info_line.bee_count";

    public InfoLineBeeCount(InfoToggle type)
    {
        super(type);
    }

    public InfoLineBeeCount()
    {
        this(InfoToggle.BEE_COUNT);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@Nonnull InfoLineContext ctx)
    {
        if (ctx.world() == null) return null;

        if (ctx.hasData())
        {
            BlockEntityType<?> beType = DataBlockUtils.getBlockEntityType(ctx.data());

            if (beType == null) return null;

            return this.parseData(ctx.world(), beType, ctx.data());
        }

        return ctx.be() != null ? this.parseBlockEnt(ctx.world(), ctx.be()) : null;
    }

    @Override
    public List<Entry> parseData(@Nonnull Level world, @Nonnull BlockEntityType<?> beType, @Nonnull CompoundData data)
    {
        List<Entry> list = new ArrayList<>();

        if (beType.equals(BlockEntityType.BEEHIVE))
        {
            Pair<List<BeehiveBlockEntity.Occupant>, BlockPos> bees = DataBlockUtils.getBeesData(data);

            // This probably means no Server Data, so don't show the flower_pos
            if (bees.getRight().equals(BlockPos.ZERO))
            {
                list.add(this.translate(BEES_KEY, bees.getLeft().size()));
            }
            else
            {
                list.add(this.translate(BEES_KEY+".flower_pos", bees.getLeft().size(), bees.getRight().toShortString()));
            }
        }

        return list;
    }

    @Override
    public List<Entry> parseBlockEnt(@Nonnull Level world, @Nonnull BlockEntity be)
    {
        List<Entry> list = new ArrayList<>();

        if (be instanceof BeehiveBlockEntity bbe)
        {
            list.add(this.translate(BEES_KEY, bbe.getOccupantCount()));
        }

        return list;
    }
}
