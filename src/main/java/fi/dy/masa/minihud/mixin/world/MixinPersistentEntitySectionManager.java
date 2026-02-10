package fi.dy.masa.minihud.mixin.world;

import java.util.Set;
import java.util.UUID;
import net.minecraft.world.level.entity.EntityLookup;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import fi.dy.masa.minihud.util.IServerEntityManager;

@Mixin(PersistentEntitySectionManager.class)
public abstract class MixinPersistentEntitySectionManager implements IServerEntityManager
{
    @Shadow @Final Set<UUID> knownUuids;
    @Shadow @Final private EntityLookup<?> visibleEntityStorage;

    @Override
    public int minihud$getUuidSize()
    {
        return this.knownUuids.size();
    }

    @Override
    public int minihud$getIndexSize()
    {
        return this.visibleEntityStorage.count();
    }
}
