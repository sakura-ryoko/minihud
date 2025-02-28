package fi.dy.masa.minihud.mixin.item;

import java.util.List;
import java.util.Optional;

import net.minecraft.item.BundleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.minihud.config.Configs;

@Mixin(BundleItem.class)
public class MixinBundleItem
{
    @Inject(method = "getTooltipData", at = @At("HEAD"), cancellable = true)
    private void minihud_getTooltipData(ItemStack stack, CallbackInfoReturnable<Optional<TooltipData>> cir)
    {
        if (Configs.Generic.BUNDLE_PREVIEW.getBooleanValue() &&
            Configs.Generic.BUNDLE_DISPLAY_REQUIRE_SHIFT.getBooleanValue() &&
            GuiBase.isShiftDown())
        {
            cir.setReturnValue(Optional.empty());
        }
        else if (Configs.Generic.BUNDLE_PREVIEW.getBooleanValue() &&
                !Configs.Generic.BUNDLE_DISPLAY_REQUIRE_SHIFT.getBooleanValue())
        {
            cir.setReturnValue(Optional.empty());
        }
    }

    @Inject(method = "appendTooltip", at = @At("HEAD"), cancellable = true)
    private void minihud_appendTooltipFix(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type, CallbackInfo ci)
    {
        // for 1.21.1 and below; this makes it so that `bundleTooltips` replaces the Vanilla function.
        if (Configs.Generic.BUNDLE_TOOLTIPS.getBooleanValue())
        {
            ci.cancel();
        }
    }
}
