package fi.dy.masa.minihud.info.camera;

import fi.dy.masa.minihud.config.InfoToggle;

public class InfoLineSpeed extends InfoLineRotationYawPitchSpeedBase
{
    public InfoLineSpeed(InfoToggle type)
    {
        super(type);
    }

    public InfoLineSpeed()
    {
        this(InfoToggle.SPEED);
    }
}
