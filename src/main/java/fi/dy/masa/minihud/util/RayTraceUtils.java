package fi.dy.masa.minihud.util;

import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.WorldUtils;
import fi.dy.masa.minihud.data.EntitiesDataStorage;
import fi.dy.masa.minihud.event.RenderHandler;
import fi.dy.masa.minihud.mixin.IMixinAbstractHorseEntity;
import fi.dy.masa.minihud.mixin.IMixinPiglinEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public class RayTraceUtils
{
    @Nonnull
    public static HitResult getRayTraceFromEntity(World worldIn, Entity entityIn, boolean useLiquids)
    {
        double reach = 5.0d;
        return getRayTraceFromEntity(worldIn, entityIn, useLiquids, reach);
    }

    @Nonnull
    public static HitResult getRayTraceFromEntity(World worldIn, Entity entityIn, boolean useLiquids, double range)
    {
        Vec3d eyesVec = new Vec3d(entityIn.getX(), entityIn.getY() + entityIn.getStandingEyeHeight(), entityIn.getZ());
        Vec3d rangedLookRot = entityIn.getRotationVec(1f).multiply(range);
        Vec3d lookVec = eyesVec.add(rangedLookRot);

        RaycastContext.FluidHandling fluidMode = useLiquids ? RaycastContext.FluidHandling.SOURCE_ONLY : RaycastContext.FluidHandling.NONE;
        RaycastContext context = new RaycastContext(eyesVec, lookVec, RaycastContext.ShapeType.COLLIDER, fluidMode, entityIn);
        HitResult result = worldIn.raycast(context);

        if (result == null)
        {
            result = BlockHitResult.createMissed(Vec3d.ZERO, Direction.UP, BlockPos.ORIGIN);
        }

        net.minecraft.util.math.Box bb = entityIn.getBoundingBox().expand(rangedLookRot.x, rangedLookRot.y, rangedLookRot.z).expand(1d, 1d, 1d);
        List<Entity> list = worldIn.getOtherEntities(entityIn, bb);

        double closest = result.getType() == HitResult.Type.BLOCK ? eyesVec.distanceTo(result.getPos()) : Double.MAX_VALUE;
        Optional<Vec3d> entityTrace = Optional.empty();
        Entity targetEntity = null;

        for (int i = 0; i < list.size(); i++)
        {
            Entity entity = list.get(i);
            bb = entity.getBoundingBox();
            Optional<Vec3d> traceTmp = bb.raycast(lookVec, eyesVec);

            if (traceTmp.isPresent())
            {
                double distance = eyesVec.distanceTo(traceTmp.get());

                if (distance <= closest)
                {
                    targetEntity = entity;
                    entityTrace = traceTmp;
                    closest = distance;
                }
            }
        }

        if (targetEntity != null)
        {
            result = new EntityHitResult(targetEntity, entityTrace.get());
        }

        return result;
    }

    public static @Nullable RayTraceUtils.InventoryPreviewData getTargetInventory(MinecraftClient mc)
    {
        World world = WorldUtils.getBestWorld(mc);
        Entity cameraEntity = EntityUtils.getCameraEntity();

        if (mc.player == null || world == null)
        {
            return null;
        }

        if (cameraEntity == mc.player && world instanceof ServerWorld)
        {
            // We need to get the player from the server world (if available, ie. in single player),
            // so that the player itself won't be included in the ray trace
            Entity serverPlayer = world.getPlayerByUuid(mc.player.getUuid());

            if (serverPlayer != null)
            {
                cameraEntity = serverPlayer;
            }
        }

        HitResult trace = getRayTraceFromEntity(world, cameraEntity, false);

        if (trace.getType() == HitResult.Type.BLOCK)
        {
            BlockPos pos = ((BlockHitResult) trace).getBlockPos();
            RenderHandler.getInstance().requestBlockEntityAt(mc.world, pos);

            Inventory inv = InventoryUtils.getInventory(world, pos);
            if (inv == null)
            {
                return null;
            }

            return new InventoryPreviewData(inv, world.getBlockEntity(pos), null);
        }
        else if (trace.getType() == HitResult.Type.ENTITY)
        {
            Entity entity = ((EntityHitResult) trace).getEntity();
            EntitiesDataStorage.getInstance().requestEntity(entity.getId());
            return getTargetInventoryFromEntity(entity);
        }
        return null;
    }

    public static InventoryPreviewData getTargetInventoryFromEntity(Entity entity)
    {
        Inventory inv = null;
        LivingEntity entityLivingBase = null;

        if (entity instanceof LivingEntity)
        {
            entityLivingBase = (LivingEntity) entity;
        }

        if (entity instanceof Inventory)
        {
            inv = (Inventory) entity;
        }
        else if (entity instanceof PlayerEntity player)
        {
            inv = new SimpleInventory(player.getInventory().main.toArray(new ItemStack[36]));
        }
        else if (entity instanceof VillagerEntity)
        {
            inv = ((VillagerEntity) entity).getInventory();
        }
        else if (entity instanceof AbstractHorseEntity)
        {
            inv = ((IMixinAbstractHorseEntity) entity).minihud_getHorseInventory();
        }
        else if (entity instanceof PiglinEntity)
        {
            inv = ((IMixinPiglinEntity) entity).inventory();
        }
        if (inv == null && entityLivingBase == null)
        {
            return null;
        }
        return new InventoryPreviewData(inv, null, entityLivingBase);
    }

    public record InventoryPreviewData(Inventory inv, @Nullable BlockEntity te, @Nullable LivingEntity entity)
    {
    }
}
