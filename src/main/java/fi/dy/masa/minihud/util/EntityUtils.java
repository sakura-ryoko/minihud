package fi.dy.masa.minihud.util;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import fi.dy.masa.malilib.util.WorldUtils;

public class EntityUtils
{
    @SuppressWarnings("unchecked")
    public static <T extends Entity> List<T> getEntitiesByClass(Minecraft mc, Class<T> entityClass, AABB box, Predicate<? super T> predicate)
    {
        if (mc.level == null)
        {
            return Collections.emptyList();
        }

        List<Integer> entityIds = mc.level.getEntitiesOfClass(entityClass, box, predicate).stream().map(Entity::getId).toList();
        Level world = WorldUtils.getBestWorld(mc);

        return entityIds.stream().map(it -> (T) world.getEntity(it))
                .filter(Objects::nonNull)
                .toList();
    }
}
