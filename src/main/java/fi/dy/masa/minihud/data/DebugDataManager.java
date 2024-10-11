package fi.dy.masa.minihud.data;

import java.util.*;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.*;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.custom.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.NameGenerator;
import net.minecraft.util.Nameable;
import net.minecraft.util.math.*;
import net.minecraft.village.raid.Raid;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.listener.GameEventListener;
import net.minecraft.world.gen.structure.Structure;

import fi.dy.masa.malilib.network.ClientPlayHandler;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.mixin.debug.IMixinMobEntity;
import fi.dy.masa.minihud.network.ServuxDebugHandler;
import fi.dy.masa.minihud.network.ServuxDebugPacket;
import fi.dy.masa.minihud.network.ServuxHudHandler;
import fi.dy.masa.minihud.util.DataStorage;

public class DebugDataManager
{
    private static final DebugDataManager INSTANCE = new DebugDataManager();

    private final static ServuxDebugHandler<ServuxDebugPacket.Payload> HANDLER = ServuxDebugHandler.getInstance();
    private final MinecraftClient mc = MinecraftClient.getInstance();

    private boolean servuxServer;
    private boolean hasInValidServux;
    private String servuxVersion;
    private boolean shouldRegisterDebugService;

    public DebugDataManager()
    {
        this.servuxServer = false;
        this.hasInValidServux = false;
        this.servuxVersion = "";
    }

    public static DebugDataManager getInstance() {return INSTANCE;}

    public void onGameInit()
    {
        ClientPlayHandler.getInstance().registerClientPlayHandler(HANDLER);
        HANDLER.registerPlayPayload(ServuxDebugPacket.Payload.ID, ServuxDebugPacket.Payload.CODEC, IPluginClientPlayHandler.BOTH_CLIENT);
    }

    public Identifier getNetworkChannel() {return ServuxHudHandler.CHANNEL_ID;}

    public IPluginClientPlayHandler<ServuxDebugPacket.Payload> getNetworkHandler() {return HANDLER;}

    public void reset(boolean isLogout)
    {
        if (isLogout)
        {
            MiniHUD.printDebug("DebugDataManager#reset() - log-out");
            HANDLER.reset(this.getNetworkChannel());
            HANDLER.resetFailures(this.getNetworkChannel());

            this.servuxServer = false;
            this.hasInValidServux = false;
            this.servuxVersion = "";
        }
    }

    public void onWorldPre()
    {
        if (!DataStorage.getInstance().hasIntegratedServer())
        {
            HANDLER.registerPlayReceiver(ServuxDebugPacket.Payload.ID, HANDLER::receivePlayPayload);
        }
    }

    public void onWorldJoin()
    {
        MiniHUD.printDebug("DebugDataManager#onWorldJoin()");

        if (!DataStorage.getInstance().hasIntegratedServer())
        {
            if (RendererToggle.DEBUG_DATA_MAIN_TOGGLE.getBooleanValue())
            {
                this.registerDebugService();
            }
            else
            {
                this.unregisterDebugService();
            }
        }
    }

    public void setIsServuxServer()
    {
        this.servuxServer = true;
        if (this.hasInValidServux)
        {
            this.hasInValidServux = false;
        }
    }

    public void setServuxVersion(String ver)
    {
        if (ver != null && !ver.isEmpty())
        {
            this.servuxVersion = ver;
        }
        else
        {
            this.servuxVersion = "unknown";
        }
    }

    public String getServuxVersion()
    {
        if (this.hasServuxServer())
        {
            return this.servuxVersion;
        }

        return "not_connected";
    }

    public boolean hasServuxServer() { return this.servuxServer; }

    public void registerDebugService()
    {
        this.shouldRegisterDebugService = true;

        if (!this.hasServuxServer() && !DataStorage.getInstance().hasIntegratedServer() && !this.hasInValidServux)
        {
            if (HANDLER.isPlayRegistered(this.getNetworkChannel()))
            {
                MiniHUD.printDebug("DebugDataManager#registerDebugService(): sending DEBUG_SERVICE_REGISTER to Servux");

                NbtCompound nbt = new NbtCompound();
                nbt.putString("version", Reference.MOD_STRING);

                HANDLER.encodeClientData(ServuxDebugPacket.DebugServiceRegister(nbt));
            }
        }
        else
        {
            this.shouldRegisterDebugService = false;
        }
    }

    public boolean receiveMetadata(NbtCompound data)
    {
        if (!this.hasServuxServer() && !DataStorage.getInstance().hasIntegratedServer() &&
            this.shouldRegisterDebugService)
        {
            MiniHUD.printDebug("DebugDataManager#receiveMetadata(): received METADATA from Servux");

            if (data.getInt("version") != ServuxDebugPacket.PROTOCOL_VERSION)
            {
                MiniHUD.logger.warn("debugDataChannel: Mis-matched protocol version!");
            }

            this.setServuxVersion(data.getString("servux"));
            this.setIsServuxServer();

            if (RendererToggle.DEBUG_DATA_MAIN_TOGGLE.getBooleanValue())
            {
                this.shouldRegisterDebugService = true;

                NbtCompound nbt = new NbtCompound();
                nbt.putString("version", Reference.MOD_STRING);

                HANDLER.encodeClientData(ServuxDebugPacket.MetadataConfirm(nbt));
                return true;
            }
            else
            {
                this.unregisterDebugService();
            }
        }

        return false;
    }

    public void unregisterDebugService()
    {
        if (this.hasServuxServer() || !RendererToggle.DEBUG_DATA_MAIN_TOGGLE.getBooleanValue())
        {
            this.servuxServer = false;
            if (!this.hasInValidServux)
            {
                MiniHUD.printDebug("DebugDataManager#unregisterDebugService(): for {}", this.servuxVersion != null ? this.servuxVersion : "<unknown>");

                HANDLER.encodeClientData(ServuxDebugPacket.DebugServiceUnregister(new NbtCompound()));
                HANDLER.reset(HANDLER.getPayloadChannel());
            }
        }
        this.shouldRegisterDebugService = false;
    }

    public void onPacketFailure()
    {
        // Define how to handle multiple sendPayload failures
        this.shouldRegisterDebugService = false;
        this.servuxServer = false;
        this.hasInValidServux = true;
    }

    public boolean isEnabled()
    {
        return RendererToggle.DEBUG_DATA_MAIN_TOGGLE.getBooleanValue() &&
                DataStorage.getInstance().hasIntegratedServer();
    }

    // Integrated Server Methods
    private void sendDebugData(ServerWorld world, CustomPayload payload)
    {
        if (this.isEnabled())
        {
            Packet<?> packet = new CustomPayloadS2CPacket(payload);

            for (ServerPlayerEntity player : world.getPlayers())
            {
                if (player.networkHandler.accepts(packet))
                {
                    player.networkHandler.sendPacket(packet);
                }
            }
        }
    }

    public void sendChunkWatchingChange(ServerWorld world, ChunkPos pos)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        if (RendererToggle.DEBUG_WORLDGEN.getBooleanValue())
        {
            this.sendDebugData(world, new DebugWorldgenAttemptCustomPayload(pos.getStartPos().up(100), 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));
        }
    }

    public void sendPoiAdditions(ServerWorld world, BlockPos pos)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        // DEBUG_VILLAGE_POI
        if (RendererToggle.DEBUG_VILLAGE_SECTIONS.getBooleanValue())
        {
            world.getPointOfInterestStorage().getType(pos).ifPresent((registryEntry) ->
             {
                 int tickets = world.getPointOfInterestStorage().getFreeTickets(pos);
                 String name = registryEntry.getIdAsString();
                 this.sendDebugData(world, new DebugPoiAddedCustomPayload(pos, name, tickets));
             });
        }
    }

    public void sendPoiRemoval(ServerWorld world, BlockPos pos)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        if (RendererToggle.DEBUG_VILLAGE_SECTIONS.getBooleanValue())
        {
            this.sendDebugData(world, new DebugPoiRemovedCustomPayload(pos));
        }
    }

    public void sendPointOfInterest(ServerWorld world, BlockPos pos)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        if (RendererToggle.DEBUG_VILLAGE_SECTIONS.getBooleanValue())
        {
            int tickets = world.getPointOfInterestStorage().getFreeTickets(pos);
            this.sendDebugData(world, new DebugPoiTicketCountCustomPayload(pos, tickets));
        }

    }

    public void sendPoi(ServerWorld world, BlockPos pos)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        if (RendererToggle.DEBUG_VILLAGE_SECTIONS.getBooleanValue())
        {
            Registry<Structure> registry = world.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);
            ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(pos);
            Iterator<RegistryEntry<Structure>> iterator = registry.iterateEntries(StructureTags.VILLAGE).iterator();

            RegistryEntry<Structure> entry;
            do
            {
                if (!iterator.hasNext())
                {
                    this.sendDebugData(world, new DebugVillageSectionsCustomPayload(Set.of(), Set.of(chunkSectionPos)));
                    return;
                }

                entry = iterator.next();
            }
            while (world.getStructureAccessor().getStructureStarts(chunkSectionPos, entry.value()).isEmpty());

            this.sendDebugData(world, new DebugVillageSectionsCustomPayload(Set.of(chunkSectionPos), Set.of()));
        }
    }

    // FIXME -- WARNING!  Causes CustomPayload Crash
    /*
    public void sendPathfindingData(ServerWorld world, MobEntity mob, @Nullable Path path, float nodeReachProximity)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        if (RendererToggle.DEBUG_PATH_FINDING.getBooleanValue())
        {
            if (path != null)
            {
                this.sendDebugData(world, new DebugPathCustomPayload(mob.getId(), path, nodeReachProximity));
            }
        }
    }
     */

    public void sendNeighborUpdate(ServerWorld world, BlockPos pos)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        if (RendererToggle.DEBUG_NEIGHBOR_UPDATES.getBooleanValue())
        {
            this.sendDebugData(world, new DebugNeighborsUpdateCustomPayload(world.getTime(), pos));
        }
    }

    public void sendStructureStart(StructureWorldAccess world, StructureStart structureStart)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        if (RendererToggle.DEBUG_STRUCTURES.getBooleanValue())
        {
            List<DebugStructuresCustomPayload.Piece> pieces = new ArrayList<>();

            for (int i = 0; i < structureStart.getChildren().size(); ++i)
            {
                pieces.add(new DebugStructuresCustomPayload.Piece(structureStart.getChildren().get(i).getBoundingBox(), i == 0));
            }

            ServerWorld serverWorld = world.toServerWorld();
            this.sendDebugData(serverWorld, new DebugStructuresCustomPayload(serverWorld.getRegistryKey(), structureStart.getBoundingBox(), pieces));
        }
    }

    public void sendGoalSelector(ServerWorld world, MobEntity mob, GoalSelector goalSelector)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        if (RendererToggle.DEBUG_GOAL_SELECTOR.getBooleanValue())
        {
            List<DebugGoalSelectorCustomPayload.Goal> goals = ((IMixinMobEntity) mob).minihud_getGoalSelector().getGoals().stream().map((goal) ->
                                      new DebugGoalSelectorCustomPayload.Goal(goal.getPriority(), goal.isRunning(), goal.getGoal().toString())).toList();

            this.sendDebugData(world, new DebugGoalSelectorCustomPayload(mob.getId(), mob.getBlockPos(), goals));
        }
    }

    public void sendRaids(ServerWorld world, Collection<Raid> raids)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        if (RendererToggle.DEBUG_RAID_CENTER.getBooleanValue())
        {
            this.sendDebugData(world, new DebugRaidsCustomPayload(raids.stream().map(Raid::getCenter).toList()));
        }
    }

    // FIXME -- WARNING!  Causes CustomPayload Crash
    /*
    public void sendBrainDebugData(ServerWorld serverWorld, LivingEntity livingEntity, List<String> memories)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        if (RendererToggle.DEBUG_BRAIN.getBooleanValue())
        {
            MobEntity entity = (MobEntity) livingEntity;
            int angerLevel;

            if (entity instanceof WardenEntity wardenEntity)
            {
                angerLevel = wardenEntity.getAnger();
            }
            else
            {
                angerLevel = -1;
            }

            List<String> gossips = new ArrayList<>();
            Set<BlockPos> pois = new HashSet<>();
            Set<BlockPos> potentialPois = new HashSet<>();
            String profession;
            int xp;
            String inventory;
            boolean wantsGolem;

            if (entity instanceof VillagerEntity villager)
            {
                profession = villager.getVillagerData().getProfession().toString();
                xp = villager.getExperience();
                inventory = villager.getInventory().toString();
                wantsGolem = villager.canSummonGolem(serverWorld.getTime());
                villager.getGossip().getEntityReputationAssociatedGossips().forEach((uuid, associatedGossip) ->
                    {
                        Entity gossipEntity = serverWorld.getEntity(uuid);

                        if (gossipEntity != null)
                        {
                            String name = NameGenerator.name(gossipEntity);

                            for (Object2IntMap.Entry<VillageGossipType> typeEntry : associatedGossip.object2IntEntrySet())
                            {
                                Map.Entry<VillageGossipType, Integer> entry = (Map.Entry) typeEntry;
                                gossips.add(name + ": " + entry.getKey().asString() + " " + entry.getValue());
                            }
                        }
                    });

                Brain<?> brain = villager.getBrain();
                addPoi(brain, MemoryModuleType.HOME, pois);
                addPoi(brain, MemoryModuleType.JOB_SITE, pois);
                addPoi(brain, MemoryModuleType.MEETING_POINT, pois);
                addPoi(brain, MemoryModuleType.HIDING_PLACE, pois);
                addPoi(brain, MemoryModuleType.POTENTIAL_JOB_SITE, potentialPois);
            }
            else
            {
                profession = "";
                xp = 0;
                inventory = "";
                wantsGolem = false;
            }

            this.sendDebugData(serverWorld, new DebugBrainCustomPayload(new DebugBrainCustomPayload.Brain(
                    entity.getUuid(), entity.getId(), entity.getName().getString(),
                    profession, xp, entity.getHealth(), entity.getMaxHealth(),
                    entity.getPos(), inventory, entity.getNavigation().getCurrentPath(),
                    wantsGolem, angerLevel,
                    entity.getBrain().getPossibleActivities().stream().map(Activity::toString).toList(),
                    entity.getBrain().getRunningTasks().stream().map(Task::getName).toList(),
                    //this.listMemories(entity, serverWorld.getTime()),
                    memories,
                    gossips, pois, potentialPois)));
        }
    }
     */

    // FIXME -- WARNING!  Causes CustomPayload Crash
    /*
    public void sendBeeDebugData(ServerWorld world, BeeEntity bee)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        if (RendererToggle.DEBUG_BEEDATA.getBooleanValue())
        {
            this.sendDebugData(world, new DebugBeeCustomPayload(
                    new DebugBeeCustomPayload.Bee(bee.getUuid(), bee.getId(), bee.getPos(),
                                                  bee.getNavigation().getCurrentPath(), bee.getHivePos(), bee.getFlowerPos(), bee.getMoveGoalTicks(),
                                                  bee.getGoalSelector().getGoals().stream().map((prioritizedGoal) ->
                                                                                                        prioritizedGoal.getGoal().toString()).collect(Collectors.toSet()), bee.getPossibleHives())));

        }
    }
     */

    public void sendBreezeDebugData(ServerWorld world, BreezeEntity breeze)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        if (RendererToggle.DEBUG_BREEZE_JUMP.getBooleanValue())
        {
            this.sendDebugData(world, new DebugBreezeCustomPayload(new DebugBreezeCustomPayload.BreezeInfo(
                    breeze.getUuid(), breeze.getId(), breeze.getTarget() == null ? null : breeze.getTarget().getId(),
                    breeze.getBrain().getOptionalRegisteredMemory(MemoryModuleType.BREEZE_JUMP_TARGET).orElse(null))));
        }
    }

    public void sendGameEvent(ServerWorld world, RegistryEntry<GameEvent> event, Vec3d pos)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        if (RendererToggle.DEBUG_GAME_EVENT.getBooleanValue())
        {
            event.getKey().ifPresent((key) -> this.sendDebugData(world, new DebugGameEventCustomPayload(key, pos)));
        }
    }

    public void sendGameEventListener(ServerWorld world, GameEventListener eventListener)
    {
        if (this.isEnabled() == false)
        {
            return;
        }
        if (RendererToggle.DEBUG_GAME_EVENT.getBooleanValue())
        {
            this.sendDebugData(world, new DebugGameEventListenersCustomPayload(eventListener.getPositionSource(), eventListener.getRange()));
        }
    }

    // Tools
    private void addPoi(Brain<?> brain, MemoryModuleType<GlobalPos> memoryModuleType, Set<BlockPos> set)
    {
        Optional<BlockPos> opt = brain.getOptionalRegisteredMemory(memoryModuleType).map(GlobalPos::pos);

        Objects.requireNonNull(set);
        opt.ifPresent(set::add);
    }

    private List<String> listMemories(LivingEntity entity, long currentTime)
    {
        Map<MemoryModuleType<?>, Optional<? extends Memory<?>>> map = entity.getBrain().getMemories();
        List<String> list = Lists.newArrayList();

        for (Map.Entry<MemoryModuleType<?>, Optional<? extends Memory<?>>> memoryModuleTypeOptionalEntry : map.entrySet())
        {
            MemoryModuleType<?> memoryModuleType = memoryModuleTypeOptionalEntry.getKey();
            Optional<? extends Memory<?>> optional = memoryModuleTypeOptionalEntry.getValue();
            String string;

            if (optional.isPresent())
            {
                Memory<?> memory = optional.get();
                Object object = memory.getValue();

                if (memoryModuleType == MemoryModuleType.HEARD_BELL_TIME)
                {
                    long l = currentTime - (Long) object;
                    string = l + " ticks ago";
                }
                else if (memory.isTimed())
                {
                    String var10000 = this.format((ServerWorld) entity.getWorld(), object);
                    string = var10000 + " (ttl: " + memory.getExpiry() + ")";
                }
                else
                {
                    string = this.format((ServerWorld) entity.getWorld(), object);
                }
            }
            else
            {
                string = "-";
            }

            String type = Registries.MEMORY_MODULE_TYPE.getId(memoryModuleType).getPath();
            list.add(type + ": " + string);
        }

        list.sort(String::compareTo);
        return list;
    }

    private String format(ServerWorld world, @Nullable Object object)
    {
        if (object == null)
        {
            return "-";
        }
        else if (object instanceof UUID)
        {
            return format(world, world.getEntity((UUID) object));
        }
        else
        {
            Entity entity;
            if (object instanceof LivingEntity)
            {
                entity = (Entity) object;
                return NameGenerator.name(entity);
            }
            else if (object instanceof Nameable)
            {
                return ((Nameable) object).getName().getString();
            }
            else if (object instanceof WalkTarget)
            {
                return format(world, ((WalkTarget) object).getLookTarget());
            }
            else if (object instanceof EntityLookTarget)
            {
                return format(world, ((EntityLookTarget) object).getEntity());
            }
            else if (object instanceof GlobalPos)
            {
                return format(world, ((GlobalPos) object).pos());
            }
            else if (object instanceof BlockPosLookTarget)
            {
                return format(world, ((BlockPosLookTarget) object).getBlockPos());
            }
            else if (object instanceof DamageSource)
            {
                entity = ((DamageSource) object).getAttacker();
                return entity == null ? object.toString() : format(world, entity);
            }
            else if (!(object instanceof Collection))
            {
                return object.toString();
            }
            else
            {
                List<String> list = Lists.newArrayList();

                for (Object object2 : (Iterable) object)
                {
                    list.add(format(world, object2));
                }

                return list.toString();
            }
        }
    }
}
