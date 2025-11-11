package fi.dy.masa.minihud.mixin.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerLevel.class)
public interface IMixinServerWorld
{
    @Accessor("entityManager")
    PersistentEntitySectionManager<Entity> minihud_getEntityManager();
}
