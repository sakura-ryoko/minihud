package fi.dy.masa.minihud.mixin.debug;

import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MobEntity.class)
public interface IMixinMobEntity
{
    @Accessor("goalSelector")
    GoalSelector minihud_getGoalSelector();
}
