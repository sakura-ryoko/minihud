package fi.dy.masa.minihud.mixin.world;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.minihud.util.DataStorage;
import net.minecraft.server.level.DistanceManager;

@Mixin(DistanceManager.class)
public class MixinDistanceManager
{
    @Inject(method = "updateSimulationDistance", at = @At("TAIL"))
    private void minihud_getSimulationDistance(int newDistance, CallbackInfo ci)
    {
        if (newDistance > 0)
        {
            final int simul = DataStorage.getInstance().getSimulationDistance();

            if (simul != newDistance)
            {
                DataStorage.getInstance().setSimulationDistance(newDistance);
            }
        }
    }
}
