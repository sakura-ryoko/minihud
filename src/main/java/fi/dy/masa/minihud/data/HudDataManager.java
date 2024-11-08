package fi.dy.masa.minihud.data;

import java.util.ArrayList;
import java.util.Collection;
import javax.annotation.Nullable;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.recipe.PreparedRecipes;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.network.ClientPlayHandler;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.mixin.IMixinServerRecipeManager;
import fi.dy.masa.minihud.network.ServuxHudHandler;
import fi.dy.masa.minihud.network.ServuxHudPacket;
import fi.dy.masa.minihud.network.ServuxStructuresPacket;
import fi.dy.masa.minihud.renderer.OverlayRendererSpawnChunks;
import fi.dy.masa.minihud.util.DataStorage;

public class HudDataManager
{
    private static final HudDataManager INSTANCE = new HudDataManager();

    private final static ServuxHudHandler<ServuxHudPacket.Payload> HANDLER = ServuxHudHandler.getInstance();
    private final MinecraftClient mc = MinecraftClient.getInstance();

    private boolean servuxServer;
    private boolean hasInValidServux;
    private String servuxVersion;

    private long worldSeed;
    private int spawnChunkRadius;
    private BlockPos worldSpawn;

    private boolean worldSeedValid;
    private boolean spawnChunkRadiusValid;
    private boolean worldSpawnValid;

    private boolean isRaining;
    private boolean isThundering;
    private int clearWeatherTimer;
    private int rainWeatherTimer;
    private int thunderWeatherTimer;

    private PreparedRecipes preparedRecipes;
    private int recipeCount;

    public HudDataManager()
    {
        this.servuxServer = false;
        this.hasInValidServux = false;
        this.servuxVersion = "";
        this.worldSeed = -1;
        this.spawnChunkRadius = -1;
        this.worldSpawn = BlockPos.ORIGIN;
        this.worldSeedValid = false;
        this.spawnChunkRadiusValid = false;
        this.worldSpawnValid = false;
        this.isRaining = false;
        this.isThundering = false;
        this.clearWeatherTimer = -1;
        this.rainWeatherTimer = -1;
        this.thunderWeatherTimer = -1;
        this.preparedRecipes = PreparedRecipes.EMPTY;
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
            MiniHUD.printDebug("HudDataStorage#reset() - log-out");
            HANDLER.reset(this.getNetworkChannel());
            HANDLER.resetFailures(this.getNetworkChannel());

            this.servuxServer = false;
            this.hasInValidServux = false;
            this.servuxVersion = "";
            this.spawnChunkRadius = -1;
            this.worldSpawn = BlockPos.ORIGIN;
            this.worldSpawnValid = false;
            this.spawnChunkRadiusValid = false;
            this.preparedRecipes = PreparedRecipes.EMPTY;
            this.recipeCount = 0;
        }

        this.isRaining = false;
        this.isThundering = false;
        this.clearWeatherTimer = -1;
        this.rainWeatherTimer = -1;
        this.thunderWeatherTimer = -1;

        if (isLogout || !Configs.Generic.DONT_RESET_SEED_ON_DIMENSION_CHANGE.getBooleanValue())
        {
            this.worldSeedValid = false;
            this.worldSeed = 0;
        }
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
        MiniHUD.printDebug("HudDataStorage#onWorldJoin()");
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

    public void setWorldSeed(long seed)
    {
        if (this.worldSeed != seed)
        {
            MiniHUD.printDebug("HudDataStorage#setWorldSeed(): set world seed [{}] -> [{}]", this.worldSeed, seed);
        }
        this.worldSeed = seed;
        this.worldSeedValid = true;
    }

    public void setWorldSpawn(BlockPos spawn)
    {
        if (!this.worldSpawn.equals(spawn))
        {
            OverlayRendererSpawnChunks.setNeedsUpdate();
            MiniHUD.printDebug("HudDataStorage#setWorldSpawn(): set world spawn [{}] -> [{}]", this.worldSpawn.toShortString(), spawn.toShortString());
        }
        this.worldSpawn = spawn;
        this.worldSpawnValid = true;
    }

    public void setSpawnChunkRadius(int radius, boolean message)
    {
        if (radius >= 0 && radius <= 32)
        {
            if (this.spawnChunkRadius != radius)
            {
                if (message)
                {
                    String strRadius = radius > 0 ? GuiBase.TXT_GREEN + String.format("%d", radius) + GuiBase.TXT_RST : GuiBase.TXT_RED + String.format("%d", radius) + GuiBase.TXT_RST;
                    InfoUtils.printActionbarMessage(StringUtils.translate("minihud.message.spawn_chunk_radius_set", strRadius));
                }

                OverlayRendererSpawnChunks.setNeedsUpdate();
                MiniHUD.printDebug("HudDataStorage#setSpawnChunkRadius(): set spawn chunk radius [{}] -> [{}]", this.spawnChunkRadius, radius);
            }
            this.spawnChunkRadius = radius;
            this.spawnChunkRadiusValid = true;
        }
        else
        {
            this.spawnChunkRadius = -1;
            this.spawnChunkRadiusValid = false;
        }
    }

    public void setWorldSpawnIfUnknown(BlockPos spawn)
    {
        if (!this.worldSpawnValid)
        {
            this.setWorldSpawn(spawn);
            OverlayRendererSpawnChunks.setNeedsUpdate();
        }
    }

    public void setSpawnChunkRadiusIfUnknown(int radius)
    {
        if (!this.spawnChunkRadiusValid)
        {
            this.setSpawnChunkRadius(radius, true);
            OverlayRendererSpawnChunks.setNeedsUpdate();
        }
    }

    public boolean isWorldSeedKnown(World world)
    {
        if (this.worldSeedValid)
        {
            return true;
        }
        else if (this.mc.isIntegratedServerRunning())
        {
            MinecraftServer server = this.mc.getServer();
            assert server != null;
            World worldTmp = server.getWorld(world.getRegistryKey());
            return worldTmp != null;
        }

        return false;
    }

    public boolean hasStoredWorldSeed()
    {
        return this.worldSeedValid;
    }

    public long worldSeed() { return this.worldSeed; }

    public long getWorldSeed(World world)
    {
        if (!this.worldSeedValid && this.mc.isIntegratedServerRunning())
        {
            MinecraftServer server = this.mc.getServer();
            assert server != null;
            ServerWorld worldTmp = server.getWorld(world.getRegistryKey());

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
        if (this.mc.isIntegratedServerRunning())
        {
            ServerWorld worldTmp = server.getOverworld();

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

    public BlockPos getWorldSpawn()
    {
        return this.worldSpawn;
    }

    public boolean isSpawnChunkRadiusKnown()
    {
        return this.spawnChunkRadiusValid;
    }

    public int getSpawnChunkRadius()
    {
        if (this.spawnChunkRadius > -1)
        {
            return this.spawnChunkRadius;
        }

        return 2;
    }

    public boolean isWeatherClear()
    {
        return this.getClearTime() > -1;
    }

    public int getClearTime()
    {
        if (!this.isRaining && !this.isThundering)
        {
            return this.clearWeatherTimer;
        }

        return -1;
    }

    public boolean isWeatherRain()
    {
        return this.isRaining && this.getRainTime() > -1;
    }

    public int getRainTime()
    {
        if (this.isRaining && this.rainWeatherTimer > -1)
        {
            return this.rainWeatherTimer;
        }

        return -1;
    }

    public boolean isWeatherThunder()
    {
        return this.isThundering && this.getThunderTime() > -1;
    }

    public int getThunderTime()
    {
        if (this.isThundering && this.thunderWeatherTimer > -1)
        {
            return this.thunderWeatherTimer;
        }

        return -1;
    }

    public boolean hasRecipes()
    {
        return !this.preparedRecipes.equals(PreparedRecipes.EMPTY);
    }

    public @Nullable PreparedRecipes getPreparedRecipes()
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

    public @Nullable RecipeManager getRecipeManager()
    {
        if (DataStorage.getInstance().hasIntegratedServer() && mc.getServer() != null)
        {
            return mc.getServer().getRecipeManager();
        }
        else if (mc.world != null)
        {
            return mc.world.getRecipeManager();
        }

        return null;
    }

    public void onClientTickPost(MinecraftClient mc)
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

    public boolean receiveMetadata(NbtCompound data)
    {
        if (!this.servuxServer && !DataStorage.getInstance().hasIntegratedServer())
        {
            MiniHUD.printDebug("HudDataStorage#receiveMetadata(): received METADATA from Servux");

            if (data.getInt("version") != ServuxStructuresPacket.PROTOCOL_VERSION)
            {
                MiniHUD.logger.warn("hudDataChannel: Mis-matched protocol version!");
            }

            this.setServuxVersion(data.getString("servux"));
            this.setWorldSpawn(new BlockPos(data.getInt("spawnPosX"), data.getInt("spawnPosY"), data.getInt("spawnPosZ")));
            this.setSpawnChunkRadius(data.getInt("spawnChunkRadius"), true);

            if (data.contains("worldSeed", Constants.NBT.TAG_LONG))
            {
                this.setWorldSeed(data.getLong("worldSeed"));
            }

            this.setIsServuxServer();
            this.requestRecipeManager();

            return true;
        }

        return false;
    }

    public void requestSpawnMetadata()
    {
        if (!DataStorage.getInstance().hasIntegratedServer() && this.hasServuxServer())
        {
            NbtCompound nbt = new NbtCompound();
            nbt.putString("version", Reference.MOD_STRING);

            HANDLER.encodeClientData(ServuxHudPacket.SpawnRequest(nbt));
        }
    }

    public void receiveSpawnMetadata(NbtCompound data)
    {
        if (!DataStorage.getInstance().hasIntegratedServer())
        {
            MiniHUD.printDebug("HudDataStorage#receiveSpawnMetadata(): from Servux");

            this.setServuxVersion(data.getString("servux"));
            this.setWorldSpawn(new BlockPos(data.getInt("spawnPosX"), data.getInt("spawnPosY"), data.getInt("spawnPosZ")));
            this.setSpawnChunkRadius(data.getInt("spawnChunkRadius"), true);
            if (data.contains("worldSeed", Constants.NBT.TAG_LONG))
            {
                this.setWorldSeed(data.getLong("worldSeed"));
            }
        }
    }

    public void receiveWeatherData(NbtCompound data)
    {
        if (!DataStorage.getInstance().hasIntegratedServer())
        {
            //MiniHUD.printDebug("HudDataStorage#receiveWeatherData(): from Servux");

            if (data.contains("SetRaining", Constants.NBT.TAG_INT))
            {
                this.rainWeatherTimer = data.getInt("SetRaining");
            }
            if (data.contains("isRaining"))
            {
                this.isRaining = data.getBoolean("isRaining");
            }
            if (data.contains("SetThundering", Constants.NBT.TAG_INT))
            {
                this.thunderWeatherTimer = data.getInt("SetThundering");
            }
            if (data.contains("isThundering"))
            {
                this.isThundering = data.getBoolean("isThundering");
            }
            if (data.contains("SetClear", Constants.NBT.TAG_INT))
            {
                this.clearWeatherTimer = data.getInt("SetClear");
            }

            if (!this.hasServuxServer() && DataStorage.getInstance().hasServuxServer())
            {
                // Backwards compat, the best effort.
                this.isThundering = this.thunderWeatherTimer > 0 && !this.isThundering;
                this.isRaining = this.rainWeatherTimer > 0 && !this.isRaining;
            }
        }
    }

    public void requestRecipeManager()
    {
        if (!DataStorage.getInstance().hasIntegratedServer() && this.hasServuxServer())
        {
            NbtCompound nbt = new NbtCompound();
            nbt.putString("version", Reference.MOD_STRING);

            HANDLER.encodeClientData(ServuxHudPacket.RecipeManagerRequest(nbt));
        }
    }

    public void receiveRecipeManager(NbtCompound data)
    {
        if (!DataStorage.getInstance().hasIntegratedServer() && data.contains("RecipeManager"))
        {
            Collection<RecipeEntry<?>> recipes = new ArrayList<>();
            NbtList list = data.getList("RecipeManager", Constants.NBT.TAG_COMPOUND);
            int count = 0;

            this.preparedRecipes = PreparedRecipes.EMPTY;
            this.recipeCount = 0;

            for (int i = 0; i < list.size(); i++)
            {
                NbtCompound item = list.getCompound(i);
                Identifier idReg = Identifier.tryParse(item.getString("id_reg"));
                Identifier idValue = Identifier.tryParse(item.getString("id_value"));

                if (idReg == null || idValue == null)
                {
                    continue;
                }

                try
                {
                    RegistryKey<Recipe<?>> key = RegistryKey.of(RegistryKey.ofRegistry(idReg), idValue);
                    Pair<Recipe<?>, NbtElement> pair = Recipe.CODEC.decode(DataStorage.getInstance().getWorldRegistryManager().getOps(NbtOps.INSTANCE), item.getCompound("recipe")).getOrThrow();
                    RecipeEntry<?> entry = new RecipeEntry<>(key, pair.getFirst());
                    recipes.add(entry);
                    count++;
                }
                catch (Exception e)
                {
                    MiniHUD.logger.error("receiveRecipeManager: index [{}], Exception reading packet, {}", i, e.getMessage());
                }
            }

            if (!recipes.isEmpty())
            {
                this.preparedRecipes = PreparedRecipes.of(recipes);
                this.recipeCount = count;
                MiniHUD.printDebug("HudDataStorage#receiveRecipeManager(): finished loading Recipe Manager: Read [{}] Recipes from Servux", count);
            }
            else
            {
                MiniHUD.logger.warn("receiveRecipeManager: failed to read Recipe Manager from Servux (Collection was empty!)");
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
                MiniHUD.printDebug("HudDataStorage#fromJson(): ignoring stale WorldSeed [{}], keeping [{}] as valid from the integrated server", seedTmp, this.worldSeed);
            }
            else
            {
                this.setWorldSeed(seedTmp);
            }
        }
        if (JsonUtils.hasInteger(obj, "spawn_chunk_radius"))
        {
            int spawnRadiusTmp = JsonUtils.getIntegerOrDefault(obj, "spawn_chunk_radius", 2);

            if (DataStorage.getInstance().hasIntegratedServer() && this.isSpawnChunkRadiusKnown() && this.spawnChunkRadius != spawnRadiusTmp)
            {
                MiniHUD.printDebug("HudDataStorage#fromJson(): ignoring stale Spawn Chunk Radius [{}], keeping [{}] as valid from the integrated server", spawnRadiusTmp, this.spawnChunkRadius);
            }
            else
            {
                this.setSpawnChunkRadius(spawnRadiusTmp, false);
            }

            // Force RenderToggle OFF if SPAWN_CHUNK_RADIUS is set to 0
            if (this.getSpawnChunkRadius() == 0 && RendererToggle.OVERLAY_SPAWN_CHUNK_OVERLAY_REAL.getBooleanValue())
            {
                MiniHUD.logger.warn("HudDataStorage#fromJson(): toggling feature OFF since SPAWN_CHUNK_RADIUS is set to 0");
                RendererToggle.OVERLAY_SPAWN_CHUNK_OVERLAY_REAL.setBooleanValue(false);
                OverlayRendererSpawnChunks.setNeedsUpdate();
            }
        }
    }
}
