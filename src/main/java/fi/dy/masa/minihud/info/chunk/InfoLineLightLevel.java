package fi.dy.masa.minihud.info.chunk;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.LightType;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineChunkCache;

public class InfoLineLightLevel extends InfoLine
{
    private static final String BLOCK_KEY = Reference.MOD_ID+".info_line.light_level";

    public InfoLineLightLevel(InfoToggle type)
    {
        super(type);
    }

    public InfoLineLightLevel()
    {
        super(InfoToggle.LIGHT_LEVEL);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@NotNull InfoLine.Context ctx)
    {
        if (ctx.world() == null) return null;

        if (ctx.pos() != null && ctx.chunkPos() != null)
        {
	        List<Entry> list = new ArrayList<>();

	        WorldChunk clientChunk = InfoLineChunkCache.INSTANCE.getClientChunk(ctx.chunkPos());

	        if (!clientChunk.isEmpty())
	        {
		        LightingProvider lightingProvider = ctx.world().getChunkManager().getLightingProvider();

		        list.add(this.translate(BLOCK_KEY,
		                                lightingProvider.get(LightType.BLOCK)
		                                                .getLightLevel(ctx.pos()))
		        );

		        return list;
	        }
        }

		return null;
    }
}
