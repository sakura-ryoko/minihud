package fi.dy.masa.minihud.mixin.world;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.minihud.data.HudDataManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelData;

@Mixin(ServerLevel.class)
public class MixinServerLevel
{
//    @Shadow private int spawnChunkRadius;

    @Inject(method = "setRespawnData", at = @At("TAIL"))
    private void minihud_setSpawnPos(LevelData.RespawnData respawnData, CallbackInfo ci)
    {
//		MiniHUD.LOGGER.error("minihud_checkSpawnPos() [ServerWorld] --> [{}]", spawnPoint.globalPos().toString());
        HudDataManager.getInstance().setWorldSpawn(respawnData.globalPos());
//        HudDataManager.getInstance().setSpawnChunkRadius(this.spawnChunkRadius - 1, true);
    }

    // NOTE:  This is only valid when `doWeatherCycle` is enabled in the Game Rules.
    @Inject(method = "advanceWeatherCycle", at = @At(value = "INVOKE",
                                                target = "Lnet/minecraft/world/level/saveddata/WeatherData;setRaining(Z)V"))
    private void minihud_onTickWeather(CallbackInfo ci,
                                       @Local(name = "clearWeatherTime") int clearWeatherTime,
                                       @Local(name = "thunderTime") int thunderTime,
                                       @Local(name = "rainTime") int rainTime,
                                       @Local(name = "thundering") boolean thundering,
                                       @Local(name = "raining") boolean raining)
    {
//        MiniHUD.LOGGER.error("ThunderTime: [{}], RainTime: [{}], ClearTime: [{}], isThunder: [{}], isRain: [{}]", j, k, i, bl2, bl3);
        HudDataManager.getInstance().onServerWeatherTick(clearWeatherTime, rainTime, thunderTime, raining, thundering);
    }
}
