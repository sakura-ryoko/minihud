package fi.dy.masa.minihud.info.chunk;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.NotNull;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineChunkCache;
import fi.dy.masa.minihud.info.InfoLineContext;

public class InfoLineBiomeRegName extends InfoLine
{
    private static final String BIOME_KEY = Reference.MOD_ID+".info_line.biome_reg_name";

    public InfoLineBiomeRegName(InfoToggle type)
    {
        super(type);
    }

    public InfoLineBiomeRegName()
    {
        super(InfoToggle.BIOME_REG_NAME);
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
			    Biome biome = this.getClientWorld().getBiome(ctx.pos()).value();
			    Identifier rl = this.getClientWorld().registryAccess().lookupOrThrow(Registries.BIOME).getKey(biome);
			    String name = rl != null ? rl.toString() : "?";

			    list.add(this.translate(BIOME_KEY, name));

			    return list;
		    }
	    }

	    return null;
    }
}
