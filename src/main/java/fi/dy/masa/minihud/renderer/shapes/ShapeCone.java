package fi.dy.masa.minihud.renderer.shapes;

import fi.dy.masa.minihud.config.Configs;

public class ShapeCone extends ShapeTaperedBase
{
	public ShapeCone()
	{
		super(ShapeType.CONE, Configs.Colors.SHAPE_CONE.getColor());
	}

	@Override
	protected boolean isInsideLayer(int u, int v, double currentBoundary)
	{
		// Circular boundary check for the cone
		return (u * u) + (v * v) <= (currentBoundary * currentBoundary);
	}
}
