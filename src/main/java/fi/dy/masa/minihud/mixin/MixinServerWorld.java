package fi.dy.masa.minihud.mixin;

import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.minihud.util.DataStorage;

@Mixin(ServerWorld.class)
public class MixinServerWorld
{
    @Shadow private int spawnChunkRadius;

    @Inject(method = "setSpawnPos", at = @At("TAIL"))
    private void minihud_checkSpawnPos(BlockPos pos, float angle, CallbackInfo ci)
    {
        DataStorage.getInstance().setWorldSpawn(pos);
        DataStorage.getInstance().setSpawnChunkRadius(this.spawnChunkRadius - 1, true);
    }

    @Inject(method = "tickWeather()V", at = @At(value = "INVOKE",
                                                target = "Lnet/minecraft/world/level/ServerWorldProperties;setRaining(Z)V"))
    private void minihud_onTickWeather(CallbackInfo ci,
                                      @Local(ordinal = 0) int i, @Local(ordinal = 1) int j, @Local(ordinal = 2) int k,
                                      @Local(ordinal = 1) boolean bl2)
    {
        DataStorage.getInstance().onServerWeatherTick(i, bl2 ? j : k, bl2);
    }
}
