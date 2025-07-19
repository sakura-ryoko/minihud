package fi.dy.masa.minihud.util;

import com.google.common.collect.ImmutableList;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum SpeedUnits implements IConfigOptionListEntry
{
    MPS         ("mps",     "minihud.label.speed_units.mps",    (mps) -> mps),
    KMPH        ("kmph",    "minihud.label.speed_units.kmph",   (mps) -> mps*3.6),
    FPS         ("fps",     "minihud.label.speed_units.fps",    (mps) -> mps*3.28084),
    MPH         ("mph",     "minihud.label.speed_units.mph",    (mps) -> mps*2.23694),
    C           ("c",       "minihud.label.speed_units.c",      (mps) -> mps/299792458.0),
    MACH        ("mach",    "minihud.label.speed_units.mach",   (mps) -> mps/343.0);

    private static final ImmutableList<SpeedUnits> VALUES = ImmutableList.copyOf(values());

    private final String configString;
    private final String translationKey;
    private final Conversion conversion;
    public final String suffix;

    SpeedUnits(String configString, String translationKey, Conversion conversion)
    {
        this.configString = configString;
        this.translationKey = translationKey;
        this.conversion = conversion;
        this.suffix = configString;
    }

    public double convert(double metersPerSecond)
    {
        return this.conversion.convert(metersPerSecond);
    }

    @Override
    public String getStringValue()
    {
        return this.configString;
    }

    @Override
    public String getDisplayName()
    {
        return StringUtils.translate(this.translationKey);
    }

    @Override
    public SpeedUnits cycle(boolean forward)
    {
        int id = this.ordinal();

        if (forward)
        {
            if (++id >= values().length)
            {
                id = 0;
            }
        }
        else
        {
            if (--id < 0)
            {
                id = values().length - 1;
            }
        }

        return values()[id % values().length];
    }

    @Override
    public SpeedUnits fromString(String name)
    {
        return fromStringStatic(name);
    }

    public static SpeedUnits fromStringStatic(String name)
    {
        for (SpeedUnits val : SpeedUnits.VALUES)
        {
            if (val.configString.equalsIgnoreCase(name))
            {
                return val;
            }
        }

        return SpeedUnits.MPS;
    }

    private interface Conversion
    {
        double convert(double metersPerSecond);
    }
}