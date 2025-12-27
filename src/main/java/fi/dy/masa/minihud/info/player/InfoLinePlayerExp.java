package fi.dy.masa.minihud.info.player;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

public class InfoLinePlayerExp extends InfoLine
{
    private static final String ENTITY_KEY = Reference.MOD_ID+".info_line.player_experience";

    public InfoLinePlayerExp(InfoToggle type)
    {
        super(type);
    }

    public InfoLinePlayerExp()
    {
        super(InfoToggle.PLAYER_EXPERIENCE);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@NotNull InfoLineContext ctx)
    {
        if (ctx.world() == null) return null;

        return ctx.ent() != null ? this.parseEnt(ctx.world(), ctx.ent()) : null;
    }

    @Override
    public List<Entry> parseEnt(@NotNull World world, @NotNull Entity ent)
    {
        List<Entry> list = new ArrayList<>();

		if (ent instanceof PlayerEntity player)
		{
			list.add(this.translate(ENTITY_KEY,
			                        player.experienceLevel,
			                        100 * player.experienceProgress,
			                        player.totalExperience)
			);

			return list;
		}

		return null;
    }
}
