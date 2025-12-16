package fi.dy.masa.minihud.info.world;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.world.World;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

public class InfoLineTimeWorld extends InfoLine
{
    private static final String TIME_KEY = Reference.MOD_ID+".info_line.time_world";

    public InfoLineTimeWorld(InfoToggle type)
    {
        super(type);
    }

    public InfoLineTimeWorld()
    {
        this(InfoToggle.TIME_WORLD);
    }

    @Override
    public boolean succeededType() { return false; }

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
    public List<Entry> parseWorld(@Nonnull World world)
    {
        List<Entry> list = new ArrayList<>();

        list.add(this.translate(TIME_KEY, world.getTimeOfDay(), world.getTime()));

        return list;
    }
}
