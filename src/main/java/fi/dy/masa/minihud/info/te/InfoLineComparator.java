package fi.dy.masa.minihud.info.te;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ComparatorBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

import fi.dy.masa.malilib.util.nbt.NbtBlockUtils;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;

public class InfoLineComparator extends InfoLine
{
    private static final String COMPARATOR_KEY = Reference.MOD_ID+".info_line.comparator_output_signal";

    public InfoLineComparator(InfoToggle type)
    {
        super(type);
    }

    public InfoLineComparator()
    {
        this(InfoToggle.COMPARATOR_OUTPUT);
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
        if (beType.equals(BlockEntityType.COMPARATOR))
        {
            int output = NbtBlockUtils.getOutputSignalFromNbt(nbt);

            if (output > 0)
            {
                return this.translate(COMPARATOR_KEY, output);
            }
        }

        return null;
    }

    @Override
    public @Nullable Entry parseBlockEnt(@Nonnull World world, @Nonnull BlockEntity be)
    {
        if (be instanceof ComparatorBlockEntity cbe)
        {
            if (cbe.getOutputSignal() > 0)
            {
                return this.translate(COMPARATOR_KEY, cbe.getOutputSignal());
            }
        }

        return null;
    }
}
