package fi.dy.masa.minihud.info.chunk;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

public class InfoLineBlockInChunk extends InfoLine
{
    private static final String BLOCK_KEY = Reference.MOD_ID+".info_line.block_in_chunk";

    public InfoLineBlockInChunk(InfoToggle type)
    {
        super(type);
    }

    public InfoLineBlockInChunk()
    {
        super(InfoToggle.BLOCK_IN_CHUNK);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@NotNull InfoLineContext ctx)
    {
        if (ctx.world() == null) return null;

        if (ctx.pos() != null && ctx.chunkPos() != null)
        {
	        List<Entry> list = new ArrayList<>();

	        list.add(this.translate(BLOCK_KEY,
	                                ctx.pos().getX() & 0xF,
	                                ctx.pos().getY() & 0xF,
	                                ctx.pos().getZ() & 0xF,
	                                ctx.chunkPos().x,
	                                ctx.pos().getY() >> 4,
	                                ctx.chunkPos().z)
	        );

	        return list;
        }

		return null;
    }
}
