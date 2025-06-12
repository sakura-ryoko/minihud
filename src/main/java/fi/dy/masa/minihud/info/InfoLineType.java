package fi.dy.masa.minihud.info;

import java.util.List;
import javax.annotation.Nullable;

import fi.dy.masa.minihud.config.InfoToggle;

public class InfoLineType<T extends InfoLine>
{
    private final Builder<? extends T> builder;
    private final InfoToggle type;
    private final List<InfoLineFlag> flags;

    public static <T extends InfoLine> InfoLineType<T> build(Builder<? extends T> builder, InfoToggle type, List<InfoLineFlag> flags)
    {
        return new InfoLineType<>(builder, type, flags);
    }

    public InfoLineType(Builder<? extends T> builder, InfoToggle type, List<InfoLineFlag> flags)
    {
        this.builder = builder;
        this.type = type;
        this.flags = flags;
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

    public List<InfoLineFlag> getFlags() { return this.flags; }

    @FunctionalInterface
    public interface Builder<T extends InfoLine>
    {
        T build(InfoToggle type);
    }
}
