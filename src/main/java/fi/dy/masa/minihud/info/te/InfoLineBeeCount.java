package fi.dy.masa.minihud.info.te;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import fi.dy.masa.malilib.util.nbt.NbtBlockUtils;
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

        if (ctx.hasNbt())
        {
            BlockEntityType<?> beType = NbtBlockUtils.getBlockEntityTypeFromNbt(ctx.nbt());

            if (beType == null) return null;

            return this.parseNbt(ctx.world(), beType, ctx.nbt());
        }

        return ctx.be() != null ? this.parseBlockEnt(ctx.world(), ctx.be()) : null;
    }

    @Override
    public List<Entry> parseNbt(@Nonnull World world, @Nonnull BlockEntityType<?> beType, @Nonnull NbtCompound nbt)
    {
        List<Entry> list = new ArrayList<>();

        if (beType.equals(BlockEntityType.BEEHIVE))
        {
            Pair<List<BeehiveBlockEntity.BeeData>, BlockPos> bees = NbtBlockUtils.getBeesDataFromNbt(nbt);

            // This probably means no Server Data, so don't show the flower_pos
            if (bees.getRight().equals(BlockPos.ORIGIN))
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
    public List<Entry> parseBlockEnt(@Nonnull World world, @Nonnull BlockEntity be)
    {
        List<Entry> list = new ArrayList<>();

        if (be instanceof BeehiveBlockEntity bbe)
        {
            list.add(this.translate(BEES_KEY, bbe.getBeeCount()));
        }

        return list;
    }
}
