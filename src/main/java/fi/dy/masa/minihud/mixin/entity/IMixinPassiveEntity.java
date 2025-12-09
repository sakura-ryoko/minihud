package fi.dy.masa.minihud.mixin.entity;

import net.minecraft.world.entity.AgeableMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AgeableMob.class)
public interface IMixinPassiveEntity
{
    @Accessor("age")
    int minihud_getRealBreedingAge();
}
