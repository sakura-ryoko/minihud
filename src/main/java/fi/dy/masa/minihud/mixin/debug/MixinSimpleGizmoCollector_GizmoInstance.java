package fi.dy.masa.minihud.mixin.debug;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.gizmos.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import fi.dy.masa.minihud.config.Configs;

@Mixin(SimpleGizmoCollector.GizmoInstance.class)
public abstract class MixinSimpleGizmoCollector_GizmoInstance
{
	@Shadow private boolean isAlwaysOnTop;
	@Shadow @Final private Gizmo gizmo;

	@WrapMethod(method = "setAlwaysOnTop")
	private GizmoProperties minihud_onFixDebugAlwaysOnTop(Operation<GizmoProperties> original)
	{
		GizmoProperties orig = original.call();

		if (Configs.Generic.FIX_VANILLA_DEBUG_RENDERER_THROUGH.getBooleanValue())
		{
			if ((this.gizmo instanceof CuboidGizmo || this.gizmo instanceof TextGizmo) &&
				this.isAlwaysOnTop)
			{
				this.isAlwaysOnTop = false;
				return (GizmoProperties) this;
			}
		}

		return orig;
	}
}
