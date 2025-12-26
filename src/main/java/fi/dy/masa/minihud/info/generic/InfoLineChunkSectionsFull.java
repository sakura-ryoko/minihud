package fi.dy.masa.minihud.info.generic;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

public class InfoLineChunkSectionsFull extends InfoLine
{
    private static final String CHUNKS_KEY = Reference.MOD_ID+".info_line.";

    public InfoLineChunkSectionsFull(InfoToggle type)
    {
        super(type);
    }

    public InfoLineChunkSectionsFull()
    {
        this(InfoToggle.CHUNK_SECTIONS_FULL);
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

	    String str = mc().levelRenderer.getSectionStatistics();

		if (str != null)
		{
			list.add(this.of(str));
		}

        return list;
    }
}
