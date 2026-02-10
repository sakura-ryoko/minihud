package fi.dy.masa.minihud.mixin.server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.minihud.data.HudDataManager;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer
{
	@Shadow public abstract LevelData.RespawnData getRespawnData();

	// TODO really not needed
//	@Inject(method = "tick", at = @At("TAIL"))
//    public void minihud_onServerTickPost(BooleanSupplier supplier, CallbackInfo ci)
//    {
//        DebugInfoUtils.onServerTickEnd((MinecraftServer) (Object) this);
//    }


	@Inject(method = "prepareLevels",
			at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;updateMobSpawningFlags()V"
			         // shift = At.Shift.BEFORE
			)
    )
    private void minihud_onPrepareStartRegion(CallbackInfo ci)
    {
//		MiniHUD.LOGGER.error("minihud_onPrepareStartRegion() [StartRegion] --> [{}]", this.getSpawnPos().toString());
		HudDataManager.getInstance().setWorldSpawn(this.getRespawnData().globalPos());
//        HudDataManager.getInstance().setSpawnChunkRadius(i, true);
    }
}
