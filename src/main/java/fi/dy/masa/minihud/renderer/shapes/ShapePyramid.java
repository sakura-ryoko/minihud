package fi.dy.masa.minihud.renderer.shapes;

import fi.dy.masa.minihud.config.Configs;

public class ShapePyramid extends ShapeTaperedBase
{
	public ShapePyramid()
	{
		super(ShapeType.PYRAMID, Configs.Colors.SHAPE_PYRAMID.getColor());
	}

	@Override
	protected boolean isInsideLayer(int u, int v, double currentBoundary)
	{
		return Math.abs(u) <= currentBoundary && Math.abs(v) <= currentBoundary;
	}
}
