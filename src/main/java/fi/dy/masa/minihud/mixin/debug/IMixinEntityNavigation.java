package fi.dy.masa.minihud.mixin.debug;

import net.minecraft.entity.ai.pathing.EntityNavigation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityNavigation.class)
public interface IMixinEntityNavigation
{
    @Accessor("nodeReachProximity")
    float minihud_getMaxDistanceToWaypoint();
}
