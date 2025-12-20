package fi.dy.masa.minihud.info.camera;

import fi.dy.masa.minihud.config.InfoToggle;

public class InfoLineCoordinatesScaled extends InfoLineCoordinatesDimensionBase
{
	public InfoLineCoordinatesScaled(InfoToggle type)
	{
		super(type);
	}

	public InfoLineCoordinatesScaled()
	{
		super(InfoToggle.COORDINATES_SCALED);
	}
}
