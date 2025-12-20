package fi.dy.masa.minihud.info.world;

import fi.dy.masa.minihud.config.InfoToggle;

public class InfoLineTileEntities extends InfoLineRenderEntitiesBase
{
    public InfoLineTileEntities(InfoToggle type)
    {
        super(type);
    }

    public InfoLineTileEntities()
    {
        this(InfoToggle.TILE_ENTITIES);
    }
}
