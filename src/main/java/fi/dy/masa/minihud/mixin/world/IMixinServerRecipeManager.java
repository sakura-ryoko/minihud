package fi.dy.masa.minihud.mixin.world;

import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RecipeManager.class)
public interface IMixinServerRecipeManager
{
    @Accessor("recipes")
    RecipeMap minihud_getPreparedRecipes();
}
