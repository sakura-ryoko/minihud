package fi.dy.masa.minihud.info.world;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
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
    public List<Entry> parse(@Nonnull Context ctx)
    {
        if (ctx.world() == null || ctx.pos() == null) return null;
        List<Entry> list = new ArrayList<>();

        if (MiscUtils.isOverworld(ctx.world()) == false)
        {
            return null;
        }

        String result;

        if (this.getHudData().isWorldSeedKnown(ctx.world()))
        {
            long seed = this.getHudData().getWorldSeed(ctx.world());

            if (MiscUtils.canSlimeSpawnAt(ctx.pos().getX(), ctx.pos().getZ(), seed))
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
