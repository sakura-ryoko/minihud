package fi.dy.masa.minihud.info.camera;

import fi.dy.masa.minihud.config.InfoToggle;

public class InfoLineRotationYaw extends InfoLineRotationYawPitchSpeedBase
{
    public InfoLineRotationYaw(InfoToggle type)
    {
        super(type);
    }

    public InfoLineRotationYaw()
    {
        this(InfoToggle.ROTATION_YAW);
    }
}
