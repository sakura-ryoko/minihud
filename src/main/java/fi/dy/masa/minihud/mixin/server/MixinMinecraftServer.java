package fi.dy.masa.minihud.mixin.server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.GlobalPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.minihud.data.HudDataManager;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer
{
	@Shadow public abstract GlobalPos getSpawnPos();

	// TODO really not needed
//	@Inject(method = "tick", at = @At("TAIL"))
//    public void minihud_onServerTickPost(BooleanSupplier supplier, CallbackInfo ci)
//    {
//        DebugInfoUtils.onServerTickEnd((MinecraftServer) (Object) this);
//    }

    @Inject(method = "prepareStartRegion",
			at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;updateMobSpawnOptions()V",
			shift = At.Shift.BEFORE)
    )
    private void minihud_onPrepareStartRegion(CallbackInfo ci)
    {
        HudDataManager.getInstance().setWorldSpawn(this.getSpawnPos().pos());
//        HudDataManager.getInstance().setSpawnChunkRadius(i, true);
    }
}
