package fi.dy.masa.minihud.renderer.shapes;

import fi.dy.masa.minihud.config.Configs;

public class ShapeDiamondPyramid extends ShapeTaperedBase
{
	public ShapeDiamondPyramid()
	{
		super(ShapeType.DIAMOND_PYRAMID, Configs.Colors.SHAPE_DIAMOND_PYRAMID.getColor());
		this.useCulling = false;
	}

	@Override
	protected boolean isInsideLayer(int u, int v, double currentBoundary)
	{
		return Math.abs(u) + Math.abs(v) <= currentBoundary;
	}
}
