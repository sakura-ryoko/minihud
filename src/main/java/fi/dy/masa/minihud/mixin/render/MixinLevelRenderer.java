package fi.dy.masa.minihud.mixin.render;

import java.util.List;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.minihud.info.InfoLineRenderStats;

@Mixin(WorldRenderer.class)
public abstract class MixinLevelRenderer
{
	@Inject(method = "getEntitiesToRender", at = @At("TAIL"))
	private void minihud_countVisibleEntitiesFix(Camera camera, Frustum frustum, List<Entity> output, CallbackInfoReturnable<Boolean> cir)
	{
		InfoLineRenderStats.INSTANCE.updateEntityCount(output.size());
	}

	@Inject(method = "renderBlockEntities", at = @At("HEAD"))
	private void minihud_countVisibleTileEntities1(MatrixStack matrices, VertexConsumerProvider.Immediate entityVertexConsumers,
	                                               VertexConsumerProvider.Immediate effectVertexConsumers,
	                                               Camera camera, float tickProgress, CallbackInfo ci)
	{
		InfoLineRenderStats.INSTANCE.resetTileEntityCount();
	}

	// fixme, Sodium breaks this
//	@Inject(method = "renderBlockEntities",
//	        at = @At(value = "INVOKE",
//	                 target = "Lnet/minecraft/client/render/block/entity/BlockEntityRenderDispatcher;render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V",
//	                 ordinal = 0)
//	)
//	private void minihud_countVisibleTileEntities2(MatrixStack matrices, VertexConsumerProvider.Immediate entityVertexConsumers,
//	                                               VertexConsumerProvider.Immediate effectVertexConsumers,
//	                                               Camera camera, float tickProgress, CallbackInfo ci)
//	{
//		InfoLineRenderStats.INSTANCE.incrementTileEntityCount();
//	}
//
//	@Inject(method = "renderBlockEntities",
//	        at = @At(value = "INVOKE",
//	                 target = "Lnet/minecraft/client/render/block/entity/BlockEntityRenderDispatcher;render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V",
//	                 ordinal = 1)
//	)
//	private void minihud_countVisibleTileEntities3(MatrixStack matrices, VertexConsumerProvider.Immediate entityVertexConsumers,
//	                                               VertexConsumerProvider.Immediate effectVertexConsumers,
//	                                               Camera camera, float tickProgress, CallbackInfo ci)
//	{
//		InfoLineRenderStats.INSTANCE.incrementTileEntityCount();
//	}
}
