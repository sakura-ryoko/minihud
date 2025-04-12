package fi.dy.masa.minihud.mixin.server;

import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.GameInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.minihud.data.HudDataManager;

@Mixin(GameInstance.class)
public class MixinGameInstance
{
    @Inject(method = "prepareStartRegion", at = @At(value = "INVOKE",
                                                    target = "Lnet/minecraft/util/math/MathHelper;square(I)I", shift = At.Shift.BEFORE)
    )
    private void onPrepareStartRegion(MinecraftServer server, WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci,
                                      @Local BlockPos blockPos, @Local int i)
    {
        HudDataManager.getInstance().setWorldSpawn(blockPos);
        HudDataManager.getInstance().setSpawnChunkRadius(i, true);
    }
}
