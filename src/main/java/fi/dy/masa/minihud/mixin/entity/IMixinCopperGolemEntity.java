package fi.dy.masa.minihud.mixin.entity;

import net.minecraft.world.entity.animal.golem.CopperGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CopperGolem.class)
public interface IMixinCopperGolemEntity
{
	@Accessor("nextWeatheringTick")
	long minihud_getNextOxidationAge();
}
