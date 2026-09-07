package fi.dy.masa.minihud.mixin.entity;

import net.minecraft.world.entity.ConversionTracker;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Zombie.class)
public interface IMixinZombie
{
    @Accessor("drowningTracker")
    ConversionTracker<Zombie> minihud_drowningTracker();
}
