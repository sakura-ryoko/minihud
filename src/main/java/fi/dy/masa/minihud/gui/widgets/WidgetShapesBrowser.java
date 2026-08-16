package fi.dy.masa.minihud.gui.widgets;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.data.ShapeDirectoryCache;
import fi.dy.masa.minihud.event.WorldLoadListener;
import fi.dy.masa.minihud.gui.GuiShapesBrowserBase;
import fi.dy.masa.minihud.gui.Icons;
import fi.dy.masa.minihud.renderer.shapes.ShapeBase;
import fi.dy.masa.minihud.renderer.shapes.ShapeManager;

public class WidgetShapesBrowser extends WidgetFileBrowserBase
{
	protected static final FileFilter FILE_FILTER_SHAPE = new FileFilterShape();

	protected final Map<Path, ShapeBase> cachedShapes = new HashMap<>();
	protected final GuiShapesBrowserBase parent;
	protected final int infoWidth;
	protected final int infoHeight;

	public WidgetShapesBrowser(int x, int y, int width, int height,
	                           GuiShapesBrowserBase parent, @Nullable ISelectionListener<DirectoryEntry> selectionListener)
	{
		super(x, y, width, height,
		      ShapeDirectoryCache.getInstance(),
		      parent.getBrowserContext(), parent.getDefaultDirectory(),
		      selectionListener, Icons.FILE_ICON_SHAPE);

		this.title = StringUtils.translate("minihud.gui.title.shapes_browser");
		this.parent = parent;
		this.infoWidth = 170;
		this.infoHeight = 310;
		ShapeDirectoryCache.getInstance().clear();
	}

	@Override
	protected int getBrowserWidthForTotalWidth(int width)
	{
		return super.getBrowserWidthForTotalWidth(width) - this.infoWidth;
	}

	@Override
	protected Path getRootDirectory()
	{
		Path dir = WorldLoadListener.getCurrentConfigDirectory().resolve(ShapeManager.SHAPE_FILE_DIR);
		FileUtils.createDirectoriesIfMissing(dir, MiniHUD.LOGGER::warn);
		return dir;
	}

	@Override
	protected FileFilter getFileFilter()
	{
		return FILE_FILTER_SHAPE;
	}

	@Override
	protected void drawAdditionalContents(GuiContext ctx, int mouseX, int mouseY)
	{
		this.drawSelectedShapeInfo(ctx, this.getLastSelectedEntry());
	}

	protected void drawSelectedShapeInfo(GuiContext ctx, @Nullable DirectoryEntry entry)
	{
		int x = this.posX + this.totalWidth - this.infoWidth;
		int y = this.posY;
		int height = Math.min(this.infoHeight, this.parent.getMaxInfoHeight());

		RenderUtils.drawOutlinedBox(ctx, x, y, this.infoWidth, height, 0xA0000000, COLOR_HORIZONTAL_BAR);

		if (entry == null)
		{
			return;
		}

		ShapeBase shape = this.getOrLoadCachedShape(entry.getFullPath());

		if (shape != null)
		{
			x += 3;
			y += 3;
			int textColor = 0xC0C0C0C0;
			int valueColor = 0xFFFFFFFF;

			List<String> lines = shape.getWidgetHoverLines();

			for (String line : lines)
			{
				this.drawString(ctx, line, x, y, textColor);
				y += 12;
			}
		}
	}

	@Nullable
	private ShapeBase getOrLoadCachedShape(Path file)
	{
		if (this.cachedShapes.containsKey(file))
		{
			return this.cachedShapes.get(file);
		}

		ShapeBase shape = ShapeManager.loadShapeFromFile(file);

		if (shape != null)
		{
			this.cachedShapes.put(file, shape);
			return shape;
		}

		return null;
	}

	public static class FileFilterShape extends FileFilter
	{
		@Override
		public boolean accept(Path entry)
		{
			return entry.getFileName().toString().endsWith(ShapeManager.SHAPE_FILE_EXT);
		}
	}
}
