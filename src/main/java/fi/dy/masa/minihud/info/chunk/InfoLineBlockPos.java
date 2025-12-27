package fi.dy.masa.minihud.info.chunk;

import fi.dy.masa.minihud.config.InfoToggle;

public class InfoLineBlockPos extends InfoLineBlockChunkRegionBase
{
    public InfoLineBlockPos(InfoToggle type)
    {
        super(type);
    }

    public InfoLineBlockPos()
    {
        super(InfoToggle.BLOCK_POS);
    }
}
