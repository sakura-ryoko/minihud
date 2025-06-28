package fi.dy.masa.minihud.info.generic;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;

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
    public List<Entry> parse(@Nonnull Context ctx)
    {
        List<Entry> list = new ArrayList<>();

        if (this.getClientWorld() == null)
        {
            return null;
        }

        list.add(this.of(mc().worldRenderer.getChunksDebugString()));

        return list;
    }
}
