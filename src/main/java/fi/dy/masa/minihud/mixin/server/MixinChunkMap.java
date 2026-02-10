package fi.dy.masa.minihud.mixin.server;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.minihud.util.IServerChunkLoading;

@Mixin(ChunkMap.class)
public abstract class MixinChunkMap implements IServerChunkLoading
{
	@Unique private final AtomicInteger totalLoadedCount = new AtomicInteger();

	@Override
	public int minihud_getTotalLoadedChunksCount()
	{
		return this.totalLoadedCount.get();
	}

	// This replaces the now-removed vanilla functionality.
	@Inject(method = "prepareTickingChunk", at = @At("RETURN"))
	private void minihud_countTotalChunks(ChunkHolder holder, CallbackInfoReturnable<CompletableFuture<ChunkResult<LevelChunk>>> cir)
	{
		cir.getReturnValue().handle(
				(chunk, throwable) ->
				{
					this.totalLoadedCount.getAndIncrement();
					return null;
				});
	}
}
