package fi.dy.masa.minihud.mixin;

import java.util.function.BooleanSupplier;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.minihud.data.HudDataManager;
import fi.dy.masa.minihud.util.DebugInfoUtils;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer
{
    @Inject(method = "tick", at = @At("TAIL"))
    public void onServerTickPost(BooleanSupplier supplier, CallbackInfo ci)
    {
        DebugInfoUtils.onServerTickEnd((MinecraftServer) (Object) this);
    }

    @Inject(method = "prepareStartRegion", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/math/MathHelper;square(I)I", shift = At.Shift.BEFORE)
    )
    private void onPrepareStartRegion(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci,
                                      @Local BlockPos blockPos, @Local int i)
    {
        HudDataManager.getInstance().setWorldSpawn(blockPos);
        HudDataManager.getInstance().setSpawnChunkRadius(i, true);
    }
}
