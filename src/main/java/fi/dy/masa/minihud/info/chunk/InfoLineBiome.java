package fi.dy.masa.minihud.info.chunk;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.NotNull;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineChunkCache;
import fi.dy.masa.minihud.info.InfoLineContext;

public class InfoLineBiome extends InfoLine
{
    private static final String BIOME_KEY = Reference.MOD_ID+".info_line.biome";

    public InfoLineBiome(InfoToggle type)
    {
        super(type);
    }

    public InfoLineBiome()
    {
        super(InfoToggle.BIOME);
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

	        if (clientChunk.isEmpty() == false)
	        {
		        Biome biome = this.mc().level.getBiome(ctx.pos()).value();
		        Identifier id = this.mc().level.registryAccess().lookupOrThrow(Registries.BIOME).getKey(biome);
		        String translationKey = "biome." + id.toString().replace(":", ".");
		        String biomeName = StringUtils.translate(translationKey);

		        if (biomeName.equals(translationKey))
		        {
			        biomeName = StringUtils.prettifyRawTranslationPath(id.getPath());
		        }

		        list.add(this.translate(BIOME_KEY, biomeName));

		        return list;
	        }
        }

		return null;
    }
}
