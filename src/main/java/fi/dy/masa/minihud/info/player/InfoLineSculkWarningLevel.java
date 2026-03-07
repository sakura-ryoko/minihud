package fi.dy.masa.minihud.info.player;

import fi.dy.masa.malilib.util.data.Constants;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.data.EntitiesDataManager;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class InfoLineSculkWarningLevel extends InfoLine
{
    private static final String LEVEL_KEY = Reference.MOD_ID+".info_line.sculk_warning_level";

    public InfoLineSculkWarningLevel(InfoToggle type)
    {
        super(type);
    }

    public InfoLineSculkWarningLevel()
    {
        super(InfoToggle.SCULK_WARNING_LEVEL);
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
        if (world instanceof ServerLevel serverLevel)
        {
            List<ServerPlayer> players = serverLevel.getPlayers(it -> it.getId() == ent.getId());

            if (players.isEmpty())
            {
                return null;
            }
            
            return players
                .getFirst()
                .getWardenSpawnTracker()
                .map(it -> this.generateEntry(it.getWarningLevel()))
                .orElse(null);
        }
        else
        {
            Pair<Entity, CompoundData> pair = EntitiesDataManager.getInstance().requestEntity(world, ent.getId());

            if (pair != null)
            {
                CompoundData compound = pair.getRight();

                if (compound.contains("warden_spawn_tracker", Constants.NBT.TAG_COMPOUND))
                {
                    int warningLevel = compound.getCompound("warden_spawn_tracker").getInt("warning_level");
                    return this.generateEntry(warningLevel);
                }
            }
        }

		return null;
    }

    private List<Entry> generateEntry(int warningLevel)
    {
        char color = switch (warningLevel)
        {
            case 0 -> 'a';
            case 1 -> 'e';
            case 2 -> '6';
            case 3, 4 -> 'c';
            default -> 'r';
        };
        //noinspection DataFlowIssue
        return List.of(this.translate(LEVEL_KEY, "§%s%s§r".formatted(color, warningLevel)));
    }
}
