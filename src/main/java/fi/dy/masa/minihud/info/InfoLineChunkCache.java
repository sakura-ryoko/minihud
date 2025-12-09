package fi.dy.masa.minihud.info;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import fi.dy.masa.minihud.util.DataStorage;

public class InfoLineChunkCache
{
    public static final InfoLineChunkCache INSTANCE = new InfoLineChunkCache();
    private final Map<ChunkPos, CompletableFuture<ChunkResult<ChunkAccess>>> chunkFutures = new HashMap<>();
    @Nullable private LevelChunk cachedClientChunk;
    private Minecraft mc;

    private InfoLineChunkCache()
    {
        this.mc = Minecraft.getInstance();
    }

    private @Nullable Level getClientWorld()
    {
        if (this.mc == null)
        {
            this.mc = Minecraft.getInstance();
        }

        if (this.mc.level != null)
        {
            return this.mc.level;
        }

        return null;
    }

    private DataStorage getData()
    {
        return DataStorage.getInstance();
    }

    public void onUpdate()
    {
        if (this.chunkFutures.size() >= 4)
        {
            this.resetCachedChunks();
        }
    }

    public void onReset()
    {
        this.resetCachedChunks();
    }

    @Nullable
    public LevelChunk getChunk(ChunkPos chunkPos)
    {
        CompletableFuture<ChunkResult<ChunkAccess>> future = this.chunkFutures.get(chunkPos);

        if (future == null)
        {
            future = this.setupChunkFuture(chunkPos);
        }

        ChunkResult<ChunkAccess> chunkResult = future.getNow(null);

        if (chunkResult == null)
        {
            return null;
        }
        else
        {
            ChunkAccess chunk = chunkResult.orElse(null);

            if (chunk instanceof LevelChunk)
            {
                return (LevelChunk) chunk;
            }
            else
            {
                return null;
            }
        }
    }

    public CompletableFuture<ChunkResult<ChunkAccess>> setupChunkFuture(ChunkPos chunkPos)
    {
        IntegratedServer server = this.getData().getIntegratedServer();
        CompletableFuture<ChunkResult<ChunkAccess>> future = null;

        if (server != null)
        {
            ServerLevel world = server.getLevel(Objects.requireNonNull(this.getClientWorld()).dimension());

            if (world != null)
            {
                future = world.getChunkSource()
                              .getChunkFuture(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false)
                              .thenApply((either) -> either.map((chunk) -> (LevelChunk) chunk) );
            }
        }

        if (future == null)
        {
            future = CompletableFuture.completedFuture(ChunkResult.of(this.getClientChunk(chunkPos)));
        }

        this.chunkFutures.put(chunkPos, future);

        return future;
    }

    public LevelChunk getClientChunk(ChunkPos chunkPos)
    {
        if (this.cachedClientChunk == null || !this.cachedClientChunk.getPos().equals(chunkPos))
        {
            this.cachedClientChunk = Objects.requireNonNull(this.getClientWorld()).getChunk(chunkPos.x, chunkPos.z);
        }

        return this.cachedClientChunk;
    }

    public void resetCachedChunks()
    {
        this.chunkFutures.clear();
        this.cachedClientChunk = null;
    }
}
