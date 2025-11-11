package fi.dy.masa.minihud.mixin.server;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.OptionalChunk;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.minihud.util.IServerChunkLoading;

@Mixin(ServerChunkLoadingManager.class)
public abstract class MixinServerChunkLoadingManager implements IServerChunkLoading
{
	@Unique private final AtomicInteger totalLoadedCount = new AtomicInteger();

	@Override
	public int minihud_getTotalLoadedChunksCount()
	{
		return this.totalLoadedCount.get();
	}

	// This replaces the now-removed vanilla functionality.
	@Inject(method = "makeChunkTickable", at = @At("RETURN"))
	private void minihud_countTotalChunks(ChunkHolder holder, CallbackInfoReturnable<CompletableFuture<OptionalChunk<WorldChunk>>> cir)
	{
		cir.getReturnValue().handle(
				(chunk, throwable) ->
				{
					this.totalLoadedCount.getAndIncrement();
					return null;
				});
	}
}
