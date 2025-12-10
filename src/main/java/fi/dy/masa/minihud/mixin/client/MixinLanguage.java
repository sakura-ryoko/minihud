package fi.dy.masa.minihud.mixin.client;


import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Map;
import net.minecraft.locale.Language;

@Mixin(Language.class)
public class MixinLanguage
{
    @ModifyArgs(
            method = "loadFromJson(Ljava/io/InputStream;Ljava/util/function/BiConsumer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/function/BiConsumer;accept(Ljava/lang/Object;Ljava/lang/Object;)V"
            )
    )
    private static void loadCustomText(Args args, @Local Map.Entry<String, JsonElement> entry)
    {
        if (args.<String>get(0).startsWith("minihud.") &&
	        entry.getValue() instanceof JsonPrimitive primitive)
        {
            args.set(1, primitive.getAsString());
        }
    }
}
