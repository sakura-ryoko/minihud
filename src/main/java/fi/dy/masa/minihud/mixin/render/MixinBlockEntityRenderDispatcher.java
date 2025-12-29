package fi.dy.masa.minihud.mixin.render;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.minihud.info.InfoLineRenderStats;

@Mixin(BlockEntityRenderDispatcher.class)
public class MixinBlockEntityRenderDispatcher
{
	@Inject(method = "render(Lnet/minecraft/client/render/block/entity/BlockEntityRenderer;Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/util/math/Vec3d;)V",
	        at = @At("RETURN"))
	private static <T extends BlockEntity> void minihud_countTileEntities2(BlockEntityRenderer<T> renderer, T blockEntity, float tickProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, Vec3d cameraPos, CallbackInfo ci)
	{
		InfoLineRenderStats.INSTANCE.incrementTileEntityCount();
	}
}
