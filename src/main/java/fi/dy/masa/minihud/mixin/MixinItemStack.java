package fi.dy.masa.minihud.mixin;

import java.util.function.Consumer;

import net.minecraft.block.BeehiveBlock;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
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

    @Inject(method = "appendComponentTooltip",
            at = @At(value = "HEAD"),
            cancellable = true)
    private <T> void minihud_disableVanillaBeeTooltips(ComponentType<T> componentType, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type, CallbackInfo ci)
    {
        if (Configs.Generic.DISABLE_VANILLA_BEE_TOOLTIPS.getBooleanValue())
        {
            if (Configs.Generic.BEE_TOOLTIPS.getBooleanValue() &&
                componentType == DataComponentTypes.BEES)
            {
                ci.cancel();
            }
            else if (Configs.Generic.HONEY_TOOLTIPS.getBooleanValue() &&
                     componentType == DataComponentTypes.BLOCK_STATE &&
                     this.getItem() instanceof BlockItem block &&
                     block.getBlock() instanceof BeehiveBlock)
            {
                ci.cancel();
            }
        }
    }
}
