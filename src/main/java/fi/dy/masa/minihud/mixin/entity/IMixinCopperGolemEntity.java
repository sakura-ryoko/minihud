package fi.dy.masa.minihud.mixin.entity;

import net.minecraft.entity.passive.CopperGolemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CopperGolemEntity.class)
public interface IMixinCopperGolemEntity
{
	@Accessor("nextOxidationAge")
	long minihud_getNextOxidationAge();
}
