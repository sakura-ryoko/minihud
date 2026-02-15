package fi.dy.masa.minihud.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import com.google.common.collect.ArrayListMultimap;
import com.google.gson.JsonObject;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.network.ClientPlayHandler;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.position.PositionUtils;
import fi.dy.masa.malilib.util.time.TickUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.data.HudDataManager;
import fi.dy.masa.minihud.data.MobCapDataHandler;
import fi.dy.masa.minihud.mixin.client.IMixinOptions;
import fi.dy.masa.minihud.mixin.server.IMixinMinecraftServer;
import fi.dy.masa.minihud.network.ServuxStructuresHandler;
import fi.dy.masa.minihud.network.ServuxStructuresPacket;
import fi.dy.masa.minihud.renderer.*;
import fi.dy.masa.minihud.renderer.shapes.ShapeManager;

public class DataStorage
{
    private static final Pattern PATTERN_CARPET_TPS = Pattern.compile("TPS: (?<tps>[0-9]+[\\.,][0-9]) MSPT: (?<mspt>[0-9]+[\\.,][0-9])");

    private static final DataStorage INSTANCE = new DataStorage();
    private final MobCapDataHandler mobCapData = new MobCapDataHandler();
    private final static ServuxStructuresHandler<ServuxStructuresPacket.Payload> HANDLER = ServuxStructuresHandler.getInstance();
    private boolean carpetServer = false;
    private boolean servuxServer = false;
    private boolean hasInValidServux = false;
    private boolean hasIntegratedServer = false;
    private int simulationDistance = -1;
    private int structureDataTimeout = 30 * 20;
    private boolean serverTPSValid;
    private boolean hasSyncedTime;
    private String servuxVersion;
    private int servuxTimeout;
    private boolean hasStructureDataFromServer;
    private boolean structureRendererNeedsUpdate;
    private boolean structuresNeedUpdating;
    private boolean shouldRegisterStructureChannel;
    private long lastServerTick;
    private long lastServerTimeUpdate;
    private BlockPos lastStructureUpdatePos;
    private double serverTPS;
    private double serverMSPT;
    private Vec3 distanceReferencePoint = Vec3.ZERO;
    private final int[] blockBreakCounter = new int[100];
    private final ArrayListMultimap<StructureType, StructureData> structures = ArrayListMultimap.create();
    private final Minecraft mc = Minecraft.getInstance();
    private IntegratedServer integratedServer;
    private RegistryAccess registryManager = RegistryAccess.EMPTY;

    private DataStorage() {}

    public static DataStorage getInstance()
    {
        return INSTANCE;
    }

    public void onGameInit()
    {
        ClientPlayHandler.getInstance().registerClientPlayHandler(HANDLER);
        HANDLER.registerPlayPayload(ServuxStructuresPacket.Payload.ID, ServuxStructuresPacket.Payload.CODEC, IPluginClientPlayHandler.BOTH_CLIENT);
    }

    public Identifier getNetworkChannel() { return ServuxStructuresHandler.CHANNEL_ID; }

    public IPluginClientPlayHandler<ServuxStructuresPacket.Payload> getNetworkHandler() { return HANDLER; }

    public MobCapDataHandler getMobCapData()
    {
        return this.mobCapData;
    }

    public void reset(boolean isLogout)
    {
        if (isLogout)
        {
            MiniHUD.debugLog("DataStorage#reset() - log-out");
            HANDLER.reset(this.getNetworkChannel());
            HANDLER.resetFailures(this.getNetworkChannel());

            this.servuxServer = false;
            this.hasInValidServux = false;
            this.structureDataTimeout = 30 * 20;
            this.registryManager = RegistryAccess.EMPTY;
            this.carpetServer = false;
            this.setHasIntegratedServer(false, null);
        }
        else
        {
            MiniHUD.debugLog("DataStorage#reset() - dimension change or log-in");
        }

        this.mobCapData.clear();
        this.serverTPSValid = false;
        this.hasSyncedTime = false;
        this.structuresNeedUpdating = true;
        this.hasStructureDataFromServer = false;
        this.structureRendererNeedsUpdate = true;

        this.lastStructureUpdatePos = null;
        this.structures.clear();
        this.servuxTimeout = -1;

        ShapeManager.INSTANCE.clear();
        OverlayRendererBeaconRange.INSTANCE.reset();
        OverlayRendererConduitRange.INSTANCE.reset();
        OverlayRendererBiomeBorders.INSTANCE.reset();
        OverlayRendererLightLevel.INSTANCE.reset();
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
        if (ver != null && ver.isEmpty() == false)
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

    public boolean hasIntegratedServer() { return this.hasIntegratedServer; }

    public void setHasIntegratedServer(boolean toggle, @Nullable IntegratedServer server)
    {
        this.hasIntegratedServer = toggle;
        this.integratedServer = server;
    }

    public IntegratedServer getIntegratedServer()
    {
        return this.integratedServer;
    }

    public boolean isSinglePlayer()
    {
        if (this.mc != null)
        {
            return this.mc.isLocalServer();
        }

        return false;
    }

    public void onWorldPre()
    {
        if (this.hasIntegratedServer == false)
        {
            HANDLER.registerPlayReceiver(ServuxStructuresPacket.Payload.ID, HANDLER::receivePlayPayload);
        }
    }

    public void onWorldJoin()
    {
        MiniHUD.debugLog("DataStorage#onWorldJoin()");
        OverlayRendererBeaconRange.INSTANCE.setNeedsUpdate();
        OverlayRendererConduitRange.INSTANCE.setNeedsUpdate();
        OverlayRendererSpawnChunks.INSTANCE_REAL.setNeedsUpdate();
        OverlayRendererSpawnChunks.INSTANCE_PLAYER.setNeedsUpdate();

        if (this.hasIntegratedServer == false)
        {
            // We don't always receive the initial metadata packet,
            // so we must send either a register or unregister packet to be sure.
            if (RendererToggle.OVERLAY_STRUCTURE_MAIN_TOGGLE.getBooleanValue())
            {
                this.registerStructureChannel();
                this.structuresNeedUpdating = true;
            }
            else
            {
                this.unregisterStructureChannel();
            }
        }
    }

    /**
     * Store's the world registry manager for Dynamic Lookup for various data
     * Set this at WorldLoadPost
     * @param manager ()
     */
    public void setWorldRegistryManager(RegistryAccess manager)
    {
        if (manager != null && manager != RegistryAccess.EMPTY)
        {
            this.registryManager = manager;
        }
        else
        {
            this.registryManager = RegistryAccess.EMPTY;
        }
    }

    public RegistryAccess getWorldRegistryManager()
    {
        if (this.registryManager != RegistryAccess.EMPTY)
        {
            return this.registryManager;
        }
        else
        {
            return RegistryAccess.EMPTY;
        }
    }

    public void setSimulationDistance(int distance)
    {
        if (distance >= 0)
        {
            if (this.simulationDistance != distance)
            {
                OverlayRendererSpawnChunks.INSTANCE_REAL.setNeedsUpdate();
                OverlayRendererSpawnChunks.INSTANCE_PLAYER.setNeedsUpdate();
            }
            this.simulationDistance = distance;
            //MiniHUD.printDebug("DataStorage#setSimulationDistance(): set to: [{}]", distance);
        }
        else
        {
            this.simulationDistance = -1;
        }
    }

    public boolean isSimulationDistanceKnown()
    {
        return this.simulationDistance >= 0;
    }

    public int getSimulationDistance()
    {
        if (this.simulationDistance > 0)
        {
            return this.simulationDistance;
        }

        return 10;
    }

    public boolean hasTPSData()
    {
        return this.serverTPSValid;
    }

    public boolean hasCarpetServer()
    {
        return this.carpetServer;
    }

    public boolean hasServuxServer() { return this.servuxServer; }

    public double getServerTPS()
    {
        return this.serverTPS;
    }

    public double getServerMSPT()
    {
        return this.serverMSPT;
    }

    public boolean structureRendererNeedsUpdate()
    {
        return this.structureRendererNeedsUpdate;
    }

    public void setStructuresNeedUpdating()
    {
        this.structuresNeedUpdating = true;
    }

    public void setStructureRendererNeedsUpdate()
    {
        this.structureRendererNeedsUpdate = true;
    }

    public Vec3 getDistanceReferencePoint()
    {
        return this.distanceReferencePoint;
    }

    public void setDistanceReferencePoint(Vec3 pos)
    {
        this.distanceReferencePoint = pos;
        String str = String.format("x: %.2f, y: %.2f, z: %.2f", pos.x, pos.y, pos.z);
        InfoUtils.printActionbarMessage("minihud.message.distance_reference_point_set", str);
    }

    public void markChunkForHeightmapCheck(int chunkX, int chunkZ)
    {
        Entity entity = Minecraft.getInstance().getCameraEntity();

        // Only update the renderers when blocks change near the camera
        if (entity != null)
        {
            Vec3 pos = entity.position();

            if (Math.abs(pos.x - (chunkX << 4) - 8) <= 48D || Math.abs(pos.z - (chunkZ << 4) - 8) <= 48D)
            {
                OverlayRendererSpawnableColumnHeights.INSTANCE.markChunkChanged(chunkX, chunkZ);
                OverlayRendererLightLevel.INSTANCE.setNeedsUpdate();
            }
        }
    }

    public void onClientTickPre(Minecraft mc)
    {
        if (mc.level != null && mc.level.getGameTime() > 0L)
        {
            int tick = (int) (mc.level.getGameTime() % this.blockBreakCounter.length);
            this.blockBreakCounter[tick] = 0;
        }
    }

    public void onPlayerBlockBreak(Minecraft mc)
    {
        if (mc.level != null && mc.level.getGameTime() > 0L)
        {
            int tick = (int) (mc.level.getGameTime() % this.blockBreakCounter.length);
            ++this.blockBreakCounter[tick];
        }
    }

    public double getBlockBreakingSpeed()
    {
        return MiscUtils.intAverage(this.blockBreakCounter) * 20;
    }

    public boolean onSendChatMessage(String message)
    {
        String[] parts = message.split(" ");

        if (parts.length > 0 &&
                (parts[0].equals("minihud-seed") ||
                 parts[0].equals("#minihud-seed") ||
                 parts[0].equals("/minihud-seed")))
        {
            if (parts.length == 2)
            {
                try
                {
                    HudDataManager.getInstance().setWorldSeed(Long.parseLong(parts[1]));
                    InfoUtils.printActionbarMessage("minihud.message.seed_set", HudDataManager.getInstance().worldSeed());
                }
                catch (NumberFormatException e)
                {
                    InfoUtils.printActionbarMessage("minihud.message.error.invalid_seed");
                }
            }
            else if (parts.length == 1)
            {
                if (HudDataManager.getInstance().hasStoredWorldSeed())
                {
                    InfoUtils.printActionbarMessage("minihud.message.seed_is", HudDataManager.getInstance().worldSeed());
                }
                else
                {
                    InfoUtils.printActionbarMessage("minihud.message.no_seed");
                }
            }

            return true;
        }
        else if (parts.length > 0 &&
                (parts[0].equals("minihud-spawnchunkradius") ||
                 parts[0].equals("/minihud-spawnchunkradius") ||
                 parts[0].equals("#minihud-spawnchunkradius"))
        )
        {
            if (parts.length == 2)
            {
                try
                {
                    int radius = Integer.parseInt(parts[1]);

                    if (radius >= -1 && radius <= 32)
                    {
                        HudDataManager.getInstance().setSpawnChunkRadius(radius, true);
                    }
                    else
                    {
	                    HudDataManager.getInstance().setSpawnChunkRadius(-1, false);
                        InfoUtils.printActionbarMessage("minihud.message.error.invalid_spawn_chunk_radius");
                    }
                }
                catch (NumberFormatException e)
                {
	                HudDataManager.getInstance().setSpawnChunkRadius(-1, false);
                    InfoUtils.printActionbarMessage("minihud.message.error.invalid_spawn_chunk_radius");
                }
            }
            else if (parts.length == 1)
            {
                if (HudDataManager.getInstance().isSpawnChunkRadiusKnown())
                {
                    int radius = HudDataManager.getInstance().getSpawnChunkRadius();
                    String strRadius = radius > 0 ? GuiBase.TXT_GREEN + String.format("%d", radius) + GuiBase.TXT_RST : GuiBase.TXT_RED + String.format("%d", radius) + GuiBase.TXT_RST;
                    InfoUtils.printActionbarMessage(StringUtils.translate("minihud.message.spawn_chunk_radius_is", strRadius));
                }
                else
                {
                    InfoUtils.printActionbarMessage("minihud.message.no_spawn_chunk_radius");
                }
            }

            return true;
        }

        return false;
    }

    public void onChatMessage(Component message)
    {
        if (message instanceof MutableComponent mutableText &&
            mutableText.getContents() instanceof TranslatableContents text)
        {
            // The vanilla "/seed" command
            if ("commands.seed.success".equals(text.getKey()) && text.getArgs().length == 1)
            {
                try
                {
                    //String str = message.getString();
                    //int i1 = str.indexOf("[");
                    //int i2 = str.indexOf("]");
                    MutableComponent m = (MutableComponent) text.getArgs()[0];
                    TranslatableContents t = (TranslatableContents) m.getContents();
                    PlainTextContents.LiteralContents l = (PlainTextContents.LiteralContents) ((MutableComponent) t.getArgs()[0]).getContents();
                    String str = l.text();

                    //if (i1 != -1 && i2 != -1)
                    {
                        //this.setWorldSeed(Long.parseLong(str.substring(i1 + 1, i2)));
                        HudDataManager.getInstance().setWorldSeed(Long.parseLong(str));
                        MiniHUD.LOGGER.info("Received world seed from the vanilla /seed command: {}", HudDataManager.getInstance().worldSeed());
                        InfoUtils.printActionbarMessage("minihud.message.seed_set", HudDataManager.getInstance().worldSeed());
                    }
                }
                catch (Exception e)
                {
                    MiniHUD.LOGGER.warn("Failed to read the world seed from '{}'", text.getArgs()[0]);
                }
            }
            // The "/jed seed" command
            else if ("jed.commands.seed.success".equals(text.getKey()))
            {
                try
                {
                    HudDataManager.getInstance().setWorldSeed(Long.parseLong(text.getArgs()[1].toString()));
                    MiniHUD.LOGGER.info("Received world seed from the JED '/jed seed' command: {}", HudDataManager.getInstance().worldSeed());
                    InfoUtils.printActionbarMessage("minihud.message.seed_set", HudDataManager.getInstance().worldSeed());
                }
                catch (Exception e)
                {
                    MiniHUD.LOGGER.warn("Failed to read the world seed from '{}'", text.getArgs()[1], e);
                }
            }
            else if ("commands.setworldspawn.success".equals(text.getKey()) && text.getArgs().length == 4)
            {
                try
                {
                    Object[] o = text.getArgs();
                    int x = Integer.parseInt(o[0].toString());
                    int y = Integer.parseInt(o[1].toString());
                    int z = Integer.parseInt(o[2].toString());
//                    float pitch = Float.parseFloat(o[3].toString());
//                    float yaw = Float.parseFloat(o[4].toString());
//                    String dim = o[5].toString();

                    ResourceKey<Level> key = this.mc.level != null ? this.mc.level.dimension() : Level.OVERWORLD;
                    GlobalPos newSpawn = new GlobalPos(key, new BlockPos(x, y, z));
                    HudDataManager.getInstance().setWorldSpawn(newSpawn);

                    String spawnStr = HudDataManager.getInstance().getWorldSpawnAsString(newSpawn);
                    MiniHUD.LOGGER.info("Received world spawn from the vanilla /setworldspawn command: {}", spawnStr);
                    InfoUtils.printActionbarMessage("minihud.message.spawn_set", spawnStr);
                }
                catch (Exception e)
                {
                    MiniHUD.LOGGER.warn("Failed to read the world spawn point from '{}'", text.getArgs(), e);
                }
            }
//            else if (("commands.gamerule.set".equals(text.getKey()) || "commands.gamerule.query".equals(text.getKey())) && text.getArgs().length == 2)
//            {
//                try
//                {
//                    Object[] o = text.getArgs();
//                    String rule = o[0].toString();
//
//                    if (rule.equals("spawnChunkRadius"))
//                    {
//                        int value = Integer.parseInt(o[1].toString());
//
//                        if (HudDataManager.getInstance().getSpawnChunkRadius() != value)
//                        {
//                            MiniHUD.LOGGER.info("Received spawn chunk radius from the vanilla /gamerule command: {}", HudDataManager.getInstance().getSpawnChunkRadius());
//                            HudDataManager.getInstance().setSpawnChunkRadius(value, true);
//                        }
//                        else
//                        {
//                            int radius = HudDataManager.getInstance().getSpawnChunkRadius();
//                            String strRadius = radius > 0 ? GuiBase.TXT_GREEN + String.format("%d", radius) + GuiBase.TXT_RST : GuiBase.TXT_RED + String.format("%d", radius) + GuiBase.TXT_RST;
//                            InfoUtils.printActionbarMessage(StringUtils.translate("minihud.message.spawn_chunk_radius_is", strRadius));
//                        }
//                    }
//                }
//                catch (Exception e)
//                {
//                    MiniHUD.LOGGER.warn("Failed to read the spawn chunk radius from '{}'", text.getArgs(), e);
//                }
//            }
        }
    }

    public void onServerTimeUpdate(long totalWorldTime)
    {
        // Carpet server sends the TPS and MSPT values via the player list footer data,
        // and for single player the data is grabbed directly from the integrated server.
        if (this.carpetServer == false && this.mc.isLocalServer() == false)
        {
            long currentTime = System.nanoTime();

            if (this.hasSyncedTime)
            {
                long elapsedTicks = totalWorldTime - this.lastServerTick;

                if (elapsedTicks > 0)
                {
                    this.serverMSPT = ((double) (currentTime - this.lastServerTimeUpdate) / (double) elapsedTicks) / 1000000D;
                    this.serverTPS = this.serverMSPT <= 50 ? 20D : (1000D / this.serverMSPT);
                    this.serverTPSValid = true;
                }
            }

            this.lastServerTick = totalWorldTime;
            this.lastServerTimeUpdate = currentTime;
            this.hasSyncedTime = true;
        }
    }

    public void updateIntegratedServerTPS()
    {
        if (this.mc != null && this.mc.player != null && this.mc.getSingleplayerServer() != null)
        {
            this.serverMSPT = MiscUtils.longAverage(this.mc.getSingleplayerServer().getTickTimesNanos()) / 1000000D;
            this.serverTPS = this.serverMSPT <= 50 ? 20D : (1000D / this.serverMSPT);
            this.serverTPSValid = true;
        }
    }

    /**
     * Gets a copy of the structure data map, and clears the dirty flag
     */
    public ArrayListMultimap<StructureType, StructureData> getCopyOfStructureData()
    {
        ArrayListMultimap<StructureType, StructureData> copy = ArrayListMultimap.create();

        if (RendererToggle.OVERLAY_STRUCTURE_MAIN_TOGGLE.getBooleanValue() == false)
        {
            return copy;
        }

        synchronized (this.structures)
        {
            for (StructureType type : StructureType.VALUES)
            {
                Collection<StructureData> values = this.structures.get(type);

                if (values.isEmpty() == false)
                {
                    copy.putAll(type, values);
                }
            }

            this.structureRendererNeedsUpdate = false;
        }

        return copy;
    }

    /**
     * Get all structures withinRange of the player (Helps reduce overhead)
     * @param pos (Player Position)
     * @param maxRange (maxChunkRange)
     * @return (The list)
     */
    public ArrayListMultimap<StructureType, StructureData> getCopyOfStructureDataWithinRange(BlockPos pos, int maxRange)
    {
        ArrayListMultimap<StructureType, StructureData> copy = ArrayListMultimap.create();

        if (RendererToggle.OVERLAY_STRUCTURE_MAIN_TOGGLE.getBooleanValue() == false)
        {
            return copy;
        }

        synchronized (this.structures)
        {
            for (StructureType type : StructureType.VALUES)
            {
                Collection<StructureData> values = this.structures.get(type);
                Collection<StructureData> valuesCopy = new ArrayList<>();

                if (values.isEmpty() == false)
                {
                    for (StructureData structure : values)
                    {
                        if (MiscUtils.isStructureWithinRange(structure.getBoundingBox(), pos, maxRange))
                        {
                            valuesCopy.add(structure);
                        }
                    }

                    copy.putAll(type, valuesCopy);
                }
            }

            this.structureRendererNeedsUpdate = false;
        }

        return copy;
    }

    public int getStructureDataMaxRange()
    {
        return this.mc.options.getEffectiveRenderDistance() + 2;
    }

    public int getServerRenderDistance()
    {
        final int range = ((IMixinOptions) this.mc.options).minihud_getServerRenderDistance();

        if (range > 0)
        {
            return range;
        }

        return this.mc.options.getEffectiveRenderDistance();
    }

    public void updateStructureData()
    {
        if (this.mc != null && this.mc.level != null && this.mc.player != null)
        {
            long currentTime = this.mc.level.getGameTime();

            if ((currentTime % 20) == 0)
            {
                if (this.mc.hasSingleplayerServer())
                {
                    if (RendererToggle.OVERLAY_STRUCTURE_MAIN_TOGGLE.getBooleanValue())
                    {
                        BlockPos playerPos = PositionUtils.getEntityBlockPos(this.mc.player);
                        final int maxRange = this.getStructureDataMaxRange();
                        final int hysteresis = 16;

                        if (this.structuresNeedUpdating(playerPos, hysteresis))
                        {
                            this.updateStructureDataFromIntegratedServer(playerPos, maxRange);
                        }
                    }
                }
                else if (this.hasStructureDataFromServer)
                {
                    this.removeExpiredStructures(currentTime, this.structureDataTimeout);
                }
                else if (this.shouldRegisterStructureChannel && this.mc.getConnection() != null)
                {
                    if (RendererToggle.OVERLAY_STRUCTURE_MAIN_TOGGLE.getBooleanValue())
                    {
                        this.registerStructureChannel();
                        this.structuresNeedUpdating = true;
                    }

                    this.shouldRegisterStructureChannel = false;
                }
            }
        }
    }

    public void registerStructureChannel()
    {
        this.shouldRegisterStructureChannel = true;

        if (this.servuxServer == false && this.hasIntegratedServer == false && this.hasInValidServux == false)
        {
            if (HANDLER.isPlayRegistered(this.getNetworkChannel()))
            {
                MiniHUD.debugLog("DataStorage#registerStructureChannel(): sending STRUCTURES_REGISTER to Servux");

                CompoundTag nbt = new CompoundTag();
                nbt.putString("version", Reference.MOD_STRING);

                HANDLER.encodeStructuresPacket(new ServuxStructuresPacket(ServuxStructuresPacket.Type.PACKET_C2S_STRUCTURES_REGISTER, nbt));
            }
        }
        else
        {
            this.shouldRegisterStructureChannel = false;
        }
        // QuickCarpet doesn't exist for 1.20.5+,
        // Will re-add if they update it
    }

    public boolean receiveServuxStrucutresMetadata(CompoundTag data)
    {
        if (this.servuxServer == false && this.hasIntegratedServer == false &&
            this.shouldRegisterStructureChannel)
        {
            MiniHUD.debugLog("DataStorage#receiveServuxStrucutresMetadata(): received METADATA from Servux");

            if (data.getIntOr("version", -1) != ServuxStructuresPacket.PROTOCOL_VERSION)
            {
                MiniHUD.LOGGER.warn("structureChannel: Mis-matched protocol version!");
            }
            this.servuxTimeout = data.getIntOr("timeout", 300);
            this.setServuxVersion(data.getStringOr("servux", "?"));
            // Backwards compat only
            if (data.contains("spawnPosX"))
            {
                HudDataManager.getInstance().setWorldSpawn(
                        new GlobalPos(
                                Level.OVERWORLD,
                                new BlockPos(data.getIntOr("spawnPosX", 0), data.getIntOr("spawnPosY", 0), data.getIntOr("spawnPosZ", 0)))
                );
            }
//            if (data.contains("spawnChunkRadius"))
//            {
//                HudDataManager.getInstance().setSpawnChunkRadius(data.getInt("spawnChunkRadius", 2), true);
//            }
            if (data.contains("worldSeed"))
            {
                HudDataManager.getInstance().setWorldSeed(data.getLongOr("worldSeed", -1L));
            }
            this.setIsServuxServer();

            if (RendererToggle.OVERLAY_STRUCTURE_MAIN_TOGGLE.getBooleanValue())
            {
                this.registerStructureChannel();
                return true;
            }
            else
            {
                this.unregisterStructureChannel();
            }
        }

        return false;
    }

    public void unregisterStructureChannel()
    {
        if (this.servuxServer || RendererToggle.OVERLAY_STRUCTURE_MAIN_TOGGLE.getBooleanValue() == false)
        {
            this.servuxServer = false;
            if (this.hasInValidServux == false)
            {
                MiniHUD.debugLog("DataStorage#unregisterStructureChannel(): for {}", this.servuxVersion != null ? this.servuxVersion : "<unknown>");

                HANDLER.encodeStructuresPacket(new ServuxStructuresPacket(ServuxStructuresPacket.Type.PACKET_C2S_STRUCTURES_UNREGISTER, new CompoundTag()));
                HANDLER.reset(HANDLER.getPayloadChannel());
            }
        }
        this.shouldRegisterStructureChannel = false;
    }

    public void onPacketFailure()
    {
        // Define how to handle multiple sendPayload failures
        this.shouldRegisterStructureChannel = false;
        this.servuxServer = false;
        this.hasInValidServux = true;
    }

    private boolean structuresNeedUpdating(BlockPos playerPos, final int hysteresis)
    {
        return this.structuresNeedUpdating || this.lastStructureUpdatePos == null ||
                Math.abs(playerPos.getX() - this.lastStructureUpdatePos.getX()) >= hysteresis ||
                Math.abs(playerPos.getY() - this.lastStructureUpdatePos.getY()) >= hysteresis ||
                Math.abs(playerPos.getZ() - this.lastStructureUpdatePos.getZ()) >= hysteresis;
    }

    public int getStrucutreCount()
    {
        return this.structures.size();
    }

    private void updateStructureDataFromIntegratedServer(final BlockPos playerPos, final int maxRange)
    {
        if (this.mc.player == null || this.mc.getSingleplayerServer() == null) return;

        final ResourceKey<Level> worldId = this.mc.player.level().dimension();
        final ServerLevel world = this.mc.getSingleplayerServer().getLevel(worldId);

        if (world != null)
        {
            MinecraftServer server = this.mc.getSingleplayerServer();

            ((IMixinMinecraftServer) server).minihud_send(new TickTask(server.getTickCount(), () ->
            {
                synchronized (this.structures)
                {
                    this.addStructureDataFromGenerator(world, playerPos, maxRange);
                }
            }));
        }
        else
        {
            synchronized (this.structures)
            {
                this.structures.clear();
            }
        }

        this.lastStructureUpdatePos = playerPos;
        this.structuresNeedUpdating = false;
    }

    public void addOrUpdateStructuresFromServer(ListTag structures, boolean isServux)
    {
        if (isServux == false)
        {
            MiniHUD.debugLog("DataStorage#addOrUpdateStructuresFromServer(): Ignoring structure data when isServux is false");
            //this.unregisterStructureChannel();
            return;
        }

        if (!structures.isEmpty())
        {
            this.structureDataTimeout = this.servuxTimeout + 300;

            long currentTime = this.mc.level.getGameTime();
            final int count = structures.size();
            final int oldCount = this.structures.size();

            this.removeExpiredStructures(currentTime, this.structureDataTimeout);

            for (int i = 0; i < count; ++i)
            {
                CompoundTag tag = structures.getCompoundOrEmpty(i);
                StructureData data = StructureData.fromStructureStartTag(tag, currentTime);

                if (data != null)
                {
                    // Remove the old entry and replace it with the new entry with the current refresh time
                    if (this.structures.containsEntry(data.getStructureType(), data))
                    {
                        this.structures.remove(data.getStructureType(), data);
                    }

                    this.structures.put(data.getStructureType(), data);
                }
            }

            MiniHUD.debugLog("addOrUpdateStructuresFromServer: received {} structures // total size {} -> {}", count, oldCount, this.structures.size());

            this.structureRendererNeedsUpdate = true;
            this.hasStructureDataFromServer = true;
        }
    }

    private void removeExpiredStructures(long currentTime, int timeout)
    {
        int countBefore = this.structures.values().size();

        this.structures.values().removeIf(data -> currentTime > (data.getRefreshTime() + (long) timeout));

        int countAfter = this.structures.values().size();

        if (countBefore != countAfter)
        {
            MiniHUD.debugLog("removeExpiredStructures: from server: {} -> {} structures", countBefore, countAfter);
        }
    }

    private void addStructureDataFromGenerator(ServerLevel world, BlockPos playerPos, int maxChunkRange)
    {
        int lastCount = this.structures.size();

        this.structures.clear();

        int minCX = (playerPos.getX() >> 4) - maxChunkRange;
        int minCZ = (playerPos.getZ() >> 4) - maxChunkRange;
        int maxCX = (playerPos.getX() >> 4) + maxChunkRange;
        int maxCZ = (playerPos.getZ() >> 4) + maxChunkRange;

        for (int cz = minCZ; cz <= maxCZ; ++cz)
        {
            for (int cx = minCX; cx <= maxCX; ++cx)
            {
                // Don't load the chunk
                ChunkAccess chunk;
                try
                {
                     chunk = world.getChunk(cx, cz, ChunkStatus.STRUCTURE_REFERENCES, false);
                }
                catch (Exception ignored)
                {
                    continue;
                }

                if (chunk == null)
                {
                    continue;
                }

                for (Map.Entry<Structure, StructureStart> entry : chunk.getAllStarts().entrySet())
                {
                    Structure structure = entry.getKey();
                    StructureStart start = entry.getValue();
                    Identifier id = world.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(structure);
                    StructureType type = StructureType.fromStructureId(id != null ? id.toString() : "?");

                    if (type.isEnabled() &&
                        start.isValid() &&
                        MiscUtils.isStructureWithinRange(start.getBoundingBox(), playerPos, maxChunkRange << 4))
                    {
                        this.structures.put(type, StructureData.fromStructureStart(type, start));
                    }
                }
            }
        }

        MiniHUD.debugLog("addStructureDataFromGenerator: updated from the integrated server: {} -> {} structures", lastCount, this.structures.size());
        this.structureRendererNeedsUpdate = true;
    }

    public void handleCarpetServerTPSData(Component textComponent)
    {
        if (textComponent.getString().isEmpty() == false)
        {
            String text = ChatFormatting.stripFormatting(textComponent.getString());
            String[] lines = text.split("\n");

            for (String line : lines)
            {
                Matcher matcher = PATTERN_CARPET_TPS.matcher(line);

                if (matcher.matches())
                {
                    if (!TickUtils.getInstance().isUsingDirectServerData())
                    {
                        TickUtils.getInstance().toggleUseDirectServerData(true);
                    }

                    try
                    {
                        this.serverTPS = Double.parseDouble(matcher.group("tps"));
                        this.serverMSPT = Double.parseDouble(matcher.group("mspt"));
                        this.serverTPSValid = true;
                        TickUtils.getInstance().updateNanoTickFromServerDirect(this.serverTPS, this.serverMSPT);
                        this.carpetServer = true;
                        return;
                    }
                    catch (NumberFormatException ignore) {}
                }
            }
        }
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        if (!this.distanceReferencePoint.equals(Vec3.ZERO))
        {
            obj.add("distance_pos", JsonUtils.vec3dToJson(this.distanceReferencePoint));
        }

        return obj;
    }

    public void fromJson(JsonObject obj)
    {
        Vec3 pos = JsonUtils.vec3dFromJson(obj, "distance_pos");
        this.distanceReferencePoint = Objects.requireNonNullElse(pos, Vec3.ZERO);

        // Backwards compat
        if (JsonUtils.hasLong(obj, "seed"))
        {
            HudDataManager.getInstance().setWorldSeed(JsonUtils.getLong(obj, "seed"));
        }

        if (JsonUtils.hasInteger(obj, "spawn_chunk_radius"))
        {
            HudDataManager.getInstance().setSpawnChunkRadius(JsonUtils.getIntegerOrDefault(obj, "spawn_chunk_radius", -1), false);
        }
    }
}
