package fi.dy.masa.minihud.mixin.entity;

import net.minecraft.entity.mob.ZombieEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ZombieEntity.class)
public interface IMixinZombieEntity
{
    @Accessor("ticksUntilWaterConversion")
    int minihud_ticksUntilWaterConversion();
}
