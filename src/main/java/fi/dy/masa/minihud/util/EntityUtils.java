package fi.dy.masa.minihud.util;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import fi.dy.masa.malilib.util.WorldUtils;

public class EntityUtils
{
    // entity.readNbt(nbt);
//    public static void loadNbtIntoEntity(Entity entity, NbtCompound nbt)
//    {
//        entity.fallDistance = nbt.getFloat("FallDistance", 0f);
//        entity.setFireTicks(nbt.getShort("Fire", (short) 0));
//        if (nbt.contains("Air"))
//        {
//            entity.setAir(nbt.getShort("Air", (short) 0));
//        }
//
//        entity.setOnGround(nbt.getBoolean("OnGround", false));
//        entity.setInvulnerable(nbt.getBoolean("Invulnerable", false));
//        entity.setPortalCooldown(nbt.getInt("PortalCooldown", 0));
//        /*
//        if (nbt.containsUuid("UUID")) {
//            entity.setUuid(nbt.getUuid("UUID"));
//        }
//         */
//        if (nbt.contains("UUID"))
//        {
//            entity.setUuid(nbt.get("UUID", Uuids.CODEC).orElse(Util.NIL_UUID));
//        }
//
//        if (nbt.contains("CustomName")) {
//            String string = nbt.getString("CustomName", "?");
//            entity.setCustomName(Text.Serialization.fromJson(string, entity.getRegistryManager()));
//        }
//
//        entity.setCustomNameVisible(nbt.getBoolean("CustomNameVisible", false));
//        entity.setSilent(nbt.getBoolean("Silent", false));
//        entity.setNoGravity(nbt.getBoolean("NoGravity", false));
//        entity.setGlowing(nbt.getBoolean("Glowing", false));
//        entity.setFrozenTicks(nbt.getInt("TicksFrozen", 0));
//        if (nbt.contains("Tags"))
//        {
//            entity.getCommandTags().clear();
//            NbtList nbtList4 = nbt.getListOrEmpty("Tags");
//            int max = Math.min(nbtList4.size(), 1024);
//
//            for(int i = 0; i < max; ++i)
//            {
//                entity.getCommandTags().add(nbtList4.getString(i, "?"));
//            }
//        }
//
//        if (entity instanceof Leashable)
//        {
//            readLeashableEntityCustomData(entity, nbt);
//        }
//        else
//        {
//            ((IMixinEntity) entity).minihud_readCustomDataFromNbt(nbt);
//        }
//    }
//
//    private static void readLeashableEntityCustomData(Entity entity, NbtCompound nbt)
//    {
//        MinecraftClient mc = MinecraftClient.getInstance();
//        assert entity instanceof Leashable;
//        Leashable leashable = (Leashable) entity;
//        ((IMixinEntity) entity).minihud_readCustomDataFromNbt(nbt);
//        if (leashable.getLeashData() != null && leashable.getLeashData().unresolvedLeashData != null)
//        {
//            leashable.getLeashData().unresolvedLeashData
//                    .ifLeft(uuid ->
//                            // We MUST use client-side world here.
//                            leashable.attachLeash(((IMixinWorld) mc.world).minihud_getEntityLookup().get(uuid), false))
//                    .ifRight(pos ->
//                            leashable.attachLeash(LeashKnotEntity.getOrCreate(mc.world, pos), false));
//        }
//    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> List<T> getEntitiesByClass(MinecraftClient mc, Class<T> entityClass, Box box, Predicate<? super T> predicate)
    {
        if (mc.world == null)
        {
            return Collections.emptyList();
        }

        List<Integer> entityIds = mc.world.getEntitiesByClass(entityClass, box, predicate).stream().map(Entity::getId).toList();
        World world = WorldUtils.getBestWorld(mc);
        return entityIds.stream().map(it -> (T) world.getEntityById(it))
                .filter(Objects::nonNull)
                .toList();
    }
}
