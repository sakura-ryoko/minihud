package fi.dy.masa.minihud.util;

import javax.annotation.Nonnull;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum CopperAgingMode implements IConfigOptionListEntry, StringRepresentable
{
	NONE			("none",			"minihud.label.copper_aging.none"),
	MC_TIME			("mc_time",		"minihud.label.copper_aging.mc_time"),
	REAL_TIME		("real_time",		"minihud.label.copper_aging.real_time"),
	TICKS			("ticks",			"minihud.label.copper_aging.ticks"),
	MC_REAL			("mc_real", 		"minihud.label.copper_aging.mc_real"),
	MC_TICKS		("mc_ticks",		"minihud.label.copper_aging.mc_ticks"),
	REAL_TICKS		("real_ticks",	"minihud.label.copper_aging.real_ticks"),
	ALL				("all",			"minihud.label.copper_aging.all"),
	;

	public static final StringRepresentable.EnumCodec<CopperAgingMode> CODEC = StringRepresentable.fromEnum(CopperAgingMode::values);
	public static final StreamCodec<ByteBuf, CopperAgingMode> PACKET_CODEC = ByteBufCodecs.STRING_UTF8.map(CopperAgingMode::fromStringStatic, CopperAgingMode::getSerializedName);
	public static final ImmutableList<@NotNull CopperAgingMode> VALUES = ImmutableList.copyOf(values());

	private final String configString;
	private final String translationKey;

	CopperAgingMode(String name, String translationKey)
	{
		this.configString = name;
		this.translationKey = translationKey;
	}

	@Override
	public @Nonnull String getSerializedName()
	{
		return this.configString;
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
	public CopperAgingMode fromString(String name)
	{
		return fromStringStatic(name);
	}

	public static CopperAgingMode fromStringStatic(String name)
	{
		for (CopperAgingMode val : VALUES)
		{
			if (val.configString.equalsIgnoreCase(name))
			{
				return val;
			}
		}

		return CopperAgingMode.NONE;
	}
}
