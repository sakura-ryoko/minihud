package fi.dy.masa.minihud.info.generic;

import fi.dy.masa.minihud.config.InfoToggle;

public class InfoLineSprinting extends InfoLineSneakingSprintingBase
{
    public InfoLineSprinting(InfoToggle type)
    {
        super(type);
    }

    public InfoLineSprinting()
    {
        this(InfoToggle.SPRINTING);
    }
}
