package fi.dy.masa.minihud.renderer.shapes;

import fi.dy.masa.minihud.config.Configs;

public class ShapeOctagonPyramid extends ShapeTaperedBase
{
	public ShapeOctagonPyramid()
	{
		super(ShapeType.OCTAGON_PYRAMID, Configs.Colors.SHAPE_OCTAGON_PYRAMID.getColor());
	}

	@Override
	protected boolean isInsideLayer(int u, int v, double currentBoundary)
	{
		boolean insideSquare = Math.abs(u) <= currentBoundary && Math.abs(v) <= currentBoundary;
		boolean insideChamfer = Math.abs(u) + Math.abs(v) <= (currentBoundary * 1.5);
		return insideSquare && insideChamfer;
	}
}
