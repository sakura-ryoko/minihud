package fi.dy.masa.minihud.mixin;

import java.util.List;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BeehiveBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.util.MiscUtils;

@Mixin(ItemStack.class)
public abstract class MixinItemStack
{
    @Shadow
    public abstract Item getItem();

    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void onGetTooltip(Item.TooltipContext context, @Nullable PlayerEntity player, TooltipType type, CallbackInfoReturnable<List<Text>> cir)
    {
        List<Text> list = cir.getReturnValue();

        if (Configs.Generic.AXOLOTL_TOOLTIPS.getBooleanValue() &&
            this.getItem() == Items.AXOLOTL_BUCKET)
        {
            MiscUtils.addAxolotlTooltip((ItemStack) (Object) this, list);
            return;
        }

        if (Configs.Generic.BEE_TOOLTIPS.getBooleanValue() &&
            this.getItem() instanceof BlockItem &&
            ((BlockItem) this.getItem()).getBlock() instanceof BeehiveBlock)
        {
            MiscUtils.addBeeTooltip((ItemStack) (Object) this, list);
        }

        if (Configs.Generic.HONEY_TOOLTIPS.getBooleanValue() &&
            this.getItem() instanceof BlockItem &&
            ((BlockItem) this.getItem()).getBlock() instanceof BeehiveBlock)
        {
            MiscUtils.addHoneyTooltip((ItemStack) (Object) this, list);
        }
    }
}
