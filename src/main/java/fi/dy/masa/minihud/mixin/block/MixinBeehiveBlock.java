package fi.dy.masa.minihud.mixin.block;

import java.util.List;

import net.minecraft.block.BeehiveBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.minihud.config.Configs;

@Mixin(BeehiveBlock.class)
public class MixinBeehiveBlock
{
    @Inject(method = "appendTooltip",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/block/BlockWithEntity;appendTooltip(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/Item$TooltipContext;Ljava/util/List;Lnet/minecraft/item/tooltip/TooltipType;)V",
                     shift = At.Shift.AFTER),
            cancellable = true)
    private void minihud_disableVanillaBeeTooltips(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options, CallbackInfo ci)
    {
        if (Configs.Generic.DISABLE_VANILLA_BEE_TOOLTIPS.getBooleanValue() &&
            (Configs.Generic.BEE_TOOLTIPS.getBooleanValue() ||
             Configs.Generic.HONEY_TOOLTIPS.getBooleanValue()))
        {
            ci.cancel();
        }
    }
}
