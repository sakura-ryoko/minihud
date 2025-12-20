package fi.dy.masa.minihud.info.chunk;

import fi.dy.masa.minihud.config.InfoToggle;

public class InfoLineRegionFile extends InfoLineBlockChunkRegionBase
{
    public InfoLineRegionFile(InfoToggle type)
    {
        super(type);
    }

    public InfoLineRegionFile()
    {
        super(InfoToggle.REGION_FILE);
    }
}
