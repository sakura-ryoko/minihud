package fi.dy.masa.minihud.info.generic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

public class InfoLineChunkUpdates extends InfoLine
{
    private static final String CHUNKS_KEY = Reference.MOD_ID+".info_line.chunk_updates";

    public InfoLineChunkUpdates(InfoToggle type)
    {
        super(type);
    }

    public InfoLineChunkUpdates()
    {
        this(InfoToggle.CHUNK_UPDATES);
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

        if (mc().levelRenderer.getSectionRenderDispatcher() != null)
        {
            // This should never throw, but just being careful.
            try
            {
                list.add(this.translate(CHUNKS_KEY, Objects.requireNonNull(mc().levelRenderer.getSectionRenderDispatcher()).getToUpload()));
            }
            catch (Exception ignored) { }
        }

        return list;
    }
}
