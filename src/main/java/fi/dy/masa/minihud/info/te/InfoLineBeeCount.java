package fi.dy.masa.minihud.info.te;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.apache.commons.lang3.tuple.Pair;
import fi.dy.masa.malilib.util.nbt.NbtBlockUtils;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;

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
    public List<Entry> parse(@Nonnull Context ctx)
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
    public List<Entry> parseNbt(@Nonnull Level world, @Nonnull BlockEntityType<?> beType, @Nonnull CompoundTag nbt)
    {
        List<Entry> list = new ArrayList<>();

        if (beType.equals(BlockEntityType.BEEHIVE))
        {
            Pair<List<BeehiveBlockEntity.Occupant>, BlockPos> bees = NbtBlockUtils.getBeesDataFromNbt(nbt);

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
