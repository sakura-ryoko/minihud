package fi.dy.masa.minihud.mixin.client;


import net.minecraft.locale.Language;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Language.class)
public class MixinLanguage
{
    // TODO -- What is this even for ???
//    @ModifyArgs(
//            method = "loadFromJson(Ljava/io/InputStream;Ljava/util/function/BiConsumer;)V",
//            at = @At(
//                    value = "INVOKE",
//                    target = "Ljava/util/function/BiConsumer;accept(Ljava/lang/Object;Ljava/lang/Object;)V"
//            )
//    )
//    private static void loadCustomText(Args args, @Local(name = "entry") Map.Entry<String, JsonElement> entry)
//    {
//        if (args.<String>get(0).startsWith("minihud.") &&
//	        entry.getValue() instanceof JsonPrimitive primitive)
//        {
//            args.set(1, primitive.getAsString());
//        }
//    }
}
