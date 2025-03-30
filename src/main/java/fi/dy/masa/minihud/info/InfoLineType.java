package fi.dy.masa.minihud.info;

import javax.annotation.Nullable;

import fi.dy.masa.minihud.config.InfoToggle;

public class InfoLineType<T extends InfoLine>
{
    private final Builder<? extends T> builder;
    private final InfoToggle type;

    public static <T extends InfoLine> InfoLineType<T> build(Builder<? extends T> builder, InfoToggle type)
    {
        return new InfoLineType<>(builder, type);
    }

    public InfoLineType(Builder<? extends T> builder, InfoToggle type)
    {
        this.builder = builder;
        this.type = type;
    }

    @Nullable
    public T init(InfoToggle type)
    {
        return this.builder.build(type);
    }

    public InfoToggle getType()
    {
        return this.type;
    }

    @FunctionalInterface
    public interface Builder<T extends InfoLine>
    {
        T build(InfoToggle type);
    }
}
