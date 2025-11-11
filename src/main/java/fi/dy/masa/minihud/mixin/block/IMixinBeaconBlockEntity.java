package fi.dy.masa.minihud.mixin.block;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.block.entity.BeamEmitter;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BeaconBlockEntity.class)
public interface IMixinBeaconBlockEntity
{
    @Accessor("level")
    int minihud_getLevel();

    @Accessor("beamSegments")
    List<BeamEmitter.BeamSegment> minihud_getBeamEmitter();

    @Accessor("primary")
    @Nullable RegistryEntry<StatusEffect> minihud_getPrimary();

    @Accessor("secondary")
    @Nullable RegistryEntry<StatusEffect> minihud_getSecondary();
}
