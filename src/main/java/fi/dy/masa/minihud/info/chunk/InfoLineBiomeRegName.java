package fi.dy.masa.minihud.info.chunk;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineChunkCache;

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
    public List<Entry> parse(@NotNull InfoLine.Context ctx)
    {
        if (ctx.world() == null) return null;

	    if (ctx.pos() != null && ctx.chunkPos() != null)
	    {
		    List<Entry> list = new ArrayList<>();
		    WorldChunk clientChunk = InfoLineChunkCache.INSTANCE.getClientChunk(ctx.chunkPos());

		    if (clientChunk.isEmpty() == false)
		    {
			    Biome biome = this.getClientWorld().getBiome(ctx.pos()).value();
			    Identifier rl = this.getClientWorld().getRegistryManager().getOrThrow(RegistryKeys.BIOME).getId(biome);
			    String name = rl != null ? rl.toString() : "?";

			    list.add(this.translate(BIOME_KEY, name));

			    return list;
		    }
	    }

	    return null;
    }
}
