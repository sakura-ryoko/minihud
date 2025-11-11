package fi.dy.masa.minihud.data;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.StringRepresentable;

public enum ServuxDataLogger implements StringRepresentable
{
    TPS             ("tps",             CompoundTag.CODEC),
    MOB_CAPS        ("mob_caps",        CompoundTag.CODEC)
    ;

    public static final EnumCodec<ServuxDataLogger> CODEC = StringRepresentable.fromEnum(ServuxDataLogger::values);
    public static final ImmutableList<ServuxDataLogger> VALUES = ImmutableList.copyOf(values());

    private final String name;
    private final Codec<?> codec;

    ServuxDataLogger(String name, Codec<?> codec)
    {
        this.name = name;
        this.codec = codec;
    }

    @Override
    public @Nonnull String getSerializedName()
    {
        return this.name;
    }

    public Codec<?> codec() { return this.codec; }

    public static @Nullable ServuxDataLogger fromStringStatic(String name)
    {
        for (ServuxDataLogger type : VALUES)
        {
            if (type.name.equalsIgnoreCase(name))
            {
                return type;
            }
        }

        return null;
    }
}
