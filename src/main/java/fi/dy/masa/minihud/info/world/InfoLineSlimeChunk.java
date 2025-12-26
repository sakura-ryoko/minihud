package fi.dy.masa.minihud.info.world;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;
import fi.dy.masa.minihud.util.MiscUtils;

public class InfoLineSlimeChunk extends InfoLine
{
    private static final String SLIME_KEY = Reference.MOD_ID+".info_line.slime_chunk";

    public InfoLineSlimeChunk(InfoToggle type)
    {
        super(type);
    }

    public InfoLineSlimeChunk()
    {
        this(InfoToggle.SLIME_CHUNK);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@Nonnull InfoLineContext ctx)
    {
        if (this.getClientWorld() == null || ctx.pos() == null)
        {
            return null;
        }

        return this.parseBlockPos(ctx.world() == null ? this.getClientWorld() : ctx.world(), ctx.pos());
    }

    @Override
    public List<Entry> parseBlockPos(@Nonnull Level world, @Nonnull BlockPos pos)
    {
        List<Entry> list = new ArrayList<>();

        if (!MiscUtils.isOverworld(world))
        {
            return null;
        }

        String result;

        if (this.getHudData().isWorldSeedKnown(world))
        {
            long seed = this.getHudData().getWorldSeed(world);

            if (MiscUtils.canSlimeSpawnAt(pos.getX(), pos.getZ(), seed))
            {
                result = this.qt(SLIME_KEY+".yes");
            }
            else
            {
                result = this.qt(SLIME_KEY+".no");
            }
        }
        else
        {
            result = this.qt(SLIME_KEY+".no_seed");
        }

        list.add(this.translate(SLIME_KEY, result));

        return list;
    }
}
