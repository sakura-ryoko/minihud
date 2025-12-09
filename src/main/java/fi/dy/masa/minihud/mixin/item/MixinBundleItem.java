package fi.dy.masa.minihud.mixin.item;

import java.util.Optional;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.minihud.config.Configs;

@Mixin(BundleItem.class)
public class MixinBundleItem
{
    @Inject(method = "getTooltipImage", at = @At("HEAD"), cancellable = true)
    private void minihud_getTooltipData(ItemStack stack, CallbackInfoReturnable<Optional<TooltipComponent>> cir)
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
}
