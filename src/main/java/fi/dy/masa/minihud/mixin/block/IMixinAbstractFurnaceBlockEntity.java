package fi.dy.masa.minihud.mixin.block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;

@Mixin(AbstractFurnaceBlockEntity.class)
public interface IMixinAbstractFurnaceBlockEntity
{
    @Accessor("recipesUsed")
    Object2IntOpenHashMap<Identifier> minihud_getUsedRecipes();
}
