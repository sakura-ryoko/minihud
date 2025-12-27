package fi.dy.masa.minihud.info.generic;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;
import fi.dy.masa.minihud.util.DataStorage;

public class InfoLineBlockBreakSpeed extends InfoLine
{
    private static final String BLOCK_KEY = Reference.MOD_ID+".info_line.block_break_speed";

    public InfoLineBlockBreakSpeed(InfoToggle type)
    {
        super(type);
    }

    public InfoLineBlockBreakSpeed()
    {
        this(InfoToggle.BLOCK_BREAK_SPEED);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@Nonnull InfoLineContext ctx)
    {
        List<Entry> list = new ArrayList<>();

        if (this.getClientWorld() == null)
        {
            return null;
        }

        list.add(this.translate(BLOCK_KEY, DataStorage.getInstance().getBlockBreakingSpeed()));

        return list;
    }
}
