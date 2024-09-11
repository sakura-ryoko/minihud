package fi.dy.masa.minihud.mixin;

import net.minecraft.entity.mob.ZombieVillagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ZombieVillagerEntity.class)
public interface IMixinZombieVillagerEntity
{
    @Accessor("conversionTimer")
    int minihud_conversionTimer();
}
