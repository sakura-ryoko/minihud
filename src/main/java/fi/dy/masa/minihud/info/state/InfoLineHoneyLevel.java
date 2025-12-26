package fi.dy.masa.minihud.info.state;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

public class InfoLineHoneyLevel extends InfoLine
{
    private static final String HONEY_KEY = Reference.MOD_ID+".info_line.honey_level";

    public InfoLineHoneyLevel(InfoToggle type)
    {
        super(type);
    }

    public InfoLineHoneyLevel()
    {
        this(InfoToggle.HONEY_LEVEL);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@Nonnull InfoLineContext ctx)
    {
        if (ctx.world() == null) return null;

        if (ctx.hasBlockState() && ctx.state() != null)
        {
            return this.parseBlockState(ctx.world(), ctx.state());
        }

        return null;
    }

    @Override
    public List<Entry> parseBlockState(@Nonnull Level world, @Nonnull BlockState state)
    {
        List<Entry> list = new ArrayList<>();

        if (state.getBlock() instanceof BeehiveBlock)
        {
            list.add(this.translate(HONEY_KEY, BeehiveBlockEntity.getHoneyLevel(state)));
        }

        return list;
    }
}
