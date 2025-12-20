package fi.dy.masa.minihud.info;

import org.jetbrains.annotations.ApiStatus;

/**
 * Calculate World Rendering stats ourselves
 * since Mojang's method is broken.
 */
public class InfoLineRenderStats
{
	public static final InfoLineRenderStats INSTANCE = new InfoLineRenderStats();
	private int visibleEntityCount;
	private int visibleTileEntityCount;

	private InfoLineRenderStats()
	{
		this.visibleEntityCount = 0;
		this.visibleTileEntityCount = 0;
	}

	public int getVisibleEntityCount()
	{
		return this.visibleEntityCount;
	}

	public int getVisibleTileEntityCount()
	{
		return this.visibleTileEntityCount;
	}

	@ApiStatus.Internal
	public void updateEntityCount(int count)
	{
		this.visibleEntityCount = count;
	}

	@ApiStatus.Internal
	public void updateTileEntityCount(int count)
	{
		this.visibleTileEntityCount = count;
	}
}
