package fi.dy.masa.minihud.mixin.item;

import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.BeehiveBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.minihud.config.Configs;

@Mixin(ItemStack.class)
public abstract class MixinItemStack
{
    @Shadow public abstract Item getItem();

    @Inject(method = "addToTooltip",
            at = @At(value = "HEAD"),
            cancellable = true)
    private <T> void minihud_disableVanillaBeeTooltips(DataComponentType<T> componentType, Item.TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type, CallbackInfo ci)
    {
        if (Configs.Generic.DISABLE_VANILLA_BEE_TOOLTIPS.getBooleanValue())
        {
            if (Configs.Generic.BEE_TOOLTIPS.getBooleanValue() &&
                componentType == DataComponents.BEES)
            {
                ci.cancel();
            }
            else if (Configs.Generic.HONEY_TOOLTIPS.getBooleanValue() &&
                     componentType == DataComponents.BLOCK_STATE &&
                     this.getItem() instanceof BlockItem block &&
                     block.getBlock() instanceof BeehiveBlock)
            {
                ci.cancel();
            }
        }
    }
}
