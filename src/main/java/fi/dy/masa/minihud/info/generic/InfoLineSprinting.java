package fi.dy.masa.minihud.info.generic;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

public class InfoLineSprinting extends InfoLine
{
    private static final String SPRINT_KEY = Reference.MOD_ID+".info_line.sprinting";

    public InfoLineSprinting(InfoToggle type)
    {
        super(type);
    }

    public InfoLineSprinting()
    {
        this(InfoToggle.SPRINTING);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@Nonnull InfoLineContext ctx)
    {
        List<Entry> list = new ArrayList<>();

        if (this.getClientWorld() == null || this.mc().player == null)
        {
            return null;
        }

		if (this.mc().player.isSprinting())
		{
			list.add(this.translate(SPRINT_KEY));
		}

        return list;
    }
}
