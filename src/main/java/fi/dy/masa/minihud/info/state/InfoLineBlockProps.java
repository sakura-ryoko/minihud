package fi.dy.masa.minihud.info.state;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import fi.dy.masa.malilib.util.game.BlockUtils;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;

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
    public List<Entry> parse(@Nonnull Context ctx)
    {
        if (ctx.hasBlockState() && ctx.state() != null)
        {
            return this.parseBlockState(ctx.world(), ctx.state());
        }

        return null;
    }

    @Override
    public List<Entry> parseBlockState(@Nonnull World world, @Nonnull BlockState state)
    {
        List<Entry> list = new ArrayList<>();
        Identifier rl = Registries.BLOCK.getId(state.getBlock());

        list.add(this.of(rl != null ? rl.toString() : "<null>"));

        for (String line : BlockUtils.getFormattedBlockStateProperties(state))
        {
            list.add(this.of(line));
        }

        return list;
    }
}
