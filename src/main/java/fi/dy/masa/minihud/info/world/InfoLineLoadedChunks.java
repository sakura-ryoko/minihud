package fi.dy.masa.minihud.info.world;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;
import fi.dy.masa.minihud.util.IServerChunkLoading;

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
    public List<Entry> parse(@Nonnull InfoLineContext ctx)
    {
        if (this.getClientWorld() == null)
        {
            return null;
        }

        return this.parseWorld(ctx.world() == null ? this.getClientWorld() : ctx.world());
    }

    @Override
    public List<Entry> parseWorld(@Nonnull Level world)
    {
        List<Entry> list = new ArrayList<>();
        String chunksClient = this.getClientWorld().gatherChunkSourceStats();

        if (world instanceof ServerLevel sw)
        {
            int chunksServer = sw.getChunkSource().getLoadedChunksCount();
            int chunksServerTot = ((IServerChunkLoading) sw.getChunkSource().chunkMap).minihud_getTotalLoadedChunksCount();
            list.add(this.translate(CHUNKS_KEY+".server", chunksServer, chunksServerTot, chunksClient));
        }
        else
        {
            list.add(this.of(chunksClient));
        }

        return list;
    }
}
