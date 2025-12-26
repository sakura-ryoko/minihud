package fi.dy.masa.minihud.info.player;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
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
    public List<Entry> parseEnt(@NotNull Level world, @NotNull Entity ent)
    {
        List<Entry> list = new ArrayList<>();

		if (ent instanceof LocalPlayer player)
		{
			PlayerInfo info = player.connection.getPlayerInfo(player.getUUID());

			if (info != null)
			{
				list.add(this.translate(PING_KEY, info.getLatency()));

				return list;
			}
		}

		return null;
    }
}
