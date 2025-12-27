package fi.dy.masa.minihud.info.generic;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;
import fi.dy.masa.minihud.util.MiscUtils;

public class InfoLineTimeIRL extends InfoLine
{
    private static final String TIME_KEY = Reference.MOD_ID+".info_line.time_world";

    public InfoLineTimeIRL(InfoToggle type)
    {
        super(type);
    }

    public InfoLineTimeIRL()
    {
        this(InfoToggle.TIME_REAL);
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

        list.add(this.of(MiscUtils.formatDateNow()));

        return list;
    }
}
