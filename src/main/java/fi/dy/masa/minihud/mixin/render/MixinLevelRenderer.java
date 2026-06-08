package fi.dy.masa.minihud.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.malilib.compat.iris.IrisCompat;
import fi.dy.masa.minihud.info.InfoLineRenderStats;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer
{
	@Shadow @Final private LevelRenderState levelRenderState;

	@Inject(method = "submitBlockEntities", at = @At("HEAD"))
	private void minihud_countVisibleTileEntities_withSodium(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector, CallbackInfo ci)
	{
		if (IrisCompat.hasSodium())
		{
			InfoLineRenderStats.INSTANCE.updateTileEntityCount(this.levelRenderState.blockEntityRenderStates.size());
		}
	}
}
