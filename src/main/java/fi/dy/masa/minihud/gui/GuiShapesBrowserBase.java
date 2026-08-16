package fi.dy.masa.minihud.gui;

import java.nio.file.Path;
import javax.annotation.Nullable;

import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetDirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.event.WorldLoadListener;
import fi.dy.masa.minihud.gui.widgets.WidgetShapesBrowser;
import fi.dy.masa.minihud.renderer.shapes.ShapeManager;

public abstract class GuiShapesBrowserBase extends GuiListBase<WidgetFileBrowserBase.DirectoryEntry, WidgetDirectoryEntry, WidgetShapesBrowser>
{
	public GuiShapesBrowserBase(int browserX, int browserY)
	{
		super(browserX, browserY);
	}

	@Override
	protected WidgetShapesBrowser createListWidget(int listX, int listY)
	{
		return new WidgetShapesBrowser(listX, listY, 100, 100, this, this.getSelectionListener());
	}

	public abstract String getBrowserContext();

	public Path getDefaultDirectory()
	{
		Path dir = WorldLoadListener.getCurrentConfigDirectory().resolve(ShapeManager.SHAPE_FILE_DIR);
		FileUtils.createDirectoriesIfMissing(dir, MiniHUD.LOGGER::warn);
		return dir;
	}

	@Override
	@Nullable
	protected ISelectionListener<WidgetFileBrowserBase.DirectoryEntry> getSelectionListener()
	{
		return null;
	}

	@Override
	protected int getBrowserWidth()
	{
		return this.getScreenWidth() - 20;
	}

	@Override
	protected int getBrowserHeight()
	{
		return this.getScreenHeight() - 70;
	}

	public int getMaxInfoHeight()
	{
		return this.getBrowserHeight();
	}
}
