package fi.dy.masa.minihud.info.entity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WeatheringCopper;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import fi.dy.masa.malilib.util.data.DataEntityUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;
import fi.dy.masa.minihud.mixin.entity.IMixinCopperGolemEntity;
import fi.dy.masa.minihud.util.CopperAgingMode;
import fi.dy.masa.minihud.util.MiscUtils;

public class InfoLineCopperAging extends InfoLine
{
    private static final String COPPER_KEY = Reference.MOD_ID+".info_line.entity_copper_aging";

    public InfoLineCopperAging(InfoToggle type)
    {
        super(type);
    }

    public InfoLineCopperAging()
    {
        super(InfoToggle.ENTITY_COPPER_AGING);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@NotNull InfoLineContext ctx)
    {
        if (ctx.world() == null) return null;

        if (ctx.hasLiving() && ctx.hasData())
        {
            EntityType<?> entityType = DataEntityUtils.getEntityType(ctx.data());
            if (entityType == null) return null;

            return this.parseData(ctx.world(), entityType, ctx.data());
        }

        return ctx.ent() != null ? this.parseEnt(ctx.world(), ctx.ent()) : null;
    }

    @Override
    public List<Entry> parseData(@NotNull Level world, @NotNull EntityType<?> entityType, @NotNull CompoundData data)
    {
        List<Entry> list = new ArrayList<>();

		if (entityType.equals(EntityType.COPPER_GOLEM))
		{
			Pair<WeatheringCopper.WeatherState, Long> pair = DataEntityUtils.getWeatheringData(data);
			WeatheringCopper.WeatherState level = pair.getLeft();
			final long age = pair.getRight();

			// Waxed (-2L)
			if (age == -2L)
			{
				list.add(this.translate(COPPER_KEY + ".waxed",
				                        level.getSerializedName()
				));
			}
			else if (age == -1)
			{
				list.add(this.translate(COPPER_KEY + ".not_aging",
				                        level.getSerializedName()
				));
			}
			else
			{
				final long diff = (world.getDayTime() - age) * -1;
				final String formatted = this.formatCountdown(diff);

				if (formatted.isEmpty())
				{
					list.add(this.translate(COPPER_KEY + ".not_aging",
					                        level.getSerializedName()
					));
				}
				else
				{
					list.add(this.translate(COPPER_KEY + ".aging",
					                        level.getSerializedName(),
					                        formatted
					));
				}
			}
		}

        return list;
    }

    @Override
    public List<Entry> parseEnt(@NotNull Level world, @NotNull Entity ent)
    {
        List<Entry> list = new ArrayList<>();

        if (ent instanceof CopperGolem cge)
        {
			WeatheringCopper.WeatherState level = cge.getWeatherState();
			final long age = ((IMixinCopperGolemEntity) cge).minihud_getNextOxidationAge();

			// Waxed (-2L)
			if (age == -2L)
			{
				list.add(this.translate(COPPER_KEY+".waxed",
										level.getSerializedName()
				));
			}
			else if (age == -1)
			{
				list.add(this.translate(COPPER_KEY+".not_aging",
										level.getSerializedName()
				));
			}
			else
			{
				final long diff = (world.getDayTime() - age) * -1;
				final String formatted = this.formatCountdown(diff);

				if (formatted.isEmpty())
				{
					list.add(this.translate(COPPER_KEY+".not_aging",
											level.getSerializedName()
					));
				}
				else
				{
					list.add(this.translate(COPPER_KEY + ".aging",
											level.getSerializedName(),
											formatted
					));
				}
			}
        }

        return list;
    }

	/**
	 * Formats the Time Remaining in various formats using COPPER_AGING_MODE
	 * @param remaining ()
	 * @return ()
	 */
	private String formatCountdown(final long remaining)
	{
		if (remaining <= 0)
		{
			return "";
		}

		final long diffMillis = (long) ((remaining * 3.6) * 1000L) / 60;		// Real time
		final long day = (int) (remaining / 24000);
		// 1 tick = 3.6 seconds in MC (0.2777... seconds IRL)
		final int dayTicks = (int) (remaining % 24000);
		final int hour = (int) ((dayTicks / 1000) + 6) % 24;
		final int min = (int) (dayTicks / 16.666666) % 60;
		final int sec = (int) (dayTicks / 0.277777) % 60;
		final String gameTime = this.qt(COPPER_KEY+".game_time",
										String.format("%d", day),
										String.format("%02d", hour),
										String.format("%02d", min),
										String.format("%02d", sec));
		final String realTime = this.qt(COPPER_KEY+".real_time", MiscUtils.formatDuration(diffMillis));
		final String tickTime = this.qt(COPPER_KEY+".ticks", String.valueOf(remaining));
		final CopperAgingMode mode = (CopperAgingMode) Configs.Generic.COPPER_AGING_MODE.getOptionListValue();

		StringBuilder result = new StringBuilder();

		switch (mode)
		{
			case ALL -> result.append(gameTime).append(" [").append(tickTime).append("] / ").append(realTime);
			case REAL_TICKS -> result.append("[").append(tickTime).append("] / ").append(realTime);
			case MC_TICKS -> result.append(gameTime).append(" [").append(tickTime).append("]");
			case MC_REAL -> result.append(gameTime).append(" / ").append(realTime);
			case MC_TIME -> result.append(gameTime);
			case REAL_TIME -> result.append(realTime);
			case TICKS -> result.append(tickTime);
		}

		return result.toString();
	}
}
