package fi.dy.masa.minihud.mixin;

import net.minecraft.client.network.DataQueryHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DataQueryHandler.class)
public interface IMixinDataQueryHandler
{
    @Accessor("expectedTransactionId")
    int minihud_currentTransactionId();
}
