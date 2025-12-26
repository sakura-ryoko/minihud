package fi.dy.masa.minihud.info.chunk;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jetbrains.annotations.NotNull;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineChunkCache;
import fi.dy.masa.minihud.info.InfoLineContext;

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
    public List<Entry> parse(@NotNull InfoLineContext ctx)
    {
        if (ctx.world() == null) return null;

        if (ctx.pos() != null && ctx.chunkPos() != null)
        {
	        List<Entry> list = new ArrayList<>();

	        LevelChunk clientChunk = InfoLineChunkCache.INSTANCE.getClientChunk(ctx.chunkPos());

	        if (!clientChunk.isEmpty())
	        {
		        LevelLightEngine lightingProvider = ctx.world().getChunkSource().getLightEngine();

		        list.add(this.translate(BLOCK_KEY,
		                                lightingProvider.getLayerListener(LightLayer.BLOCK)
		                                                .getLightValue(ctx.pos()))
		        );

		        return list;
	        }
        }

		return null;
    }
}
