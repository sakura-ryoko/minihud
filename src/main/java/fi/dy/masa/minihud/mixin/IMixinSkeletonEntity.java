package fi.dy.masa.minihud.mixin;

import net.minecraft.entity.mob.SkeletonEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SkeletonEntity.class)
public interface IMixinSkeletonEntity
{
    @Accessor("conversionTime")
    int minihud_conversionTime();
}
