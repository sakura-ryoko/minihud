package fi.dy.masa.minihud.info;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.OptionalChunk;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import fi.dy.masa.minihud.util.DataStorage;

public class InfoLineChunkCache
{
    public static final InfoLineChunkCache INSTANCE = new InfoLineChunkCache();
    private final Map<ChunkPos, CompletableFuture<OptionalChunk<Chunk>>> chunkFutures = new HashMap<>();
    @Nullable private WorldChunk cachedClientChunk;
    private MinecraftClient mc;

    private InfoLineChunkCache()
    {
        this.mc = MinecraftClient.getInstance();
    }

    private @Nullable World getClientWorld()
    {
        if (this.mc == null)
        {
            this.mc = MinecraftClient.getInstance();
        }

        if (this.mc.world != null)
        {
            return this.mc.world;
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
    public WorldChunk getChunk(ChunkPos chunkPos)
    {
        CompletableFuture<OptionalChunk<Chunk>> future = this.chunkFutures.get(chunkPos);

        if (future == null)
        {
            future = this.setupChunkFuture(chunkPos);
        }

        OptionalChunk<Chunk> chunkResult = future.getNow(null);

        if (chunkResult == null)
        {
            return null;
        }
        else
        {
            Chunk chunk = chunkResult.orElse(null);

            if (chunk instanceof WorldChunk)
            {
                return (WorldChunk) chunk;
            }
            else
            {
                return null;
            }
        }
    }

    public CompletableFuture<OptionalChunk<Chunk>> setupChunkFuture(ChunkPos chunkPos)
    {
        IntegratedServer server = this.getData().getIntegratedServer();
        CompletableFuture<OptionalChunk<Chunk>> future = null;

        if (server != null)
        {
            ServerWorld world = server.getWorld(Objects.requireNonNull(this.getClientWorld()).getRegistryKey());

            if (world != null)
            {
                future = world.getChunkManager()
                              .getChunkFutureSyncOnMainThread(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false)
                              .thenApply((either) -> either.map((chunk) -> (WorldChunk) chunk) );
            }
        }

        if (future == null)
        {
            future = CompletableFuture.completedFuture(OptionalChunk.of(this.getClientChunk(chunkPos)));
        }

        this.chunkFutures.put(chunkPos, future);

        return future;
    }

    public WorldChunk getClientChunk(ChunkPos chunkPos)
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
