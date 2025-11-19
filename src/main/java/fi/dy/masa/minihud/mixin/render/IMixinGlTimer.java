package fi.dy.masa.minihud.mixin.render;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuQuery;
import net.minecraft.client.gl.GlTimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlTimer.class)
public interface IMixinGlTimer
{
	@Accessor("query")
	GpuQuery minihud_getQuery();

	@Accessor("encoder")
	CommandEncoder minihud_getCommandEncoder();
}
