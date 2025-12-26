package fi.dy.masa.minihud.info.camera;

import fi.dy.masa.minihud.config.InfoToggle;

public class InfoLineCoordinates extends InfoLineCoordinatesDimensionBase
{
	public InfoLineCoordinates(InfoToggle type)
	{
		super(type);
	}

	public InfoLineCoordinates()
	{
		super(InfoToggle.COORDINATES);
	}
}
