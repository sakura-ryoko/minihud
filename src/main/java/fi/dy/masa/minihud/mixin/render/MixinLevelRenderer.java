package fi.dy.masa.minihud.mixin.render;

import org.joml.Matrix4f;

import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.minihud.info.InfoLineRenderStats;

@Mixin(WorldRenderer.class)
public abstract class MixinLevelRenderer
{
	@Inject(method = "render", at = @At("HEAD"))
	private void minihud_resetRenderStats(RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci)
	{
		InfoLineRenderStats.INSTANCE.resetEntityCount();
		InfoLineRenderStats.INSTANCE.resetTileEntityCount();
	}

	@Inject(method = "renderEntity", at = @At("TAIL"))
	private void minihud_countVisibleEntitiesFix(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci)
	{
		InfoLineRenderStats.INSTANCE.incrementEntityCount();
	}
}
