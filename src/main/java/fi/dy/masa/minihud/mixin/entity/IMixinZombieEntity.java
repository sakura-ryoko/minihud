package fi.dy.masa.minihud.mixin.entity;

import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Zombie.class)
public interface IMixinZombieEntity
{
    @Accessor("conversionTime")
    int minihud_ticksUntilWaterConversion();
}
