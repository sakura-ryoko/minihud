package fi.dy.masa.minihud.info.player;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

public class InfoLinePing extends InfoLine
{
    private static final String PING_KEY = Reference.MOD_ID+".info_line.ping";

    public InfoLinePing(InfoToggle type)
    {
        super(type);
    }

    public InfoLinePing()
    {
        super(InfoToggle.PING);
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

		if (ent instanceof ClientPlayerEntity player)
		{
			PlayerListEntry info = player.networkHandler.getPlayerListEntry(player.getUuid());

			if (info != null)
			{
				list.add(this.translate(PING_KEY, info.getLatency()));

				return list;
			}
		}

		return null;
    }
}
