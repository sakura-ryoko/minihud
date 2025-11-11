package fi.dy.masa.minihud.mixin.debug;

import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PathNavigation.class)
public interface IMixinEntityNavigation
{
    @Accessor("maxDistanceToWaypoint")
    float minihud_getMaxDistanceToWaypoint();
}
