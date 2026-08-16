package fi.dy.masa.minihud.data;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import fi.dy.masa.malilib.gui.interfaces.IDirectoryCache;

public class ShapeDirectoryCache implements IDirectoryCache
{
	private static final ShapeDirectoryCache INSTANCE = new ShapeDirectoryCache();
	public static ShapeDirectoryCache getInstance() { return INSTANCE; }
	private final Map<String, Path> LAST_DIRECTORIES;

	private ShapeDirectoryCache()
	{
		this.LAST_DIRECTORIES = new HashMap<>();
	}

	@Override
	public @Nullable Path getCurrentDirectoryForContext(String context)
	{
		return this.LAST_DIRECTORIES.get(context);
	}

	@Override
	public void setCurrentDirectoryForContext(String context, Path dir)
	{
		this.LAST_DIRECTORIES.put(context, dir);
	}

	public void clear()
	{
		this.LAST_DIRECTORIES.clear();
	}
}
