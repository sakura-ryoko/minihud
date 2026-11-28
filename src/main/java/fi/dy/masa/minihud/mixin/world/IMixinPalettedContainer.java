package fi.dy.masa.minihud.mixin.world;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PalettedContainer.class)
public interface IMixinPalettedContainer
{
    @Accessor("data")
    PalettedContainer.Data<BlockState> minihud_getData();
}
