package fi.dy.masa.minihud.data;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.time.DurationFormatUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.world.World;
import net.minecraft.world.level.LevelProperties;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.WorldUtils;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.event.RenderHandler;
import fi.dy.masa.minihud.network.ServuxEntitiesPacket;
import fi.dy.masa.minihud.util.DataStorage;
import fi.dy.masa.minihud.util.MiscUtils;

public class InfoLinesHandler
{
    private static final InfoLinesHandler INSTANCE = new InfoLinesHandler();
    public static InfoLinesHandler getInstance() { return INSTANCE; }

    private final DataStorage data;
    private final EntitiesDataStorage entities;
    private final Date date;

    public InfoLinesHandler()
    {
        this.data = DataStorage.getInstance();
        this.entities = EntitiesDataStorage.getInstance();
        this.date = new Date();
    }

    private DataStorage data()
    {
        return this.data;
    }

    private EntitiesDataStorage ent()
    {
        return this.entities;
    }

    private void addLineI18n(String str, Object... args)
    {
        RenderHandler.getInstance().addLineI18n(str, args);
    }

    private void addLine(String str)
    {
        RenderHandler.getInstance().addLine(str);
    }

    public void addFPS()
    {
        this.addLineI18n("minihud.info_line.fps", MinecraftClient.getInstance().getCurrentFps());
    }

    public void addMemoryUsage()
    {
        long memMax = Runtime.getRuntime().maxMemory();
        long memTotal = Runtime.getRuntime().totalMemory();
        long memFree = Runtime.getRuntime().freeMemory();
        long memUsed = memTotal - memFree;

        this.addLineI18n("minihud.info_line.memory_usage",
                         memUsed * 100L / memMax,
                         MiscUtils.bytesToMb(memUsed),
                         MiscUtils.bytesToMb(memMax),
                         memTotal * 100L / memMax,
                         MiscUtils.bytesToMb(memTotal));
    }

    public void addRealTime()
    {
        try
        {
            SimpleDateFormat sdf = new SimpleDateFormat(Configs.Generic.DATE_FORMAT_REAL.getStringValue());
            this.date.setTime(System.currentTimeMillis());
            this.addLine(sdf.format(this.date));
        }
        catch (Exception e)
        {
            this.addLineI18n("minihud.info_line.time.exception");
        }
    }

    public void addWorldTime(World world)
    {
        long current = world.getTimeOfDay();
        long total = world.getTime();
        this.addLineI18n("minihud.info_line.time_world", current, total);
    }

    public void addWorldTimeFormatted(World world)
    {
        try
        {
            long timeDay = world.getTimeOfDay();
            long day = (int) (timeDay / 24000);
            // 1 tick = 3.6 seconds in MC (0.2777... seconds IRL)
            int dayTicks = (int) (timeDay % 24000);
            int hour = (int) ((dayTicks / 1000) + 6) % 24;
            int min = (int) (dayTicks / 16.666666) % 60;
            int sec = (int) (dayTicks / 0.277777) % 60;
            // Moonphase has 8 different states in MC
            int moonNumber = (int) day % 8;
            String moon;
            if (moonNumber > 7)
            {
                moon = StringUtils.translate("minihud.info_line.invalid_value");
            }
            else
            {
                moon = StringUtils.translate("minihud.info_line.time_world_formatted.moon_" + moonNumber);
            }

            String str = Configs.Generic.DATE_FORMAT_MINECRAFT.getStringValue();
            str = str.replace("{DAY}",  String.format("%d", day));
            str = str.replace("{DAY_1}",String.format("%d", day + 1));
            str = str.replace("{HOUR}", String.format("%02d", hour));
            str = str.replace("{MIN}",  String.format("%02d", min));
            str = str.replace("{SEC}",  String.format("%02d", sec));
            str = str.replace("{MOON}",  String.format("%s", moon));

            this.addLine(str);
        }
        catch (Exception e)
        {
            this.addLineI18n("minihud.info_line.time.exception");
        }
    }

    public void addDayTimeModulo(World world)
    {
        int mod = Configs.Generic.TIME_DAY_DIVISOR.getIntegerValue();
        long current = world.getTimeOfDay() % mod;
        this.addLineI18n("minihud.info_line.time_day_modulo", mod, current);
    }

    public void addTotalTimeModulo(World world)
    {
        int mod = Configs.Generic.TIME_TOTAL_DIVISOR.getIntegerValue();
        long current = world.getTime() % mod;
        this.addLineI18n("minihud.info_line.time_total_modulo", mod, current);
    }

    public void addServerTPS()
    {
        if (this.data().hasIntegratedServer() && (this.data().getIntegratedServer().getTicks() % 10) == 0)
        {
            this.data().updateIntegratedServerTPS();
        }

        if (this.data().hasTPSData())
        {
            double tps = this.data().getServerTPS();
            double mspt = this.data().getServerMSPT();
            String rst = GuiBase.TXT_RST;
            String preTps = tps >= 20.0D ? GuiBase.TXT_GREEN : GuiBase.TXT_RED;
            String preMspt;

            // Carpet server and integrated server have actual meaningful MSPT data available
            if (this.data().hasCarpetServer() || this.data().isSinglePlayer())
            {
                if      (mspt <= 40) { preMspt = GuiBase.TXT_GREEN; }
                else if (mspt <= 45) { preMspt = GuiBase.TXT_YELLOW; }
                else if (mspt <= 50) { preMspt = GuiBase.TXT_GOLD; }
                else                 { preMspt = GuiBase.TXT_RED; }

                this.addLineI18n("minihud.info_line.server_tps", preTps, tps, rst, preMspt, mspt, rst);
            }
            else
            {
                if (mspt <= 51) { preMspt = GuiBase.TXT_GREEN; }
                else            { preMspt = GuiBase.TXT_RED; }

                this.addLineI18n("minihud.info_line.server_tps.est", preTps, tps, rst, preMspt, mspt, rst);
            }
        }
        else
        {
            this.addLineI18n("minihud.info_line.server_tps.invalid");
        }
    }

    public void addServux()
    {
        if (ent().hasServuxServer())
        {
            this.addLineI18n("minihud.info_line.servux",
                             ent().getServuxVersion(),
                             ServuxEntitiesPacket.PROTOCOL_VERSION,
                             ent().getPendingBLockEntitiesCount(),
                             ent().getPendingEntitiesCount()
            );
        }
    }

    public void addEntityData()
    {
        // Entity Data Storage values
    }

    public void addWeather()
    {
        World bestWorld = WorldUtils.getBestWorld(this.data().getMc());
        String weatherType = "clear";
        int weatherTime = -1;

        if (bestWorld == null)
        {
            return;
        }
        if (bestWorld.getLevelProperties().isThundering())
        {
            weatherType = "thundering";
            if (bestWorld.getLevelProperties() instanceof LevelProperties lp)
            {
                weatherTime = lp.getThunderTime();
            }
        }
        else if (bestWorld.getLevelProperties().isRaining())
        {
            weatherType = "raining";
            if (bestWorld.getLevelProperties() instanceof LevelProperties lp)
            {
                weatherTime = lp.getRainTime();
            }
        }

        if (weatherType.equals("clear") || weatherTime == -1)
        {
            this.addLineI18n("minihud.info_line.weather", StringUtils.translate("minihud.info_line.weather." + weatherType), "");
        }
        else
        {
            // 50 = 1000 (ms/s) / 20 (ticks/s)
            this.addLineI18n("minihud.info_line.weather",
                     StringUtils.translate("minihud.info_line.weather." + weatherType),
                     ", " + DurationFormatUtils.formatDurationWords(weatherTime * 50L, true, true)
                     + " " + StringUtils.translate("minihud.info_line.remaining")
            );
        }
    }
}
