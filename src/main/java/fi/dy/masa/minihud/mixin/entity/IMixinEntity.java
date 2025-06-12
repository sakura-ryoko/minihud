package fi.dy.masa.minihud.mixin.entity;

import net.minecraft.entity.Entity;
import net.minecraft.storage.ReadView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface IMixinEntity
{
    @Invoker("readCustomData")
    void minihud_readCustomData(ReadView view);
}
