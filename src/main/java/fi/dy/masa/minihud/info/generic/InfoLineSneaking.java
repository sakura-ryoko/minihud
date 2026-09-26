package fi.dy.masa.minihud.info.generic;

import fi.dy.masa.minihud.config.InfoToggle;

public class InfoLineSneaking extends InfoLineSneakingSprintingBase
{
    public InfoLineSneaking(InfoToggle type)
    {
        super(type);
    }

    public InfoLineSneaking()
    {
        this(InfoToggle.SNEAKING);
    }
}
