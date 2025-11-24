package fi.dy.masa.minihud.info.chunk;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;

import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineChunkCache;

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
    public List<Entry> parse(@NotNull InfoLine.Context ctx)
    {
        if (ctx.world() == null) return null;

        if (ctx.pos() != null && ctx.chunkPos() != null)
        {
	        List<Entry> list = new ArrayList<>();
			WorldChunk clientChunk = InfoLineChunkCache.INSTANCE.getClientChunk(ctx.chunkPos());

	        if (clientChunk.isEmpty() == false)
	        {
		        Biome biome = this.mc().world.getBiome(ctx.pos()).value();
		        Identifier id = this.mc().world.getRegistryManager().getOrThrow(RegistryKeys.BIOME).getId(biome);
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
