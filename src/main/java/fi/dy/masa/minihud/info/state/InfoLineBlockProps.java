package fi.dy.masa.minihud.info.state;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import fi.dy.masa.malilib.util.game.BlockUtils;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

public class InfoLineBlockProps extends InfoLine
{
    private static final String BLOCK_KEY = Reference.MOD_ID+".info_line.block_props";

    public InfoLineBlockProps(InfoToggle type)
    {
        super(type);
    }

    public InfoLineBlockProps()
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
        Identifier rl = BuiltInRegistries.BLOCK.getKey(state.getBlock());

        list.add(this.of(rl != null ? rl.toString() : "<null>"));

        for (String line : BlockUtils.getFormattedBlockStateProperties(state))
        {
            list.add(this.of(line));
        }

        return list;
    }
}
