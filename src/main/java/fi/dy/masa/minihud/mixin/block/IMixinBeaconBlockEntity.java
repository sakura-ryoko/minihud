package fi.dy.masa.minihud.mixin.block;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;

@Mixin(BeaconBlockEntity.class)
public interface IMixinBeaconBlockEntity
{
    @Accessor("level")
    int minihud_getLevel();

    @Accessor("primary")
    @Nullable RegistryEntry<StatusEffect> minihud_getPrimary();

//    @Accessor("secondary")
//    @Nullable RegistryEntry<StatusEffect> minihud_getSecondary();
}
