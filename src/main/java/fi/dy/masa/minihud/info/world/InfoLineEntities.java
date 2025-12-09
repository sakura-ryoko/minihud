package fi.dy.masa.minihud.info.world;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.world.level.Level;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;

public class InfoLineEntities extends InfoLine
{
    private static final String ENT_KEY = Reference.MOD_ID+".info_line.entities";

    public InfoLineEntities(InfoToggle type)
    {
        super(type);
    }

    public InfoLineEntities()
    {
        this(InfoToggle.ENTITIES);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@Nonnull Context ctx)
    {
        if (this.getClientWorld() == null)
        {
            return null;
        }

        return this.parseWorld(ctx.world() == null ? this.getClientWorld() : ctx.world());
    }

    @Override
    public List<Entry> parseWorld(@Nonnull Level world)
    {
		List<Entry> list = new ArrayList<>();
	    String ent = this.mc().levelRenderer.getEntityStatistics();

		if (ent != null && !ent.isEmpty())
		{
			int p = ent.indexOf(",");

			if (p != -1)
			{
				ent = ent.substring(0, p);
			}

			list.add(this.of(ent));

			return list;
		}

		return null;
    }
}
