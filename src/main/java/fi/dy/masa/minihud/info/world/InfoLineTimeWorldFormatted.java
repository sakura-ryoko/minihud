package fi.dy.masa.minihud.info.world;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.world.level.Level;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

public class InfoLineTimeWorldFormatted extends InfoLine
{
    private static final String TIME_KEY = Reference.MOD_ID+".info_line";

    public InfoLineTimeWorldFormatted(InfoToggle type)
    {
        super(type);
    }

    public InfoLineTimeWorldFormatted()
    {
        this(InfoToggle.TIME_WORLD_FORMATTED);
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
    public List<Entry> parseWorld(@Nonnull Level world)
    {
        List<Entry> list = new ArrayList<>();

        try
        {
            final long timeDay = world.getDayTime();
            final long day = (int) (timeDay / 24000);
            // 1 tick = 3.6 seconds in MC (0.2777... seconds IRL)
            final int dayTicks = (int) (timeDay % 24000);
            final int hour = (int) ((dayTicks / 1000) + 6) % 24;
            final int min = (int) (dayTicks / 16.666666) % 60;
            final int sec = (int) (dayTicks / 0.277777) % 60;
            final int minIrl = (int) (dayTicks / 1200) % 20;
            final int secIrl = (int) (dayTicks / 20) % 60;
            // Moonphase has 8 different states in MC
            final int moonNumber = (int) day % 8;
            String moon;

            if (moonNumber > 7)
            {
                moon = this.qt(TIME_KEY+".invalid_value");
            }
            else
            {
                moon = this.qt(TIME_KEY+".time_world_formatted.moon_" + moonNumber);
            }

            String str = Configs.Generic.DATE_FORMAT_MINECRAFT.getStringValue();

            str = str.replace("{DAY}",  String.format("%d", day));
            str = str.replace("{DAY_1}",String.format("%d", day + 1));
            str = str.replace("{HOUR}", String.format("%02d", hour));
            str = str.replace("{MIN}",  String.format("%02d", min));
            str = str.replace("{SEC}",  String.format("%02d", sec));
            str = str.replace("{MINIRL}", String.format("%02d", minIrl));
            str = str.replace("{SECIRL}", String.format("%02d", secIrl));
            str = str.replace("{MOON}",  String.format("%s", moon));

            list.add(this.of(str));
        }
        catch (Exception e)
        {
            list.add(this.translate(TIME_KEY+".time.exception"));
        }

        return list;
    }
}
