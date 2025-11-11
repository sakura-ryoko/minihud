package fi.dy.masa.minihud.mixin.world;

import java.util.Set;
import java.util.UUID;
import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.world.entity.EntityIndex;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import fi.dy.masa.minihud.util.IServerEntityManager;

@Mixin(ServerEntityManager.class)
public abstract class MixinServerEntityManager implements IServerEntityManager
{
    @Shadow @Final Set<UUID> entityUuids;
    @Shadow @Final private EntityIndex<?> index;

    @Override
    public int minihud$getUuidSize()
    {
        return this.entityUuids.size();
    }

    @Override
    public int minihud$getIndexSize()
    {
        return this.index.size();
    }
}
