package fi.dy.masa.minihud.info.te;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ComparatorBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import fi.dy.masa.malilib.util.nbt.NbtBlockUtils;
import fi.dy.masa.minihud.Reference;
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
    public List<Entry> parseNbt(@Nonnull World world, @Nonnull BlockEntityType<?> beType, @Nonnull NbtCompound nbt)
    {
        List<Entry> list = new ArrayList<>();

        if (beType.equals(BlockEntityType.COMPARATOR))
        {
            int output = NbtBlockUtils.getOutputSignalFromNbt(nbt);

            if (output > 0)
            {
                list.add(this.translate(COMPARATOR_KEY, output));
            }
        }

        return list;
    }

    @Override
    public List<Entry> parseBlockEnt(@Nonnull World world, @Nonnull BlockEntity be)
    {
        List<Entry> list = new ArrayList<>();

        if (be instanceof ComparatorBlockEntity cbe)
        {
            if (cbe.getOutputSignal() > 0)
            {
                list.add(this.translate(COMPARATOR_KEY, cbe.getOutputSignal()));
            }
        }

        return list;
    }
}
