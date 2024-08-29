package fi.dy.masa.minihud.mixin;

import net.minecraft.client.render.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = DebugRenderer.class)
public interface IMixinDebugRenderer
{
    @Accessor("showChunkBorder")
    boolean minihud_getShowChunkBorder();

    @Accessor("showOctree")
    boolean minihud_getShowOctree();
}
