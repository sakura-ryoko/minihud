package fi.dy.masa.minihud.info.generic;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

public class InfoLineFPS extends InfoLine
{
    private static final String FPS_KEY = Reference.MOD_ID+".info_line.fps";

    public InfoLineFPS(InfoToggle type)
    {
        super(type);
    }

    public InfoLineFPS()
    {
        this(InfoToggle.FPS);
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

        list.add(this.translate(FPS_KEY, this.mc().getFps()));

        return list;
    }
}
