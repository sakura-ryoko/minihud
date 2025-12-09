package fi.dy.masa.minihud.mixin.entity;

import net.minecraft.world.entity.monster.skeleton.Skeleton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Skeleton.class)
public interface IMixinSkeletonEntity
{
    @Accessor("conversionTime")
    int minihud_conversionTime();
}
