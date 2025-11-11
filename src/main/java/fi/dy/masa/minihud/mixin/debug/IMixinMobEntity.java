package fi.dy.masa.minihud.mixin.debug;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Mob.class)
public interface IMixinMobEntity
{
    @Accessor("goalSelector")
    GoalSelector minihud_getGoalSelector();
}
