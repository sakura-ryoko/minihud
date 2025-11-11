package fi.dy.masa.minihud.mixin.render;

import net.minecraft.client.gl.GlTimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlTimer.class)
public interface IGlTimer
{
	@Accessor("queryId")
	int minihud_getQueryId();
}
