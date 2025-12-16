package fi.dy.masa.minihud.info.generic;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;
import fi.dy.masa.minihud.util.MiscUtils;

public class InfoLineWeather extends InfoLine
{
    private static final String WEATHER_KEY = Reference.MOD_ID+".info_line.weather";
    private static final String REMAIN_KEY = Reference.MOD_ID+".info_line.remaining";

    public InfoLineWeather(InfoToggle type)
    {
        super(type);
    }

    public InfoLineWeather()
    {
        this(InfoToggle.WEATHER);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@Nonnull InfoLineContext ctx)
    {
        List<Entry> list = new ArrayList<>();

        String weatherType = "clear";
        int weatherTime = -1;

        if (this.getHudData().hasValidWeatherCycle())
        {
            if (this.getHudData().isWeatherThunder() && this.getHudData().isWeatherRain())
            {
                weatherType = "thundering";
                weatherTime = this.getHudData().getThunderTime();
            }
            else if (this.getHudData().isWeatherRain())
            {
                weatherType = "raining";
                weatherTime = this.getHudData().getRainTime();
            }
            else if (this.getHudData().isWeatherClear())
            {
                weatherType = "clear";
                weatherTime = this.getHudData().getClearTime();
            }

            if (weatherTime < 1)
            {
                list.add(this.translate(WEATHER_KEY,
                                        this.qt(WEATHER_KEY + "." + weatherType), ""
                ));
            }
            else
            {
                // 50 = 1000 (ms/s) / 20 (ticks/s)
                list.add(this.translate(WEATHER_KEY,
                                        this.qt(WEATHER_KEY + "." + weatherType),
                                        ", " + MiscUtils.formatDuration(weatherTime * 50L)
                                                + " " + this.qt(REMAIN_KEY)
                ));
            }
        }
        else
        {
            list.add(this.translate(WEATHER_KEY,
                                    this.qt(WEATHER_KEY + ".invalid"), ""
            ));

            // Clear any invalid values
            if (this.getHudData().getClearTime() > -1)
            {
                this.getHudData().resetWeatherData();
            }
        }

        return list;
    }
}
