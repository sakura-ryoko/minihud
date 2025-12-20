package fi.dy.masa.minihud.info.camera;

import fi.dy.masa.minihud.config.InfoToggle;

public class InfoLineDimension extends InfoLineCoordinatesDimensionBase
{
	public InfoLineDimension(InfoToggle type)
	{
		super(type);
	}

	public InfoLineDimension()
	{
		super(InfoToggle.DIMENSION);
	}
}
