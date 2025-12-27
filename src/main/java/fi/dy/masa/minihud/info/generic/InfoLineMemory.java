package fi.dy.masa.minihud.info.generic;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;
import fi.dy.masa.minihud.util.MiscUtils;

public class InfoLineMemory extends InfoLine
{
    private static final String MEM_KEY = Reference.MOD_ID+".info_line.memory_usage";

    public InfoLineMemory(InfoToggle type)
    {
        super(type);
    }

    public InfoLineMemory()
    {
        this(InfoToggle.MEMORY_USAGE);
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

        long memMax = Runtime.getRuntime().maxMemory();
        long memTotal = Runtime.getRuntime().totalMemory();
        long memFree = Runtime.getRuntime().freeMemory();
        long memUsed = memTotal - memFree;

        list.add(this.translate(MEM_KEY,
                         memUsed * 100L / memMax,
                         MiscUtils.bytesToMb(memUsed),
                         MiscUtils.bytesToMb(memMax),
                         memTotal * 100L / memMax,
                         MiscUtils.bytesToMb(memTotal))
        );

        return list;
    }
}
