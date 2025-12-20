package fi.dy.masa.minihud.info.camera;

import fi.dy.masa.minihud.config.InfoToggle;

public class InfoLineRotationPitch extends InfoLineRotationYawPitchSpeedBase
{
    public InfoLineRotationPitch(InfoToggle type)
    {
        super(type);
    }

    public InfoLineRotationPitch()
    {
        this(InfoToggle.ROTATION_PITCH);
    }
}
