package fi.dy.masa.minihud.info.te;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import fi.dy.masa.malilib.util.data.DataBlockUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

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

        if (beType.equals(BlockEntityType.COMPARATOR))
        {
            int output = DataBlockUtils.getOutputSignal(data);

            if (output > 0)
            {
                list.add(this.translate(COMPARATOR_KEY, output));
            }
        }

        return list;
    }

    @Override
    public List<Entry> parseBlockEnt(@Nonnull Level world, @Nonnull BlockEntity be)
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
