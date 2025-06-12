package fi.dy.masa.minihud.info.generic;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.world.ServerWorld;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;

public class InfoLineLoadedChunks extends InfoLine
{
    private static final String CHUNKS_KEY = Reference.MOD_ID+".info_line.loaded_chunks_count";

    public InfoLineLoadedChunks(InfoToggle type)
    {
        super(type);
    }

    public InfoLineLoadedChunks()
    {
        this(InfoToggle.LOADED_CHUNKS_COUNT);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@Nonnull Context ctx)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        List<Entry> list = new ArrayList<>();

        if (mc.world == null)
        {
            return null;
        }

        String chunksClient = mc.world.asString();

        if (ctx.world() instanceof ServerWorld sw)
        {
            int chunksServer = sw.getChunkManager().getLoadedChunkCount();
            int chunksServerTot = sw.getChunkManager().getTotalChunksLoadedCount();
            list.add(this.translate(CHUNKS_KEY+".server", chunksServer, chunksServerTot, chunksClient));
        }
        else
        {
            list.add(this.of(chunksClient));
        }

        return list;
    }
}
