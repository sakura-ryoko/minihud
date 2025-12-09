package fi.dy.masa.minihud.mixin.network;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import fi.dy.masa.minihud.util.DataStorage;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinClientPlayerInteractionManager
{
    @Shadow @Final private net.minecraft.client.Minecraft minecraft;

    @Inject(method = "destroyBlock", at = @At(value = "INVOKE",
                target = "Lnet/minecraft/world/level/block/Block;destroy(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void countBlockBreakingSpeed(net.minecraft.core.BlockPos pos, CallbackInfoReturnable<Boolean> cir)
    {
        DataStorage.getInstance().onPlayerBlockBreak(this.minecraft);
    }
}
