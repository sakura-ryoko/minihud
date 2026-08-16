package fi.dy.masa.minihud.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;

import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.minihud.renderer.shapes.ShapeBase;
import fi.dy.masa.minihud.renderer.shapes.ShapeManager;

public class GuiShapesExport extends GuiShapesExportBase
{
	public GuiShapesExport(@Nullable ShapeBase shape)
	{
		super(shape);
		this.title = StringUtils.translate("minihud.gui.title.shape_export");
	}

	@Override
	public String getBrowserContext()
	{
		return "shape_export";
	}

	@Override
	protected IButtonActionListener createButtonListener(ButtonType type)
	{
		return new ButtonListener(type, this);
	}

	private record ButtonListener(ButtonType type, GuiShapesExport gui) implements IButtonActionListener
	{
		@Override
		public void actionPerformedWithButton(ButtonBase button, int mouseButton)
		{
			if (this.type == ButtonType.EXPORT)
			{
				Path dir = this.gui.getListWidget().getCurrentDirectory();
				String fileName = this.gui.getTextFieldText();

				if (!Files.isDirectory(dir))
				{
					this.gui.addMessage(Message.MessageType.ERROR, "minihud.error.shape_export.invalid_directory", dir.toAbsolutePath());
					return;
				}

				if (fileName.isEmpty())
				{
					this.gui.addMessage(Message.MessageType.ERROR, "minihud.error.shape_export.invalid_name", fileName);
					return;
				}

				String tempFileName = fileName;

				if (!tempFileName.endsWith(ShapeManager.SHAPE_FILE_EXT))
				{
					tempFileName += ShapeManager.SHAPE_FILE_EXT;
				}

				Path tempFile = dir.resolve(tempFileName);

				if (this.gui.shape != null)
				{
					if (ShapeManager.INSTANCE.exportShapeToFile(this.gui.shape, tempFile, true))
					{
						this.gui.getListWidget().refreshEntries();
						String key = "minihud.message.shape.exported";
						this.gui.addMessage(Message.MessageType.SUCCESS, key, tempFile.getFileName().toString());

						if (this.gui.mc.player != null)
						{
							StringUtils.sendOpenFileChatMessage(this.gui.mc.player, key, tempFile.toFile());
						}
					}
				}
				else
				{
					this.gui.addMessage(Message.MessageType.ERROR, "minihud.message.error.shape_export");
				}
			}
		}
	}
}
