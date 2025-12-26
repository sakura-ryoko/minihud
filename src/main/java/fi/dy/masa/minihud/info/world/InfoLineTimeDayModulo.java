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

public class InfoLineTimeDayModulo extends InfoLine
{
    private static final String TIME_KEY = Reference.MOD_ID + ".info_line.time_day_modulo";

    public InfoLineTimeDayModulo(InfoToggle type)
    {
        super(type);
    }

    public InfoLineTimeDayModulo()
    {
        this(InfoToggle.TIME_DAY_MODULO);
    }

    @Override
    public boolean succeededType() {return false;}

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
        final int mod = Configs.Generic.TIME_DAY_DIVISOR.getIntegerValue();
        final long current = world.getDayTime() % mod;

        list.add(this.translate(TIME_KEY, mod, current));

        return list;
    }
}
