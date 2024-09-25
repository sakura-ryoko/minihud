package fi.dy.masa.minihud.data;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.lib.apache.commons.tuple.Pair;

import com.mojang.datafixers.util.Either;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import fi.dy.masa.malilib.network.ClientPlayHandler;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.WorldUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.mixin.IMixinDataQueryHandler;
import fi.dy.masa.minihud.network.ServuxEntitiesHandler;
import fi.dy.masa.minihud.network.ServuxEntitiesPacket;
import fi.dy.masa.minihud.util.DataStorage;
import fi.dy.masa.minihud.util.EntityUtils;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EntitiesDataStorage implements IClientTickHandler
{
    private static final EntitiesDataStorage INSTANCE = new EntitiesDataStorage();

    public static EntitiesDataStorage getInstance()
    {
        return INSTANCE;
    }

    private final static ServuxEntitiesHandler<ServuxEntitiesPacket.Payload> HANDLER = ServuxEntitiesHandler.getInstance();
    private final static MinecraftClient mc = MinecraftClient.getInstance();
    private int uptimeTicks = 0;
    private boolean servuxServer = false;
    private boolean hasInValidServux = false;
    private String servuxVersion;

    private final ConcurrentHashMap<BlockPos, Pair<Long, NbtCompound>> blockEntityCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer,  Pair<Long, NbtCompound>> entityCache      = new ConcurrentHashMap<>();
    private long cacheTimeout = 10;
    private long serverTickTime = 0;
    // Requests to be executed
    private final Set<BlockPos> pendingBlockEntitiesQueue = new LinkedHashSet<>();
    private final Set<Integer> pendingEntitiesQueue = new LinkedHashSet<>();
    // To save vanilla query packet transaction
    private final Map<Integer, Either<BlockPos, Integer>> transactionToBlockPosOrEntityId = new HashMap<>();
    private ClientWorld clientWorld;

    @Nullable
    public World getWorld()
    {
        return WorldUtils.getBestWorld(mc);
    }

    private ClientWorld getClientWorld()
    {
        if (this.clientWorld == null)
        {
            clientWorld = mc.world;
        }

        return clientWorld;
    }

    private EntitiesDataStorage() { }

    @Override
    public void onClientTick(MinecraftClient mc)
    {
        this.uptimeTicks++;
        if (System.currentTimeMillis() - this.serverTickTime > 50)
        {
            // In this block, we do something every server tick
            if (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue() == false)
            {
                this.serverTickTime = System.currentTimeMillis();
                if (DataStorage.getInstance().hasIntegratedServer() == false && this.hasServuxServer())
                {
                    this.servuxServer = false;
                    HANDLER.unregisterPlayReceiver();
                }
                return;
            }
            else if (DataStorage.getInstance().hasIntegratedServer() == false &&
                    this.hasServuxServer() == false &&
                    this.hasInValidServux == false &&
                    this.getWorld() != null)
            {
                // Make sure we're Play Registered, and request Metadata
                HANDLER.registerPlayReceiver(ServuxEntitiesPacket.Payload.ID, HANDLER::receivePlayPayload);
                this.requestMetadata();
            }

            // Expire cached NBT
            this.tickCache();

            // 5 queries / server tick
            for (int i = 0; i < Configs.Generic.SERVER_NBT_REQUEST_RATE.getIntegerValue(); i++)
            {
                if (!this.pendingBlockEntitiesQueue.isEmpty())
                {
                    var iter = this.pendingBlockEntitiesQueue.iterator();
                    BlockPos pos = iter.next();
                    iter.remove();
                    if (this.hasServuxServer())
                    {
                        requestServuxBlockEntityData(pos);
                    }
                    else
                    {
                        requestQueryBlockEntity(pos);
                    }
                }
                if (!this.pendingEntitiesQueue.isEmpty())
                {
                    var iter = this.pendingEntitiesQueue.iterator();
                    int entityId = iter.next();
                    iter.remove();
                    if (this.hasServuxServer())
                    {
                        requestServuxEntityData(entityId);
                    }
                    else
                    {
                        requestQueryEntityData(entityId);
                    }
                }
            }
            this.serverTickTime = System.currentTimeMillis();
        }
    }

    public Identifier getNetworkChannel()
    {
        return ServuxEntitiesHandler.CHANNEL_ID;
    }

    private static ClientPlayNetworkHandler getVanillaHandler()
    {
        if (mc.player != null)
        {
            return mc.player.networkHandler;
        }

        return null;
    }

    public IPluginClientPlayHandler<ServuxEntitiesPacket.Payload> getNetworkHandler()
    {
        return HANDLER;
    }

    public void reset(boolean isLogout)
    {
        if (isLogout)
        {
            MiniHUD.printDebug("EntitiesDataStorage#reset() - log-out");
            HANDLER.reset(this.getNetworkChannel());
            HANDLER.resetFailures(this.getNetworkChannel());
            this.servuxServer = false;
            this.hasInValidServux = false;
        }
        else
        {
            MiniHUD.printDebug("EntitiesDataStorage#reset() - dimension change or log-in");
        }
        // Clear data
        this.blockEntityCache.clear();
        this.entityCache.clear();
    }

    private void tickCache()
    {
        long nowTime = System.currentTimeMillis();
        long timeout = this.cacheTimeout * 1000L;

        synchronized (this.blockEntityCache)
        {
            for (BlockPos pos : this.blockEntityCache.keySet())
            {
                Pair<Long, NbtCompound> pair = this.blockEntityCache.get(pos);

                if (nowTime - pair.getLeft() > timeout || pair.getLeft() - nowTime > 0)
                {
                    System.out.printf("SYNC: be at pos [%s] has timed out\n", pos.toShortString());
                    this.blockEntityCache.remove(pos);
                }
            }
        }
        synchronized (this.entityCache)
        {
            for (Integer entityId : this.entityCache.keySet())
            {
                Pair<Long, NbtCompound> pair = this.entityCache.get(entityId);

                if (nowTime - pair.getLeft() > timeout || pair.getLeft() - nowTime > 0)
                {
                    System.out.printf("SYNC: enity Id [%d] has timed out\n", entityId);
                    this.entityCache.remove(entityId);
                }
            }
        }
    }

    public @Nullable NbtCompound getFromBlockEntityCache(BlockPos pos)
    {
        if (this.blockEntityCache.containsKey(pos))
        {
            return this.blockEntityCache.get(pos).getRight();
        }

        return null;
    }

    public @Nullable NbtCompound getFromEntityCache(int entityId)
    {
        if (this.entityCache.containsKey(entityId))
        {
            return this.entityCache.get(entityId).getRight();
        }

        return null;
    }

    public void setIsServuxServer()
    {
        this.servuxServer = true;
        this.hasInValidServux = false;
    }

    public boolean hasServuxServer()
    {
        return this.servuxServer;
    }

    public void setServuxVersion(String ver)
    {
        if (ver != null && ver.isEmpty() == false)
        {
            this.servuxVersion = ver;
            MiniHUD.printDebug("entityDataChannel: joining Servux version {}", ver);
        }
        else
        {
            this.servuxVersion = "unknown";
        }
    }

    public String getServuxVersion()
    {
        return servuxVersion;
    }

    public int getPendingBLockEntitiesCount()
    {
        return this.pendingBlockEntitiesQueue.size();
    }

    public int getPendingEntitiesCount()
    {
        return this.pendingEntitiesQueue.size();
    }

    public void onGameInit()
    {
        ClientPlayHandler.getInstance().registerClientPlayHandler(HANDLER);
        HANDLER.registerPlayPayload(ServuxEntitiesPacket.Payload.ID, ServuxEntitiesPacket.Payload.CODEC, IPluginClientPlayHandler.BOTH_CLIENT);
    }

    public void onWorldPre()
    {
        if (DataStorage.getInstance().hasIntegratedServer() == false)
        {
            HANDLER.registerPlayReceiver(ServuxEntitiesPacket.Payload.ID, HANDLER::receivePlayPayload);
        }
    }

    public void onWorldJoin()
    {
        // NO-OP
    }

    public void requestMetadata()
    {
        if (DataStorage.getInstance().hasIntegratedServer() == false &&
            Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue())
        {
            NbtCompound nbt = new NbtCompound();
            nbt.putString("version", Reference.MOD_STRING);

            HANDLER.encodeClientData(ServuxEntitiesPacket.MetadataRequest(nbt));
        }
    }

    public boolean receiveServuxMetadata(NbtCompound data)
    {
        if (DataStorage.getInstance().hasIntegratedServer() == false)
        {
            MiniHUD.printDebug("EntitiesDataStorage#receiveServuxMetadata(): received METADATA from Servux");

            if (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue())
            {
                if (data.getInt("version") != ServuxEntitiesPacket.PROTOCOL_VERSION)
                {
                    MiniHUD.logger.warn("entityDataChannel: Mis-matched protocol version!");
                }

                this.setServuxVersion(data.getString("servux"));
                this.setIsServuxServer();

                return true;
            }
        }

        return false;
    }

    public void onPacketFailure()
    {
        this.servuxServer = false;
        this.hasInValidServux = true;
    }

    /**
     * These should function with the ServerWorld in Single Player
     * @param entityId
     * @return
     */
    public NbtCompound requestBlockEntity(World world, BlockPos pos)
    {
        if (this.blockEntityCache.containsKey(pos))
        {
            return this.blockEntityCache.get(pos).getRight();
        }
        else if (world.getBlockState(pos).getBlock() instanceof BlockEntityProvider)
        {
            if (world instanceof ServerWorld)
            {
                BlockEntity be = world.getWorldChunk(pos).getBlockEntity(pos);

                if (be != null)
                {
                    NbtCompound nbt = be.createNbtWithIdentifyingData(world.getRegistryManager());
                    this.blockEntityCache.put(pos, Pair.of(System.currentTimeMillis(), nbt));
                    return nbt;
                }
            }
            else if (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue())
            {
                this.pendingBlockEntitiesQueue.add(pos);
            }
        }

        return new NbtCompound();
    }

    /**
     * These should function with the ServerWorld in Single Player
     * @param entityId
     * @return
     */
    public NbtCompound requestEntity(int entityId)
    {
        if (this.entityCache.containsKey(entityId))
        {
            return this.entityCache.get(entityId).getRight();
        }
        else if (this.getWorld() instanceof ServerWorld)
        {
            Entity entity = this.getWorld().getEntityById(entityId);
            NbtCompound nbt = new NbtCompound();

            if (entity.saveSelfNbt(nbt))
            {
                this.entityCache.put(entityId, Pair.of(System.currentTimeMillis(), nbt));
                return nbt;
            }
        }
        else if (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue())
        {
            this.pendingEntitiesQueue.add(entityId);
        }

        return new NbtCompound();
    }

    @Nullable
    public Inventory getBlockInventory(World world, BlockPos pos)
    {
        if (this.blockEntityCache.containsKey(pos))
        {
            Inventory inv = InventoryUtils.getNbtInventory(this.blockEntityCache.get(pos).getRight(), -1, world.getRegistryManager());

            if (inv != null)
            {
                return inv;
            }
        }

        if (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue())
        {
            this.requestBlockEntity(world, pos);
        }
        return null;
    }

    @Nullable
    public Inventory getEntityInventory(int entityId)
    {
        if (this.entityCache.containsKey(entityId) && this.getWorld() != null)
        {
            Inventory inv = InventoryUtils.getNbtInventory(this.entityCache.get(entityId).getRight(), -1, this.getWorld().getRegistryManager());

            if (inv != null)
            {
                return inv;
            }
        }

        if (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue())
        {
            this.requestEntity(entityId);
        }
        return null;
    }

    private void requestQueryBlockEntity(BlockPos pos)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue() == false)
        {
            return;
        }

        ClientPlayNetworkHandler handler = this.getVanillaHandler();

        if (handler != null)
        {
            handler.getDataQueryHandler().queryBlockNbt(pos, nbtCompound ->
            {
                handleBlockEntityData(pos, nbtCompound, null);
            });
            this.transactionToBlockPosOrEntityId.put(((IMixinDataQueryHandler) handler.getDataQueryHandler()).minihud_currentTransactionId(), Either.left(pos));
        }
    }

    private void requestQueryEntityData(int entityId)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue() == false)
        {
            return;
        }

        ClientPlayNetworkHandler handler = this.getVanillaHandler();

        if (handler != null)
        {
            handler.getDataQueryHandler().queryEntityNbt(entityId, nbtCompound ->
            {
                handleEntityData(entityId, nbtCompound);
            });
            this.transactionToBlockPosOrEntityId.put(((IMixinDataQueryHandler) handler.getDataQueryHandler()).minihud_currentTransactionId(), Either.right(entityId));
        }
    }

    private void requestServuxBlockEntityData(BlockPos pos)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue())
        {
            HANDLER.encodeClientData(ServuxEntitiesPacket.BlockEntityRequest(pos));
        }
    }

    private void requestServuxEntityData(int entityId)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue())
        {
            HANDLER.encodeClientData(ServuxEntitiesPacket.EntityRequest(entityId));
        }
    }

    @Nullable
    public BlockEntity handleBlockEntityData(BlockPos pos, NbtCompound nbt, @Nullable Identifier type)
    {
        this.pendingBlockEntitiesQueue.remove(pos);
        if (nbt == null || this.getClientWorld() == null) return null;

        BlockEntity blockEntity = this.getClientWorld().getBlockEntity(pos);

        if (blockEntity != null && (type == null || type.equals(BlockEntityType.getId(blockEntity.getType()))))
        {
            if (this.blockEntityCache.containsKey(pos))
            {
                this.blockEntityCache.replace(pos, Pair.of(System.currentTimeMillis(), nbt));
            }
            else
            {
                this.blockEntityCache.put(pos, Pair.of(System.currentTimeMillis(), nbt));
            }

            blockEntity.read(nbt, this.getClientWorld().getRegistryManager());
            return blockEntity;
        }

        Optional<RegistryEntry.Reference<BlockEntityType<?>>> opt = Registries.BLOCK_ENTITY_TYPE.getEntry(type);

        if (opt.isPresent())
        {
            BlockEntityType<?> beType = opt.get().value();

            if (beType.supports(this.getClientWorld().getBlockState(pos)))
            {
                BlockEntity blockEntity2 = beType.instantiate(pos, this.getClientWorld().getBlockState(pos));

                if (blockEntity2 != null)
                {
                    if (this.blockEntityCache.containsKey(pos))
                    {
                        this.blockEntityCache.replace(pos, Pair.of(System.currentTimeMillis(), nbt));
                    }
                    else
                    {
                        this.blockEntityCache.put(pos, Pair.of(System.currentTimeMillis(), nbt));
                    }

                    blockEntity2.read(nbt, this.getClientWorld().getRegistryManager());
                    this.getClientWorld().addBlockEntity(blockEntity2);

                    return blockEntity2;
                }
            }
        }

        return null;
    }

    @Nullable
    public Entity handleEntityData(int entityId, NbtCompound nbt)
    {
        this.pendingEntitiesQueue.remove(entityId);
        if (nbt == null || this.getClientWorld() == null) return null;
        Entity entity = this.getClientWorld().getEntityById(entityId);

        if (entity != null)
        {
            if (this.entityCache.containsKey(entityId))
            {
                this.entityCache.replace(entityId, Pair.of(System.currentTimeMillis(), nbt));
            }
            else
            {
                this.entityCache.put(entityId, Pair.of(System.currentTimeMillis(), nbt));
            }

            EntityUtils.loadNbtIntoEntity(entity, nbt);
        }
        return entity;
    }

    public void handleBulkEntityData(int transactionId, NbtCompound nbt)
    {
        // todo
    }

    public void handleVanillaQueryNbt(int transactionId, NbtCompound nbt)
    {
        Either<BlockPos, Integer> either = this.transactionToBlockPosOrEntityId.remove(transactionId);
        if (either != null)
        {
            either.ifLeft(pos -> handleBlockEntityData(pos, nbt, null))
                    .ifRight(entityId -> handleEntityData(entityId, nbt));
        }
    }

    // TODO --> Only in case we need to save config settings in the future
    public JsonObject toJson()
    {
        return new JsonObject();
    }

    public void fromJson(JsonObject obj)
    {
        // NO-OP
    }
}
