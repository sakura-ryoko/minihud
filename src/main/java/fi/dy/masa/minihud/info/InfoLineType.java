package fi.dy.masa.minihud.info;

import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;

import fi.dy.masa.minihud.config.InfoToggle;

public class InfoLineType<T extends InfoLine>
{
    private final Builder<? extends T> builder;
    private final InfoToggle type;
    private final ImmutableList<InfoLineFlag> flags;
    private final ImmutableList<String> group;

    public static <T extends InfoLine> InfoLineType<T> build(Builder<? extends T> builder, InfoToggle type, ImmutableList<InfoLineFlag> flags, ImmutableList<String> group)
    {
        return new InfoLineType<>(builder, type, flags, group);
    }

    public InfoLineType(Builder<? extends T> builder, InfoToggle type, ImmutableList<InfoLineFlag> flags, ImmutableList<String> group)
    {
        this.builder = builder;
        this.type = type;
        this.flags = ImmutableList.copyOf(flags);
        this.group = ImmutableList.copyOf(group);
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

    public ImmutableList<InfoLineFlag> getFlags() { return this.flags; }

    public ImmutableList<String> getGroup() { return this.group; }

    @FunctionalInterface
    public interface Builder<T extends InfoLine>
    {
        T build(InfoToggle type);
    }
}
