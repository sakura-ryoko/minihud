package fi.dy.masa.minihud.mixin.render;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuQuery;
import com.mojang.blaze3d.systems.TimerQuery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TimerQuery.class)
public interface IMixinGlTimer
{
	@Accessor("activeGpuQuery")
	GpuQuery minihud_getQuery();

	@Accessor("activeEncoder")
	CommandEncoder minihud_getCommandEncoder();
}
