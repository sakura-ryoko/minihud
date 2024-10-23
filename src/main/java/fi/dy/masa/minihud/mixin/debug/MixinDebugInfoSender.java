package fi.dy.masa.minihud.mixin.debug;

import java.util.Collection;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.network.packet.s2c.custom.DebugRedstoneUpdateOrderCustomPayload;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.raid.Raid;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.listener.GameEventListener;

import fi.dy.masa.minihud.data.DebugDataManager;
import fi.dy.masa.minihud.util.DebugInfoUtils;

@Mixin(value = DebugInfoSender.class)
public abstract class MixinDebugInfoSender
{
    @Inject(method = "sendChunkWatchingChange", at = @At("HEAD"))
    private static void servux_onChunkWatchingChange(ServerWorld world, ChunkPos pos, CallbackInfo ci)
    {
        DebugDataManager.getInstance().sendChunkWatchingChange(world, pos);
    }

    @Inject(method = "sendPoiAddition", at = @At("HEAD"))
    private static void servux_onSendPoiAddition(ServerWorld world, BlockPos pos, CallbackInfo ci)
    {
        DebugDataManager.getInstance().sendPoiAdditions(world, pos);
    }

    @Inject(method = "sendPoiRemoval", at = @At("HEAD"))
    private static void servux_onSendPoiRemoval(ServerWorld world, BlockPos pos, CallbackInfo ci)
    {
        DebugDataManager.getInstance().sendPoiRemoval(world, pos);
    }

    @Inject(method = "sendPointOfInterest", at = @At("HEAD"))
    private static void servux_onSendPointOfInterest(ServerWorld world, BlockPos pos, CallbackInfo ci)
    {
        DebugDataManager.getInstance().sendPointOfInterest(world, pos);
    }

    @Inject(method = "sendPoi", at = @At("HEAD"))
    private static void servux_onSendPoi(ServerWorld world, BlockPos pos, CallbackInfo ci)
    {
        DebugDataManager.getInstance().sendPoi(world, pos);
    }

    //FIXME (CustomPayload Error)
    @Inject(method = "sendPathfindingData", at = @At("HEAD"))
    private static void servux_onSendPathfindingData(World world, MobEntity mob, @Nullable Path path, float nodeReachProximity, CallbackInfo ci)
    {
        if (world instanceof ServerWorld serverWorld)
        {
            DebugDataManager.getInstance().sendPathfindingData(serverWorld, mob, path, nodeReachProximity);
        }
    }

    @Inject(method = "sendRedstoneUpdateOrder", at = @At("HEAD"))
    private static void servux_onSendRedstoneUpdateOrder(World world, DebugRedstoneUpdateOrderCustomPayload payload, CallbackInfo ci)
    {
        // NO-OP
    }
    
    @Inject(method = "sendNeighborUpdate", at = @At("HEAD"))
    private static void onSendNeighborUpdate(World world, BlockPos pos, CallbackInfo ci)
    {
        DebugInfoUtils.onNeighborUpdate(world, pos);
    }
    
    @Inject(method = "sendStructureStart", at = @At("HEAD"))
    private static void servux_onSendStructureStart(StructureWorldAccess world, StructureStart structureStart, CallbackInfo ci)
    {
        DebugDataManager.getInstance().sendStructureStart(world, structureStart);
    }

    @Inject(method = "sendGoalSelector", at = @At("HEAD"))
    private static void servux_onSendGoalSelector(World world, MobEntity mob, GoalSelector goalSelector, CallbackInfo ci)
    {
        if (world instanceof ServerWorld serverWorld)
        {
            DebugDataManager.getInstance().sendGoalSelector(serverWorld, mob, goalSelector);
        }
    }

    @Inject(method = "sendRaids", at = @At("HEAD"))
    private static void servux_onSendRaids(ServerWorld server, Collection<Raid> raids, CallbackInfo ci)
    {
        DebugDataManager.getInstance().sendRaids(server, raids);
    }

    //FIXME (CustomPayload Error)
    @Inject(method = "sendBrainDebugData", at = @At("HEAD"))
    private static void servux_onSendBrainDebugData(LivingEntity living, CallbackInfo ci)
    {
        if (living.getWorld() instanceof ServerWorld world)
        {
            DebugDataManager.getInstance().sendBrainDebugData(world, living);
        }
    }

    //FIXME (CustomPayload Error)
    @Inject(method = "sendBeeDebugData", at = @At("HEAD"))
    private static void servux_onSendBeeDebugData(BeeEntity bee, CallbackInfo ci)
    {
        if (bee.getWorld() instanceof ServerWorld world)
        {
            DebugDataManager.getInstance().sendBeeDebugData(world, bee);
        }
    }

    @Inject(method = "sendBreezeDebugData", at = @At("HEAD"))
    private static void servux_onSendBreezeDebugData(BreezeEntity breeze, CallbackInfo ci)
    {
        if (breeze.getWorld() instanceof ServerWorld world)
        {
            DebugDataManager.getInstance().sendBreezeDebugData(world, breeze);
        }
    }

    @Inject(method = "sendGameEvent", at = @At("HEAD"))
    private static void servux_onSendGameEvent(World world, RegistryEntry<GameEvent> event, Vec3d pos, CallbackInfo ci)
    {
        if (world instanceof ServerWorld serverWorld)
        {
            DebugDataManager.getInstance().sendGameEvent(serverWorld, event, pos);
        }
    }

    @Inject(method = "sendGameEventListener", at = @At("HEAD"))
    private static void servux_onSendGameEventListener(World world, GameEventListener eventListener, CallbackInfo ci)
    {
        if (world instanceof ServerWorld serverWorld)
        {
            DebugDataManager.getInstance().sendGameEventListener(serverWorld, eventListener);
        }
    }
}
