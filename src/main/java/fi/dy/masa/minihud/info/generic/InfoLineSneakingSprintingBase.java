package fi.dy.masa.minihud.info.generic;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

public abstract class InfoLineSneakingSprintingBase extends InfoLine
{
	private static final String SNEAKING_KEY = Reference.MOD_ID+".info_line.sneaking";
	private static final String SPRINT_KEY = Reference.MOD_ID+".info_line.sprinting";

	public InfoLineSneakingSprintingBase(InfoToggle type)
	{
		super(type);
	}

	@Override
	public boolean succeededType() { return this.succeeded; }

	@Override
	public List<Entry> parse(@Nonnull InfoLineContext ctx)
	{
		if (this.getClientWorld() == null || this.mc().player == null)
		{
			return null;
		}

		List<Entry> list = new ArrayList<>();
		String pre = "";
		StringBuilder str = new StringBuilder(256);

		if (InfoToggle.SPRINTING.getBooleanValue())
		{
			if (this.mc().player.isSprinting())
			{
				boolean toggled = this.mc().options.toggleSprint().get();
				String msg = this.qt(
						toggled
						? SPRINT_KEY + ".toggled"
						: SPRINT_KEY
				);

				str.append(msg);
				pre = " / ";
			}
		}

		if (InfoToggle.SNEAKING.getBooleanValue())
		{
			if (this.mc().player.isCrouching())
			{
				boolean toggled = this.mc().options.toggleCrouch().get();
				String msg = this.qt(
						toggled
						? SNEAKING_KEY + ".toggled"
						: SNEAKING_KEY
				);
				str.append(pre).append(msg);
			}
		}

		list.add(this.of(str.toString()));
		this.succeeded = true;

		return list;
	}
}
