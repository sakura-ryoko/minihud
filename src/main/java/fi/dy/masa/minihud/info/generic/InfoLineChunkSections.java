package fi.dy.masa.minihud.info.generic;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;
import fi.dy.masa.minihud.mixin.render.IMixinWorldRenderer;

public class InfoLineChunkSections extends InfoLine
{
    private static final String CHUNKS_KEY = Reference.MOD_ID+".info_line.chunk_sections";

    public InfoLineChunkSections(InfoToggle type)
    {
        super(type);
    }

    public InfoLineChunkSections()
    {
        this(InfoToggle.CHUNK_SECTIONS);
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

        list.add(this.translate(CHUNKS_KEY, ((IMixinWorldRenderer) mc().levelRenderer).minihud_getRenderedChunksInvoker()));

        return list;
    }
}
