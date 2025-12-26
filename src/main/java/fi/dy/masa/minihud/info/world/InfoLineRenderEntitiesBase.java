package fi.dy.masa.minihud.info.world;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;
import fi.dy.masa.minihud.info.InfoLineRenderStats;

public class InfoLineRenderEntitiesBase extends InfoLine
{
    private static final String ENT_KEY = Reference.MOD_ID+".info_line.entities";
	private static final String TILES_KEY = Reference.MOD_ID+".info_line.tile_entities";

    public InfoLineRenderEntitiesBase(InfoToggle type)
    {
        super(type);
    }

    @Override
    public boolean succeededType() { return this.succeeded; }

    @Override
    public List<Entry> parse(@Nonnull InfoLineContext ctx)
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

	    if (world instanceof ClientLevel level)
	    {
		    String pre = "";
		    StringBuilder str = new StringBuilder(128);

		    if (InfoToggle.ENTITIES.getBooleanValue())
		    {
			    str.append(String.format("E: %d/%d",
			                                  InfoLineRenderStats.INSTANCE.getVisibleEntityCount(),
			                                  level.getEntityCount()
			    ));

			    pre = " / ";
		    }

			if (InfoToggle.TILE_ENTITIES.getBooleanValue())
			{
				str.append(pre).append(String.format("TE: %d",
				                         InfoLineRenderStats.INSTANCE.getVisibleTileEntityCount()
				));
			}

		    list.add(this.of(str.toString()));
		    this.succeeded = true;

		    return list;
	    }

	    return null;
    }
}
