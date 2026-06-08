package fi.dy.masa.minihud.mixin.render;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.malilib.compat.iris.IrisCompat;
import fi.dy.masa.minihud.info.InfoLineRenderStats;

@Mixin(LevelExtractor.class)
public abstract class MixinLevelExtractor
{
	@Shadow @Final private LevelRenderState levelRenderState;

	@Inject(method = "extractVisibleEntities", at = @At("TAIL"))
	private void minihud_countVisibleEntitiesFix(Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState output, CallbackInfo ci)
	{
		InfoLineRenderStats.INSTANCE.updateEntityCount(this.levelRenderState.entityRenderStates.size());
	}

	@Inject(method = "extractVisibleBlockEntities", at = @At("TAIL"))
	private void minihud_countVisibleTileEntities_withoutSodium(Camera camera, float deltaPartialTick, LevelRenderState levelRenderState, CallbackInfo ci)
	{
		// Sodium blocks calling this method.  Why?
		if (!IrisCompat.hasSodium())
		{
			InfoLineRenderStats.INSTANCE.updateTileEntityCount(this.levelRenderState.blockEntityRenderStates.size());
		}
	}
}
