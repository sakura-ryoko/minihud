package fi.dy.masa.minihud.mixin.render;

import com.mojang.blaze3d.systems.TimerQuery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TimerQuery.class)
public interface IGlTimer
{
	@Accessor("nextQueryName")
	int minihud_getQueryId();
}
