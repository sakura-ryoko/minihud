package fi.dy.masa.minihud.util;

import com.google.common.collect.ImmutableList;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

@Deprecated(forRemoval = true)
public enum WorkerThreadProfile implements IConfigOptionListEntry
{
    MAX("max", "minihud.label.worker_profile.max", 16, 20L),
    DEFAULT("default", "minihud.label.worker_profile.default", 8, 50L),
    MINIMAL("min", "minihud.label.worker_profile.min", 2, 100L),
    POTATO("potato", "minihud.label.worker_profile.potato", 1, 200L),
    ;

    private static final ImmutableList<WorkerThreadProfile> VALUES = ImmutableList.copyOf(values());

    private final String configString;
    private final String translationKey;
    private final int maxTicks;
    private final long yieldTime;

    WorkerThreadProfile(String configString, String translationKey, int maxTicks, long yieldTime)
    {
        this.configString = configString;
        this.translationKey = translationKey;
        this.maxTicks = maxTicks;
        this.yieldTime = yieldTime;
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
    public WorkerThreadProfile cycle(boolean forward)
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
    public WorkerThreadProfile fromString(String name)
    {
        return fromStringStatic(name);
    }

    public static WorkerThreadProfile fromStringStatic(String name)
    {
        for (WorkerThreadProfile val : WorkerThreadProfile.VALUES)
        {
            if (val.configString.equalsIgnoreCase(name))
            {
                return val;
            }
        }

        return WorkerThreadProfile.DEFAULT;
    }

    public int maxTicks()
    {
        return this.maxTicks;
    }

    public long yieldTime()
    {
        return this.yieldTime;
    }
}
