package fi.dy.masa.minihud.mixin.world;

import net.minecraft.world.biome.source.BiomeAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BiomeAccess.class)
public interface IMixinBiomeAccess
{
    @Accessor("seed")
    long minihud_getSeed();
}
