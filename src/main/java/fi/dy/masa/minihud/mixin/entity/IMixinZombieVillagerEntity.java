package fi.dy.masa.minihud.mixin.entity;

import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ZombieVillager.class)
public interface IMixinZombieVillagerEntity
{
    @Accessor("villagerConversionTime")
    int minihud_conversionTimer();
}
