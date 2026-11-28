package fi.dy.masa.minihud.util;

import java.util.Arrays;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.data.Color4f;

public enum LightLevelAnimation implements IConfigOptionListEntry
{
    NONE("none", (color, _, _) -> color),
    WAVE_IN("wave_in", (original, distance, timeMs) ->
            (distance + timeMs / 10) % 360 < 16 ? Color4f.WHITE : original
    ),
    WAVE_OUT("wave_out", (original, distance, timeMs) ->
            WAVE_IN.apply(original, 360 - distance % 360, timeMs)),
    BLINK_NEARBY("blink_nearby", (original, distance, timeMs) ->
            distance >= 12 || timeMs % 1024 < 512 ? original : Color4f.WHITE
    ),
    BLINK_NEARBY_SMOOTH("blink_nearby_smooth", (original, distance, timeMs) ->
    {
        if (distance > 24)
        {
            return original;
        }

        double str = 1 - distance / 24;
        double sin = Math.sin(timeMs / 256 + str * 3);
        boolean darken = sin < 0;
        float mul = (float) ((1 - Math.pow(1 - Math.abs(sin), 1.618)) * str);
        return mul < 0.002 ? original : darken ? new Color4f(
                original.r * (1 - mul),
                original.g * (1 - mul),
                original.b * (1 - mul),
                original.a
        ) : mul > 0.998 ? Color4f.WHITE : new Color4f(
                original.r + (1 - original.r) * mul,
                original.g + (1 - original.g) * mul,
                original.b + (1 - original.b) * mul,
                original.a
        );
    });

    private final String configString;
    private final String translationKey;
    private final IColorConverter converter;

    LightLevelAnimation(String configString, IColorConverter converter)
    {
        this.configString = configString;
        this.translationKey = "minihud.label.light_level_animation." + configString;
        this.converter = converter;
    }

    public Color4f apply(Color4f original, double distance, double timeMs)
    {
        return this.converter.apply(original, distance, timeMs);
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
    public IConfigOptionListEntry cycle(boolean forward)
    {
        return values()[(this.ordinal() + values().length + (forward ? 1 : -1)) % values().length];
    }

    @Override
    public IConfigOptionListEntry fromString(String name)
    {
        return fromStringStatic(name);
    }

    public static LightLevelAnimation fromStringStatic(String name)
    {
        return Arrays.stream(LightLevelAnimation.values())
                .filter(v -> v.configString.equalsIgnoreCase(name))
                .findFirst()
                .orElse(NONE);
    }

    private interface IColorConverter
    {
        Color4f apply(Color4f original, double distance, double timeMs);
    }
}
