package fi.dy.masa.minihud.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface IMixinEntity
{
    @Invoker("readCustomDataFromNbt")
    void minihud_readCustomDataFromNbt(NbtCompound nbt);
}
