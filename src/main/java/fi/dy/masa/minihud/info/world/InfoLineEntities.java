package fi.dy.masa.minihud.info.world;

import fi.dy.masa.minihud.config.InfoToggle;

public class InfoLineEntities extends InfoLineRenderEntitiesBase
{
    public InfoLineEntities(InfoToggle type)
    {
        super(type);
    }

    public InfoLineEntities()
    {
        this(InfoToggle.ENTITIES);
    }
}
