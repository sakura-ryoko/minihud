package fi.dy.masa.minihud.info.chunk;

import fi.dy.masa.minihud.config.InfoToggle;

public class InfoLineChunkPos extends InfoLineBlockChunkRegionBase
{
    public InfoLineChunkPos(InfoToggle type)
    {
        super(type);
    }

    public InfoLineChunkPos()
    {
        super(InfoToggle.CHUNK_POS);
    }
}
