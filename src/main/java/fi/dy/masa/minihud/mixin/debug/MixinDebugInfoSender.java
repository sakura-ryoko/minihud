package fi.dy.masa.minihud.mixin.debug;

//@Mixin(value = DebugInfoSender.class)
@Deprecated(forRemoval = true)
public abstract class MixinDebugInfoSender
{
//    @Inject(method = "sendChunkWatchingChange", at = @At("HEAD"))
//    private static void minihud_onChunkWatchingChange(ServerWorld world, ChunkPos pos, CallbackInfo ci)
//    {
//        DebugDataManager.getInstance().sendChunkWatchingChange(world, pos);
//    }
//
//    @Inject(method = "sendPoiAddition", at = @At("HEAD"))
//    private static void minihud_onSendPoiAddition(ServerWorld world, BlockPos pos, CallbackInfo ci)
//    {
//        DebugDataManager.getInstance().sendPoiAdditions(world, pos);
//    }
//
//    @Inject(method = "sendPoiRemoval", at = @At("HEAD"))
//    private static void minihud_onSendPoiRemoval(ServerWorld world, BlockPos pos, CallbackInfo ci)
//    {
//        DebugDataManager.getInstance().sendPoiRemoval(world, pos);
//    }
//
//    @Inject(method = "sendPointOfInterest", at = @At("HEAD"))
//    private static void minihud_onSendPointOfInterest(ServerWorld world, BlockPos pos, CallbackInfo ci)
//    {
//        DebugDataManager.getInstance().sendPointOfInterest(world, pos);
//    }
//
//    @Inject(method = "sendPoi", at = @At("HEAD"))
//    private static void minihud_onSendPoi(ServerWorld world, BlockPos pos, CallbackInfo ci)
//    {
//        DebugDataManager.getInstance().sendPoi(world, pos);
//    }
//
//    //FIXME (CustomPayload Error)
//    @Inject(method = "sendPathfindingData", at = @At("HEAD"))
//    private static void minihud_onSendPathfindingData(World world, MobEntity mob, @Nullable Path path, float nodeReachProximity, CallbackInfo ci)
//    {
//        if (world instanceof ServerWorld serverWorld)
//        {
//            DebugDataManager.getInstance().sendPathfindingData(serverWorld, mob, path, nodeReachProximity);
//        }
//    }
//
//    @Inject(method = "sendRedstoneUpdateOrder", at = @At("HEAD"))
//    private static void minihud_onSendRedstoneUpdateOrder(World world, DebugRedstoneUpdateOrderCustomPayload payload, CallbackInfo ci)
//    {
//        // NO-OP
//    }
//
//    @Inject(method = "sendNeighborUpdate", at = @At("HEAD"))
//    private static void onSendNeighborUpdate(World world, BlockPos pos, CallbackInfo ci)
//    {
//        DebugInfoUtils.onNeighborUpdate(world, pos);
//    }
//
//    @Inject(method = "sendStructureStart", at = @At("HEAD"))
//    private static void minihud_onSendStructureStart(StructureWorldAccess world, StructureStart structureStart, CallbackInfo ci)
//    {
//        DebugDataManager.getInstance().sendStructureStart(world, structureStart);
//    }
//
//    @Inject(method = "sendGoalSelector", at = @At("HEAD"))
//    private static void minihud_onSendGoalSelector(World world, MobEntity mob, GoalSelector goalSelector, CallbackInfo ci)
//    {
//        if (world instanceof ServerWorld serverWorld)
//        {
//            DebugDataManager.getInstance().sendGoalSelector(serverWorld, mob, goalSelector);
//        }
//    }
//
//    @Inject(method = "sendRaids", at = @At("HEAD"))
//    private static void minihud_onSendRaids(ServerWorld server, Collection<Raid> raids, CallbackInfo ci)
//    {
//        DebugDataManager.getInstance().sendRaids(server, raids);
//    }
//
//    //FIXME (CustomPayload Error)
//    @Inject(method = "sendBrainDebugData", at = @At("HEAD"))
//    private static void minihud_onSendBrainDebugData(LivingEntity living, CallbackInfo ci)
//    {
//        if (living.getEntityWorld() instanceof ServerWorld world)
//        {
//            DebugDataManager.getInstance().sendBrainDebugData(world, living);
//        }
//    }
//
//    //FIXME (CustomPayload Error)
//    @Inject(method = "sendBeeDebugData", at = @At("HEAD"))
//    private static void minihud_onSendBeeDebugData(BeeEntity bee, CallbackInfo ci)
//    {
//        if (bee.getEntityWorld() instanceof ServerWorld world)
//        {
//            DebugDataManager.getInstance().sendBeeDebugData(world, bee);
//        }
//    }
//
//    @Inject(method = "sendBreezeDebugData", at = @At("HEAD"))
//    private static void minihud_onSendBreezeDebugData(BreezeEntity breeze, CallbackInfo ci)
//    {
//        if (breeze.getEntityWorld() instanceof ServerWorld world)
//        {
//            DebugDataManager.getInstance().sendBreezeDebugData(world, breeze);
//        }
//    }
//
//    @Inject(method = "sendGameEvent", at = @At("HEAD"))
//    private static void minihud_onSendGameEvent(World world, RegistryEntry<GameEvent> event, Vec3d pos, CallbackInfo ci)
//    {
//        if (world instanceof ServerWorld serverWorld)
//        {
//            DebugDataManager.getInstance().sendGameEvent(serverWorld, event, pos);
//        }
//    }
//
//    @Inject(method = "sendGameEventListener", at = @At("HEAD"))
//    private static void minihud_onSendGameEventListener(World world, GameEventListener eventListener, CallbackInfo ci)
//    {
//        if (world instanceof ServerWorld serverWorld)
//        {
//            DebugDataManager.getInstance().sendGameEventListener(serverWorld, eventListener);
//        }
//    }
}
