package fi.dy.masa.minihud.mixin.entity;

import net.minecraft.world.entity.ConversionTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ConversionTracker.class)
public interface IMixinConversionTracker
{
	@Accessor("conversionTime")
	int minihud_getConversionTime();

	@Accessor("afflictionTime")
	int minihud_getAfflictionTime();
}
