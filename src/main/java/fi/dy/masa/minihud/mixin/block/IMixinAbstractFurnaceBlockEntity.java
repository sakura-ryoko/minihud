package fi.dy.masa.minihud.mixin.block;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractFurnaceBlockEntity.class)
public interface IMixinAbstractFurnaceBlockEntity
{
    @Accessor("recipesUsed")
    Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> minihud_getUsedRecipes();
}
