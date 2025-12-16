package fi.dy.masa.minihud.mixin.render;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.state.WorldRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.minihud.info.InfoLineRenderStats;

@Mixin(WorldRenderer.class)
public abstract class MixinLevelRenderer
{
	@Shadow @Final private WorldRenderState worldRenderState;

	@Inject(method = "fillEntityRenderStates", at = @At("TAIL"))
	private void minihud_countVisibleEntitiesFix(Camera camera, Frustum frustum, RenderTickCounter tickCounter, WorldRenderState renderStates, CallbackInfo ci)
	{
		InfoLineRenderStats.INSTANCE.updateEntityCount(this.worldRenderState.entityRenderStates.size());
	}

	@Inject(method = "fillBlockEntityRenderStates", at = @At("TAIL"))
	private void minihud_countVisibleTileEntities(Camera camera, float tickProgress, WorldRenderState renderStates, CallbackInfo ci)
	{
		InfoLineRenderStats.INSTANCE.updateTileEntityCount(this.worldRenderState.blockEntityRenderStates.size());
	}
}
