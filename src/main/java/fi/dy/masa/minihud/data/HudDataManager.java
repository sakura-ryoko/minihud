package fi.dy.masa.minihud.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Pair;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.network.ClientPlayHandler;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.time.TickUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.mixin.world.IMixinServerRecipeManager;
import fi.dy.masa.minihud.network.ServuxHudHandler;
import fi.dy.masa.minihud.network.ServuxHudPacket;
import fi.dy.masa.minihud.renderer.OverlayRendererSpawnChunks;
import fi.dy.masa.minihud.util.DataStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;

public class HudDataManager
{
    private static final HudDataManager INSTANCE = new HudDataManager();

    private final static ServuxHudHandler<ServuxHudPacket.Payload> HANDLER = ServuxHudHandler.getInstance();
    private final Minecraft mc = Minecraft.getInstance();

    private boolean shouldRegister;
    private boolean servuxServer;
    private boolean hasInValidServux;
    private String servuxVersion;
    private int servuxProtocolVersion;

    private long worldSeed;
    private int spawnChunkRadius;
    private GlobalPos worldSpawn;

    private boolean worldSeedValid;
    private boolean spawnChunkRadiusValid;
    private boolean worldSpawnValid;

    private boolean isRaining;
    private boolean isThundering;
    private int clearWeatherTimer;
    private int rainWeatherTimer;
    private int thunderWeatherTimer;

    private RecipeMap preparedRecipes;
    private int recipeCount;

    public HudDataManager()
    {
        this.servuxServer = false;
        this.hasInValidServux = false;
        this.servuxVersion = "";
        this.worldSeed = -1;
        this.spawnChunkRadius = -1;
        this.worldSpawn = new GlobalPos(Level.OVERWORLD, BlockPos.ZERO);
        this.worldSeedValid = false;
        this.spawnChunkRadiusValid = false;
        this.worldSpawnValid = false;
        this.isRaining = false;
        this.isThundering = false;
        this.clearWeatherTimer = -1;
        this.rainWeatherTimer = -1;
        this.thunderWeatherTimer = -1;
        this.preparedRecipes = RecipeMap.EMPTY;
        this.recipeCount = 0;
    }

    public static HudDataManager getInstance() { return INSTANCE; }

    public void onGameInit()
    {
        ClientPlayHandler.getInstance().registerClientPlayHandler(HANDLER);
        HANDLER.registerPlayPayload(ServuxHudPacket.Payload.ID, ServuxHudPacket.Payload.CODEC, IPluginClientPlayHandler.BOTH_CLIENT);
    }

    public Identifier getNetworkChannel() { return ServuxHudHandler.CHANNEL_ID; }

    public IPluginClientPlayHandler<ServuxHudPacket.Payload> getNetworkHandler() { return HANDLER; }

    public void reset(boolean isLogout)
    {
        if (isLogout)
        {
            MiniHUD.debugLog("HudDataStorage#reset() - log-out");
            HANDLER.reset(this.getNetworkChannel());
            HANDLER.resetFailures(this.getNetworkChannel());

            this.servuxServer = false;
            this.hasInValidServux = false;
            this.servuxVersion = "";
            this.spawnChunkRadius = -1;
            this.worldSpawn = new GlobalPos(Level.OVERWORLD, BlockPos.ZERO);
            this.worldSpawnValid = false;
            this.spawnChunkRadiusValid = false;
            this.preparedRecipes = RecipeMap.EMPTY;
            this.recipeCount = 0;
        }
        else
        {
            MiniHUD.debugLog("HudDataStorage#reset() - dimension change or log-in");
        }

        this.resetWeatherData();

        if (isLogout || !Configs.Generic.DONT_RESET_SEED_ON_DIMENSION_CHANGE.getBooleanValue())
        {
            this.worldSeedValid = false;
            this.worldSeed = 0;
        }
    }

    public void resetWeatherData()
    {
        this.isRaining = false;
        this.isThundering = false;
        this.clearWeatherTimer = -1;
        this.rainWeatherTimer = -1;
        this.thunderWeatherTimer = -1;
    }

    public void onWorldPre()
    {
        if (!DataStorage.getInstance().hasIntegratedServer())
        {
            HANDLER.registerPlayReceiver(ServuxHudPacket.Payload.ID, HANDLER::receivePlayPayload);
        }
    }

    public void onWorldJoin()
    {
        MiniHUD.debugLog("HudDataStorage#onWorldJoin()");

        if (DataStorage.getInstance().hasIntegratedServer() == false)
        {
            if (Configs.Generic.HUD_DATA_SYNC.getBooleanValue())
            {
                this.requestMetadata();
            }
            else
            {
                this.unregisterChannel();
            }
        }
    }

    public void onPacketFailure()
    {
        // Define how to handle multiple sendPayload failures
        this.servuxServer = false;
        this.hasInValidServux = true;
    }

    public void setIsServuxServer()
    {
        this.servuxServer = true;
        if (this.hasInValidServux)
        {
            this.hasInValidServux = false;
        }
    }

    public void setServuxVersion(String ver, int protocol)
    {
        if (ver != null && !ver.isEmpty())
        {
            this.servuxVersion = ver;
            this.servuxProtocolVersion = protocol;
            MiniHUD.LOGGER.info("hudDataChannel: joining Servux version {}", ver);
        }
        else
        {
            this.servuxVersion = "unknown";
            this.servuxProtocolVersion = -1;
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

    public void setWorldSeed(long seed)
    {
        if (this.worldSeed != seed)
        {
            MiniHUD.debugLog("HudDataStorage#setWorldSeed(): set world seed [{}] -> [{}]", this.worldSeed, seed);
        }
        this.worldSeed = seed;
        this.worldSeedValid = true;
    }

    public void setWorldSpawn(GlobalPos spawn)
    {
        if (!this.worldSpawn.equals(spawn))
        {
            OverlayRendererSpawnChunks.INSTANCE_REAL.setNeedsUpdate();
            MiniHUD.debugLog("HudDataStorage#setWorldSpawn(): set world spawn [{}] -> [{}]", this.getWorldSpawnAsString(), this.getWorldSpawnAsString(spawn));
        }
        this.worldSpawn = spawn;
        this.worldSpawnValid = true;
    }

    public void setSpawnChunkRadius(int radius, boolean message)
    {
        if (radius == 0)
        {
            radius = -1;
        }

        if (radius >= -1 && radius <= 32)
        {
            if (this.spawnChunkRadius != radius)
            {
                if (message)
                {
                    String strRadius = GuiBase.TXT_GREEN + String.format("%d", radius) + GuiBase.TXT_RST;
                    InfoUtils.printActionbarMessage(StringUtils.translate("minihud.message.spawn_chunk_radius_set", strRadius));
                }

                OverlayRendererSpawnChunks.INSTANCE_REAL.setNeedsUpdate();
                MiniHUD.debugLog("HudDataStorage#setSpawnChunkRadius(): set spawn chunk radius [{}] -> [{}]", this.spawnChunkRadius, radius);
            }
            this.spawnChunkRadius = radius;
	        this.spawnChunkRadiusValid = radius > 0;
        }
        else
        {
            this.spawnChunkRadius = -1;
            this.spawnChunkRadiusValid = false;
        }
    }

    public void setWorldSpawnIfUnknown(GlobalPos spawn)
    {
        if (!this.worldSpawnValid)
        {
            this.setWorldSpawn(spawn);
            OverlayRendererSpawnChunks.INSTANCE_REAL.setNeedsUpdate();
        }
    }

    public void setSpawnChunkRadiusIfUnknown(int radius)
    {
        if (!this.spawnChunkRadiusValid)
        {
            this.setSpawnChunkRadius(radius, true);
            OverlayRendererSpawnChunks.INSTANCE_REAL.setNeedsUpdate();
        }
    }

    public boolean isWorldSeedKnown(Level world)
    {
        if (this.worldSeedValid)
        {
            return true;
        }
        else if (this.mc.hasSingleplayerServer())
        {
            MinecraftServer server = this.mc.getSingleplayerServer();
            assert server != null;
            Level worldTmp = server.getLevel(world.dimension());
            return worldTmp != null;
        }

        return false;
    }

    public boolean hasStoredWorldSeed()
    {
        return this.worldSeedValid;
    }

    public long worldSeed() { return this.worldSeed; }

    public long getWorldSeed(Level world)
    {
        if (!this.worldSeedValid && this.mc.hasSingleplayerServer())
        {
            MinecraftServer server = this.mc.getSingleplayerServer();
            assert server != null;
            ServerLevel worldTmp = server.getLevel(world.dimension());

            if (worldTmp != null)
            {
                this.setWorldSeed(worldTmp.getSeed());
            }
        }

        return this.worldSeed;
    }

    /**
     * This function checks the Integrated Server's World Seed at Server Launch.
     * This happens before the WorldLoadListener/fromJson load which works fine for Multiplayer;
     * But if we own the Server, use this value as valid, overriding the value from the JSON file.
     * This is because your default "New World" .json files' seed tends to eventually get stale
     * without using the /seed command continuously, or deleting the json files.
     * @param server (Server Object to get the data from)
     */
    public void checkWorldSeed(MinecraftServer server)
    {
        if (this.mc.hasSingleplayerServer())
        {
            ServerLevel worldTmp = server.overworld();

            if (worldTmp != null)
            {
                long seedTmp = worldTmp.getSeed();

                if (seedTmp != this.worldSeed)
                {
                    this.setWorldSeed(seedTmp);
                }
            }
        }
    }

    public boolean isWorldSpawnKnown()
    {
        return this.worldSpawnValid;
    }

    public GlobalPos getWorldSpawn()
    {
        return this.worldSpawn;
    }

    public String getWorldSpawnAsString()
    {
        GlobalPos pos = this.getWorldSpawn();

        return String.format("[%s: %d, %d, %d]", pos.dimension().identifier().toString(), pos.pos().getX(), pos.pos().getY(), pos.pos().getZ());
    }

    public String getWorldSpawnAsString(GlobalPos pos)
    {
        return String.format("[%s: %d, %d, %d]", pos.dimension().identifier().toString(), pos.pos().getX(), pos.pos().getY(), pos.pos().getZ());
    }

    public boolean isSpawnChunkRadiusKnown()
    {
        return this.spawnChunkRadiusValid;
    }

    public int getSpawnChunkRadius()
    {
	    return Math.max(this.spawnChunkRadius, -1);
    }

    public boolean hasValidWeatherCycle()
    {
        if (DataStorage.getInstance().hasIntegratedServer())
        {
            IntegratedServer server = DataStorage.getInstance().getIntegratedServer();

            return (server.overworld().getGameRules().get(GameRules.ADVANCE_WEATHER));
        }

        return this.getClearTime() >= 0 || this.getRainTime() >= 0 || this.getThunderTime() >= 0;
    }

    public boolean isWeatherClear()
    {
        return !this.isWeatherRain() && !this.isWeatherThunder();
    }

    public int getClearTime()
    {
        if (this.isWeatherClear())
        {
            return this.clearWeatherTimer;
        }

        return -1;
    }

    public boolean isWeatherRain()
    {
        return this.isRaining;
    }

    public int getRainTime()
    {
        if (this.isWeatherRain())
        {
            return this.rainWeatherTimer;
        }

        return -1;
    }

    public boolean isWeatherThunder()
    {
        return this.isThundering;
    }

    public int getThunderTime()
    {
        if (this.isWeatherThunder())
        {
            return this.thunderWeatherTimer;
        }

        return -1;
    }

    public boolean hasRecipes()
    {
        return !this.preparedRecipes.equals(RecipeMap.EMPTY);
    }

    public @Nullable RecipeMap getPreparedRecipes()
    {
        if (DataStorage.getInstance().hasIntegratedServer() && this.getRecipeManager() != null)
        {
            return ((IMixinServerRecipeManager) this.getRecipeManager()).minihud_getPreparedRecipes();
        }
        else if (this.hasRecipes())
        {
            return this.preparedRecipes;
        }

        return null;
    }

    public int getRecipeCount()
    {
        return this.recipeCount;
    }

    public @Nullable RecipeAccess getRecipeManager()
    {
        if (DataStorage.getInstance().hasIntegratedServer() && mc.getSingleplayerServer() != null)
        {
            return mc.getSingleplayerServer().getRecipeManager();
        }
        else if (mc.level != null)
        {
            return mc.level.recipeAccess();
        }

        return null;
    }

    public void onClientTickPost(Minecraft mc)
    {
        if (!DataStorage.getInstance().hasIntegratedServer())
        {
            if (this.clearWeatherTimer > 0)
            {
                this.clearWeatherTimer--;
            }
            if (this.rainWeatherTimer > 0)
            {
                this.rainWeatherTimer--;
            }
            if (this.thunderWeatherTimer > 0)
            {
                this.thunderWeatherTimer--;
            }
        }
    }

    public void onServerWeatherTick(int clearTime, int rainTime, int thunderTime, boolean isRaining, boolean isThunder)
    {
        this.clearWeatherTimer = clearTime;
        this.rainWeatherTimer = rainTime;
        this.thunderWeatherTimer = thunderTime;
        this.isRaining = isRaining;
        this.isThundering = isThunder;
    }

    public void onHudDataSyncToggled(ConfigBoolean config)
    {
        if (this.hasInValidServux)
        {
            this.reset(true);
        }

        if (this.hasServuxServer() && !config.getBooleanValue())
        {
            this.unregisterChannel();
        }
        else
        {
            this.shouldRegister = true;
            this.registerChannel();
        }
    }

    public void registerChannel()
    {
        this.shouldRegister = true;

        if (!this.hasServuxServer() && !DataStorage.getInstance().hasIntegratedServer() && !this.hasInValidServux)
        {
            this.onWorldPre();
            this.requestMetadata();
        }
        else
        {
            this.shouldRegister = false;
        }
    }

    public void requestMetadata()
    {
        if (!DataStorage.getInstance().hasIntegratedServer())
        {
            if (Configs.Generic.HUD_DATA_SYNC.getBooleanValue() &&
                HANDLER.isPlayRegistered(HANDLER.getPayloadChannel()))
            {
                CompoundTag nbt = new CompoundTag();
                nbt.putString("version", Reference.MOD_STRING);

                HANDLER.encodeClientData(ServuxHudPacket.MetadataRequest(nbt));
            }
        }
    }

    public ResourceKey<Level> getWorldType(String in)
    {
        return switch (in)
        {
            case "minecraft:the_nether" -> Level.NETHER;
            case "minecraft:the_end" -> Level.END;
            default -> Level.OVERWORLD;
        };
    }

    public boolean receiveMetadata(CompoundTag data)
    {
        if (!this.servuxServer && !DataStorage.getInstance().hasIntegratedServer() &&
            this.shouldRegister)
        {
            final int ver = data.getIntOr("version", -1);
            MiniHUD.debugLog("HudDataStorage#receiveMetadata(): received METADATA from Servux");

            if (ver != ServuxHudPacket.PROTOCOL_VERSION)
            {
                MiniHUD.LOGGER.warn("hudDataChannel: Mis-matched protocol version!");
            }

            this.setServuxVersion(data.getStringOr("servux", "?"), ver);
            this.setWorldSpawn(
                    new GlobalPos(
                            this.getWorldType(data.getString("spawnDimension").orElse(Level.OVERWORLD.identifier().toString())),
                            new BlockPos(data.getIntOr("spawnPosX", 0), data.getIntOr("spawnPosY", 0), data.getIntOr("spawnPosZ", 0)))
            );
//            this.setSpawnChunkRadius(data.getInt("spawnChunkRadius", 2), true);

            if (data.contains("worldSeed"))
            {
                this.setWorldSeed(data.getLongOr("worldSeed", -1L));
            }

            this.setIsServuxServer();

            if (Configs.Generic.HUD_DATA_SYNC.getBooleanValue())
            {
                //this.registerChannel();
                this.requestRecipeManager();
                this.refreshDataLoggers();
                return true;
            }
            else
            {
                this.unregisterChannel();
            }
        }

        return false;
    }

    public void unregisterChannel()
    {
        if (this.hasServuxServer() || !Configs.Generic.HUD_DATA_SYNC.getBooleanValue())
        {
            this.servuxServer = false;

            if (!this.hasInValidServux)
            {
                MiniHUD.debugLog("HudDataManager#unregisterChannel(): for {}", this.servuxVersion != null ? this.servuxVersion : "<unknown>");

                HANDLER.unregisterPlayReceiver();
                HANDLER.reset(HANDLER.getPayloadChannel());
            }
        }

        this.shouldRegister = false;
    }

    public void requestSpawnMetadata()
    {
        if (!DataStorage.getInstance().hasIntegratedServer() && this.hasServuxServer())
        {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("version", Reference.MOD_STRING);

            HANDLER.encodeClientData(ServuxHudPacket.SpawnRequest(nbt));
        }
    }

    public void receiveSpawnMetadata(CompoundTag data)
    {
        if (!DataStorage.getInstance().hasIntegratedServer())
        {
            final int ver = data.getIntOr("version", -1);
            MiniHUD.debugLog("HudDataStorage#receiveSpawnMetadata(): from Servux");

            this.setServuxVersion(data.getStringOr("servux", "?"), ver);
            this.setWorldSpawn(
                    new GlobalPos(
                            this.getWorldType(data.getString("spawnDimension").orElse(Level.OVERWORLD.identifier().toString())),
                            new BlockPos(data.getIntOr("spawnPosX", 0), data.getIntOr("spawnPosY", 0), data.getIntOr("spawnPosZ", 0)))
            );
//            this.setSpawnChunkRadius(data.getInt("spawnChunkRadius", 2), true);

            if (data.contains("worldSeed"))
            {
                this.setWorldSeed(data.getLongOr("worldSeed", -1L));
            }

            if (Configs.Generic.HUD_DATA_SYNC.getBooleanValue())
            {
                if (!this.hasServuxServer())
                {
                    this.registerChannel();
                }
            }
            else
            {
                this.unregisterChannel();
                this.shouldRegister = false;
            }
        }
    }

    public void refreshDataLoggers()
    {
        if (!DataStorage.getInstance().hasIntegratedServer() && this.hasServuxServer())
        {
            if (this.servuxProtocolVersion >= ServuxHudPacket.PROTOCOL_VERSION)
            {
                CompoundTag nbt = new CompoundTag();

                MiniHUD.debugLog("refreshDataLoggers: TPS: [{}] / MobCaps: [{}]", InfoToggle.SERVER_TPS.getBooleanValue(), InfoToggle.MOB_CAPS.getBooleanValue());
                nbt.putBoolean(ServuxDataLogger.TPS.name(), InfoToggle.SERVER_TPS.getBooleanValue());
                nbt.putBoolean(ServuxDataLogger.MOB_CAPS.name(), InfoToggle.MOB_CAPS.getBooleanValue());

                HANDLER.encodeClientData(ServuxHudPacket.DataLoggerRequest(nbt));
            }
            else
            {
                MiniHUD.LOGGER.warn("refreshDataLoggers: Incompatible Servux version detected!");
            }
        }
    }

    public void receiveDataLogger(CompoundTag nbt)
    {
        if (this.hasServuxServer() && nbt != null && !nbt.isEmpty())
        {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            for (String key : nbt.keySet())
            {
                ServuxDataLogger type = ServuxDataLogger.fromStringStatic(key);

                if (type != null)
                {
                    CompoundTag entry = (CompoundTag) nbt.read(key, type.codec()).orElse(null);

                    if (entry != null)
                    {
                        switch (type)
                        {
                            case TPS ->
                            {
                                try
                                {
                                    ServuxTickData data = ServuxTickData.CODEC.decode(mc.level.registryAccess().createSerializationContext(NbtOps.INSTANCE), entry).getOrThrow().getFirst();

//                                    MiniHUD.LOGGER.warn("Servux TPS: [{}], MSPT: [{}]",
//                                                        String.format(Locale.ROOT, "%.1f", data.tps()),
//                                                        String.format(Locale.ROOT, "%.1f", data.mspt())
//                                                       );

                                    if (data != null)
                                    {
                                        if (!TickUtils.getInstance().isUsingDirectServerData())
                                        {
                                            TickUtils.getInstance().toggleUseDirectServerData(true);
                                        }

                                        TickUtils.getInstance().updateNanoTickFromServux(data.tps(),
                                                                                         data.mspt(),
                                                                                         data.sprintTicks(),
                                                                                         data.frozen(),
                                                                                         data.sprinting(),
                                                                                         data.stepping()
                                        );
                                    }
                                }
                                catch (Exception err)
                                {
                                    MiniHUD.LOGGER.error("receiveDataLogger: TPS / Exception; {}", err.getLocalizedMessage());
                                }
                            }
                            case MOB_CAPS ->
                            {
                                String dimKey = mc.level.dimension().identifier().toString();

                                if (entry.contains(dimKey))
                                {
                                    // We are receiving MobCap Data for every dimension that is loaded;
                                    // but we only care about the one that we are in.
                                    CompoundTag nbtEntry = entry.getCompoundOrEmpty(dimKey);

                                    try
                                    {
                                        long worldTick = nbtEntry.getLongOr("WorldTick", mc.level.getGameTime());
                                        nbtEntry.remove("WorldTick");       // Not to confuse the Deserializer
                                        MobCapData serverData = MobCapData.CODEC.decode(mc.level.registryAccess().createSerializationContext(NbtOps.INSTANCE), nbtEntry).getOrThrow().getFirst();

                                        if (serverData != null)
                                        {
                                            DataStorage.getInstance().getMobCapData().setFromServuxData(serverData, worldTick);
                                        }
                                    }
                                    catch (Exception err)
                                    {
                                        MiniHUD.LOGGER.error("receiveDataLogger: MobCaps / Exception; {}", err.getLocalizedMessage());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void receiveWeatherData(CompoundTag data)
    {
        if (!DataStorage.getInstance().hasIntegratedServer())
        {
            //MiniHUD.printDebug("HudDataStorage#receiveWeatherData(): from Servux");

            if (data.contains("SetRaining"))
            {
                this.rainWeatherTimer = data.getIntOr("SetRaining", -1);
            }
            if (data.contains("isRaining"))
            {
                this.isRaining = data.getBooleanOr("isRaining", false);
            }
            if (data.contains("SetThundering"))
            {
                this.thunderWeatherTimer = data.getIntOr("SetThundering", -1);
            }
            if (data.contains("isThundering"))
            {
                this.isThundering = data.getBooleanOr("isThundering", false);
            }
            if (data.contains("SetClear"))
            {
                this.clearWeatherTimer = data.getIntOr("SetClear", -1);
            }

            if (!this.hasServuxServer() && DataStorage.getInstance().hasServuxServer())
            {
                // Backwards compat, the best effort.
                this.isThundering = this.thunderWeatherTimer > 0 && !this.isThundering;
                this.isRaining = this.rainWeatherTimer > 0 && !this.isRaining;
            }

            if (Configs.Generic.HUD_DATA_SYNC.getBooleanValue())
            {
                if (!this.hasServuxServer())
                {
                    this.registerChannel();
                }
            }
            else
            {
                this.unregisterChannel();
                this.shouldRegister = false;
            }
        }
    }

    public void requestRecipeManager()
    {
        if (!DataStorage.getInstance().hasIntegratedServer() && this.hasServuxServer())
        {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("version", Reference.MOD_STRING);

            HANDLER.encodeClientData(ServuxHudPacket.RecipeManagerRequest(nbt));
        }
    }

    public void receiveRecipeManager(CompoundTag data)
    {
        if (!DataStorage.getInstance().hasIntegratedServer() && data.contains("RecipeManager"))
        {
            Collection<RecipeHolder<?>> recipes = new ArrayList<>();
            ListTag list = data.getListOrEmpty("RecipeManager");
            int count = 0;

            this.preparedRecipes = RecipeMap.EMPTY;
            this.recipeCount = 0;

            for (int i = 0; i < list.size(); i++)
            {
                CompoundTag item = list.getCompoundOrEmpty(i);
                Identifier idReg = Identifier.tryParse(item.getStringOr("id_reg", ""));
                Identifier idValue = Identifier.tryParse(item.getStringOr("id_value", ""));

                if (idReg == null || idValue == null)
                {
                    continue;
                }

                try
                {
                    ResourceKey<Recipe<?>> key = ResourceKey.create(ResourceKey.createRegistryKey(idReg), idValue);
                    Pair<Recipe<?>, Tag> pair = Recipe.CODEC.decode(DataStorage.getInstance().getWorldRegistryManager().createSerializationContext(NbtOps.INSTANCE), item.getCompoundOrEmpty("recipe")).getOrThrow();
                    RecipeHolder<?> entry = new RecipeHolder<>(key, pair.getFirst());
                    recipes.add(entry);
                    count++;
                }
                catch (Exception e)
                {
                    MiniHUD.LOGGER.error("receiveRecipeManager: index [{}], Exception reading packet, {}", i, e.getMessage());
                }
            }

            if (!recipes.isEmpty())
            {
                this.preparedRecipes = RecipeMap.create(recipes);
                this.recipeCount = count;
                MiniHUD.debugLog("HudDataStorage#receiveRecipeManager(): finished loading Recipe Manager: Read [{}] Recipes from Servux", count);
            }
            else
            {
                MiniHUD.LOGGER.warn("receiveRecipeManager: failed to read Recipe Manager from Servux (Collection was empty!)");
            }

            if (Configs.Generic.HUD_DATA_SYNC.getBooleanValue())
            {
                if (!this.hasServuxServer())
                {
                    this.registerChannel();
                }
            }
            else
            {
                this.unregisterChannel();
                this.shouldRegister = false;
            }
        }
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        if (this.worldSeedValid)
        {
            obj.add("seed", new JsonPrimitive(this.worldSeed));
        }

        if (this.isSpawnChunkRadiusKnown())
        {
            obj.add("spawn_chunk_radius", new JsonPrimitive(this.spawnChunkRadius));
        }

        return obj;
    }

    /**
     * This function now checks for stale JSON data.
     * It only compares it if we have an Integrated Server running, and they are marked as valid.
     * @param obj ()
     */
    public void fromJson(JsonObject obj)
    {
        if (JsonUtils.hasLong(obj, "seed"))
        {
            long seedTmp = JsonUtils.getLong(obj, "seed");

            if (DataStorage.getInstance().hasIntegratedServer() && this.hasStoredWorldSeed() && this.worldSeed != seedTmp)
            {
                MiniHUD.debugLog("HudDataStorage#fromJson(): ignoring stale WorldSeed [{}], keeping [{}] as valid from the integrated server", seedTmp, this.worldSeed);
            }
            else
            {
                this.setWorldSeed(seedTmp);
            }
        }
        if (JsonUtils.hasInteger(obj, "spawn_chunk_radius"))
        {
            int spawnRadiusTmp = JsonUtils.getIntegerOrDefault(obj, "spawn_chunk_radius", -1);
//
//            if (DataStorage.getInstance().hasIntegratedServer() && this.isSpawnChunkRadiusKnown() && this.spawnChunkRadius != spawnRadiusTmp)
//            {
//                MiniHUD.debugLog("HudDataStorage#fromJson(): ignoring stale Spawn Chunk Radius [{}], keeping [{}] as valid from the integrated server", spawnRadiusTmp, this.spawnChunkRadius);
//            }
//            else
//            {
//                this.setSpawnChunkRadius(spawnRadiusTmp, false);
//            }
//
	        if (this.spawnChunkRadius != spawnRadiusTmp && spawnRadiusTmp > 0)
	        {
				this.setSpawnChunkRadius(spawnRadiusTmp, false);
	        }

//            // Force RenderToggle OFF if SPAWN_CHUNK_RADIUS is set to 0
//            if (this.getSpawnChunkRadius() == 0 && RendererToggle.OVERLAY_SPAWN_CHUNK_OVERLAY_REAL.getBooleanValue())
//            {
//                MiniHUD.LOGGER.warn("HudDataStorage#fromJson(): toggling feature OFF since SPAWN_CHUNK_RADIUS is set to 0");
//                RendererToggle.OVERLAY_SPAWN_CHUNK_OVERLAY_REAL.setBooleanValue(false);
//                OverlayRendererSpawnChunks.INSTANCE_REAL.setNeedsUpdate();
//            }
        }
    }
}
