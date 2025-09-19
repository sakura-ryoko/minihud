package fi.dy.masa.minihud.mixin.world;

import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.WorldProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.minihud.data.HudDataManager;

@Mixin(ServerWorld.class)
public class MixinServerWorld
{
//    @Shadow private int spawnChunkRadius;

    @Inject(method = "setSpawnPoint", at = @At("TAIL"))
    private void minihud_setSpawnPos(WorldProperties.SpawnPoint spawnPoint, CallbackInfo ci)
    {
//		MiniHUD.LOGGER.error("minihud_checkSpawnPos() [ServerWorld] --> [{}]", spawnPoint.globalPos().toString());
        HudDataManager.getInstance().setWorldSpawn(spawnPoint.globalPos());
//        HudDataManager.getInstance().setSpawnChunkRadius(this.spawnChunkRadius - 1, true);
    }

    // NOTE:  This is only valid when `doWeatherCycle` is enabled in the Game Rules.
    @Inject(method = "tickWeather()V", at = @At(value = "INVOKE",
                                                target = "Lnet/minecraft/world/level/ServerWorldProperties;setRaining(Z)V"))
    private void minihud_onTickWeather(CallbackInfo ci,
                                       @Local(ordinal = 0) int i, @Local(ordinal = 1) int j, @Local(ordinal = 2) int k,
                                       @Local(ordinal = 1) boolean bl2, @Local(ordinal = 2) boolean bl3)
    {
        /*
        this.worldProperties.setThunderTime(j);
        this.worldProperties.setRainTime(k);
        this.worldProperties.setClearWeatherTime(i);
        this.worldProperties.setThundering(bl2);
        this.worldProperties.setRaining(bl3);
         */

//        MiniHUD.LOGGER.error("ThunderTime: [{}], RainTime: [{}], ClearTime: [{}], isThunder: [{}], isRain: [{}]", j, k, i, bl2, bl3);
        HudDataManager.getInstance().onServerWeatherTick(i, k, j, bl3, bl2);
    }
}
