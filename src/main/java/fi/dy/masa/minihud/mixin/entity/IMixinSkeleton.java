package fi.dy.masa.minihud.mixin.entity;

import net.minecraft.world.entity.ConversionTracker;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Skeleton.class)
public interface IMixinSkeleton
{
    @Accessor("freezingTracker")
    ConversionTracker<AbstractSkeleton> minihud_freezingTracker();
}
