package fi.dy.masa.minihud.info.te;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import fi.dy.masa.malilib.util.nbt.NbtBlockUtils;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.Configs;
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
    public @Nullable Entry parse(@Nonnull Context ctx)
    {
        if (Configs.Generic.INFO_LINES_USES_NBT.getBooleanValue() &&
            ctx.hasNbt())
        {
            BlockEntityType<?> beType = NbtBlockUtils.getBlockEntityTypeFromNbt(ctx.nbt());

            if (beType == null) return null;

            return this.parseNbt(ctx.world(), beType, ctx.nbt());
        }

        return ctx.be() != null ? this.parseBlockEnt(ctx.world(), ctx.be()) : null;
    }

    @Override
    public @Nullable Entry parseNbt(@Nonnull World world, @Nonnull BlockEntityType<?> beType, @Nonnull NbtCompound nbt)
    {
        if (beType.equals(BlockEntityType.BEEHIVE))
        {
            Pair<List<BeehiveBlockEntity.BeeData>, BlockPos> bees = NbtBlockUtils.getBeesDataFromNbt(nbt);

            // This probably means no Server Data, so don't show the flower_pos
            if (bees.getRight().equals(BlockPos.ORIGIN))
            {
                return this.translate(BEES_KEY, bees.getLeft().size());
            }
            else
            {
                return this.translate(BEES_KEY+".flower_pos", bees.getLeft().size(), bees.getRight().toShortString());
            }
        }

        return null;
    }

    @Override
    public @Nullable Entry parseBlockEnt(@Nonnull World world, @Nonnull BlockEntity be)
    {
        if (be instanceof BeehiveBlockEntity bbe)
        {
            return this.translate(BEES_KEY, bbe.getBeeCount());
        }

        return null;
    }
}
