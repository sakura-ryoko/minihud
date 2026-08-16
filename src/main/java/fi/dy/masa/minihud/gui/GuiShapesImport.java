package fi.dy.masa.minihud.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfirmAction;
import fi.dy.masa.malilib.gui.GuiTextInputFeedback;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase;
import fi.dy.masa.malilib.interfaces.IStringConsumerFeedback;
import fi.dy.masa.malilib.util.FileCopier;
import fi.dy.masa.malilib.util.FileDeleter;
import fi.dy.masa.malilib.util.FileRenamer;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.renderer.shapes.ShapeBase;
import fi.dy.masa.minihud.renderer.shapes.ShapeManager;

public class GuiShapesImport extends GuiShapesBrowserBase implements ISelectionListener<WidgetFileBrowserBase.DirectoryEntry>
{
	public GuiShapesImport()
	{
		super(12, 24);

		this.title = StringUtils.translate("minihud.gui.title.shape_import");
	}

	@Override
	public String getBrowserContext()
	{
		return "shapes_import";
	}

	@Override
	public void initGui()
	{
		super.initGui();
		this.createButtons();
	}

	private void createButtons()
	{
		int x = 12;
		int y = this.getScreenHeight() - 40;

		if (this.getListWidget() == null) { return; }
		WidgetFileBrowserBase.DirectoryEntry selected = this.getListWidget().getLastSelectedEntry();

		if (selected != null)
		{
			x += this.createButton(x, y, -1, Type.IMPORT);
			x += this.createButton(x, y, -1, Type.RENAME_SHAPE);
			x += this.createButton(x, y, -1, Type.RENAME_FILE);
			x += this.createButton(x, y, -1, Type.DELETE_FILE);
		}
		else
		{
			x += this.createButton(x, y, -1, Type.IMPORT);
		}
	}

	private int createButton(int x, int y, int width, Type type)
	{
		ButtonListener listener = new ButtonListener(type, this);
		String label = StringUtils.translate(type.getTranslationKey());

		if (width == -1)
		{
			width = this.getStringWidth(label) + 10;
		}

		ButtonGeneric button = new ButtonGeneric(x, y, width, 20, label);

		if (type.getHoverInfo() != null)
		{
			button.setHoverStrings(StringUtils.translate(type.getHoverInfo()));
		}

		this.addButton(button, listener);

		return width;
	}

	@Override
	public void onSelectionChange(@Nullable WidgetFileBrowserBase.DirectoryEntry entry)
	{
		this.clearButtons();
		this.createButtons();
	}

	@Override
	protected ISelectionListener<WidgetFileBrowserBase.DirectoryEntry> getSelectionListener()
	{
		return this;
	}

	private record ShapeRenamer(Path file, GuiShapesImport gui)
			implements IStringConsumerFeedback
	{
		@Override
		public boolean setString(String string)
		{
			ShapeBase shape = ShapeManager.loadShapeFromFile(this.file);

			if (shape != null)
			{
				shape.setDisplayName(string);

				if (ShapeManager.INSTANCE.exportShapeToFile(shape, this.file, true))
				{
					return true;
				}
				else
				{
					this.gui.setString(StringUtils.translate("minihud.error.shape_import.cant_read_file"));
				}
			}
			else
			{
				this.gui.setString(StringUtils.translate("minihud.error.shape_import.cant_read_file"));
			}

			return false;
		}
	}

	private record ButtonListener(Type type, GuiShapesImport gui) implements IButtonActionListener
	{
		@Override
		public void actionPerformedWithButton(ButtonBase button, int mouseButton)
		{
			if (this.gui.getListWidget() == null) { return; }
			WidgetFileBrowserBase.DirectoryEntry entry = this.gui.getListWidget().getLastSelectedEntry();

			if (entry == null)
			{
				this.gui.addMessage(Message.MessageType.ERROR, "minihud.error.shape_import.no_shape_selected");
			}
			else
			{
				Path file = entry.getFullPath();
				String fileName = entry.name();

				if (!Files.exists(file) || !Files.isReadable(file))
				{
					this.gui.addMessage(Message.MessageType.ERROR, "minihud.error.shape_import.cant_read_file", file.getFileName());
					return;
				}

				if (!fileName.endsWith(ShapeManager.SHAPE_FILE_EXT))
				{
					this.gui.addMessage(Message.MessageType.ERROR, "minihud.error.shape_import.unsupported_type", file.getFileName());
					return;
				}

				if (this.type == Type.RENAME_FILE)
				{
					FileRenamer renamer = new FileRenamer(file, this.gui.getListWidget(), Configs.Generic.DISPLAY_FILE_OPS_FEEDBACK.getBooleanValue());
					GuiBase.openGui(new GuiTextInputFeedback(256, "minihud.gui.title.rename_file", entry.name(), this.gui, renamer));
				}
				else if (this.type == Type.COPY_FILE)
				{
					FileCopier copier = new FileCopier(file, this.gui.getListWidget(), Configs.Generic.DISPLAY_FILE_OPS_FEEDBACK.getBooleanValue());
					GuiBase.openGui(new GuiTextInputFeedback(256, "minihud.gui.title.copy_file", entry.name(), this.gui, copier));
				}
				else if (this.type == Type.DELETE_FILE)
				{
					FileDeleter deleter = new FileDeleter(entry.getFullPath(), this.gui.getListWidget(), Configs.Generic.DISPLAY_FILE_OPS_FEEDBACK.getBooleanValue());
					GuiBase.openGui(new GuiConfirmAction(400, "minihud.gui.title.confirm_file_deletion", deleter, this.gui, "minihud.gui.message.confirm_file_deletion", entry.name()));
				}

				ShapeBase shape = ShapeManager.loadShapeFromFile(file);

				if (shape == null)
				{
					this.gui.addMessage(Message.MessageType.ERROR, "minihud.error.shape_import.cant_read_file", file.getFileName());
					return;
				}

				if (this.type == Type.IMPORT)
				{
					ShapeManager.INSTANCE.addShape(shape);
					GuiBase.openGui(this.gui.getParent());
				}
				else if (this.type == Type.RENAME_SHAPE)
				{
					String oldName = shape.getDisplayName();
					GuiBase.openGui(new GuiTextInputFeedback(256, "minihud.gui.title.rename_shape", oldName, this.gui, new ShapeRenamer(file, this.gui)));
				}
			}
		}
	}

	public enum Type
	{
		IMPORT          ("minihud.gui.button.import"),
		RENAME_SHAPE    ("minihud.gui.button.rename_shape"),
		RENAME_FILE     ("minihud.gui.button.rename_file"),
		COPY_FILE       ("minihud.gui.button.copy_file"),
		DELETE_FILE     ("minihud.gui.button.delete_file"),
		;

		private final String translationKey;
		@Nullable
		private final String hoverInfo;

		Type(String translationKey)
		{
			this(translationKey, null);
		}

		Type(String translationKey, @Nullable String hoverInfo)
		{
			this.translationKey = translationKey;
			this.hoverInfo = hoverInfo;
		}

		public String getTranslationKey()
		{
			return this.translationKey;
		}

		public @Nullable String getHoverInfo()
		{
			return this.hoverInfo;
		}
	}
}
