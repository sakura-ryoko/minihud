package fi.dy.masa.minihud.mixin.item;

// todo 1.21.5+
//@Mixin(ItemStack.class)
public abstract class MixinItemStack
{
//    @Shadow public abstract Item getItem();
//
//    @Inject(method = "appendTooltip",
//            at = @At(value = "HEAD"),
//            cancellable = true)
//    private <T> void minihud_disableVanillaBeeTooltips(ComponentType<T> componentType, Item.TooltipContext context, Consumer<Text> textConsumer, TooltipType type, CallbackInfo ci)
//    {
//        if (Configs.Generic.DISABLE_VANILLA_BEE_TOOLTIPS.getBooleanValue())
//        {
//            if (Configs.Generic.BEE_TOOLTIPS.getBooleanValue() &&
//                componentType == DataComponentTypes.BEES)
//            {
//                ci.cancel();
//            }
//            else if (Configs.Generic.HONEY_TOOLTIPS.getBooleanValue() &&
//                     componentType == DataComponentTypes.BLOCK_STATE &&
//                     this.getItem() instanceof BlockItem block &&
//                     block.getBlock() instanceof BeehiveBlock)
//            {
//                ci.cancel();
//            }
//        }
//    }
}
