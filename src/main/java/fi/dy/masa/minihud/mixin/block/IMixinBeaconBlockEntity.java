package fi.dy.masa.minihud.mixin.block;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.entity.BeaconBeamOwner;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BeaconBlockEntity.class)
public interface IMixinBeaconBlockEntity
{
    @Accessor("levels")
    int minihud_getLevel();

    @Accessor("beamSections")
    List<BeaconBeamOwner.Section> minihud_getBeamEmitter();

    @Accessor("primaryPower")
    @Nullable Holder<MobEffect> minihud_getPrimary();

    @Accessor("secondaryPower")
    @Nullable Holder<MobEffect> minihud_getSecondary();
}
