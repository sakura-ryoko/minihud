package fi.dy.masa.minihud.mixin.world;

import net.minecraft.world.level.biome.BiomeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BiomeManager.class)
public interface IMixinBiomeAccess
{
    @Accessor("biomeZoomSeed")
    long minihud_getSeed();
}
