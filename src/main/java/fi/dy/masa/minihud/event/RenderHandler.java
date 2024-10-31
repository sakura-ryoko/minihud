package fi.dy.masa.minihud.event;

import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.*;
import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.data.EntitiesDataManager;
import fi.dy.masa.minihud.data.HudDataManager;
import fi.dy.masa.minihud.data.MobCapDataHandler;
import fi.dy.masa.minihud.mixin.*;
import fi.dy.masa.minihud.renderer.OverlayRenderer;
import fi.dy.masa.minihud.util.*;

import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.Frustum;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.OptionalChunk;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.LightType;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;

import com.llamalad7.mixinextras.lib.apache.commons.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import com.mojang.blaze3d.systems.RenderSystem;

public class RenderHandler implements IRenderer
{
    private static final RenderHandler INSTANCE = new RenderHandler();

    private final MinecraftClient mc;
    private final DataStorage data;
    private final HudDataManager hudData;
    private final Date date;
    private final Map<ChunkPos, CompletableFuture<OptionalChunk<Chunk>>> chunkFutures = new HashMap<>();
    private final Set<InfoToggle> addedTypes = new HashSet<>();
    @Nullable private WorldChunk cachedClientChunk;
    private long infoUpdateTime;

    private final List<StringHolder> lineWrappers = new ArrayList<>();
    private final List<String> lines = new ArrayList<>();
    private Pair<BlockEntity, NbtCompound> lastBlockEntity = null;
    private Pair<Entity,      NbtCompound> lastEntity = null;

    public RenderHandler()
    {
        this.mc = MinecraftClient.getInstance();
        this.data = DataStorage.getInstance();
        this.hudData = HudDataManager.getInstance();
        this.date = new Date();
    }

    public static RenderHandler getInstance()
    {
        return INSTANCE;
    }

    public DataStorage getDataStorage()
    {
        return this.data;
    }

    public HudDataManager getHudData()
    {
        return this.hudData;
    }

    public static void fixDebugRendererState()
    {
        /*
        if (Configs.Generic.FIX_VANILLA_DEBUG_RENDERERS.getBooleanValue())
        {
            RenderSystem.disableLighting();
            RenderUtils.color(1, 1, 1, 1);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        }
         */
    }

    @Override
    public void onRenderGameOverlayPostAdvanced(DrawContext drawContext, float partialTicks, Profiler profiler, MinecraftClient mc)
    {
        if (Configs.Generic.MAIN_RENDERING_TOGGLE.getBooleanValue() == false)
        {
            this.resetCachedChunks();
            return;
        }

        if (mc.getDebugHud().shouldShowDebugHud() == false &&
            mc.player != null && mc.options.hudHidden == false &&
            (Configs.Generic.REQUIRE_SNEAK.getBooleanValue() == false || mc.player.isSneaking()) &&
            Configs.Generic.REQUIRED_KEY.getKeybind().isKeybindHeld())
        {

            long currentTime = System.nanoTime();

            // Only update the text once per game tick
            if (currentTime - this.infoUpdateTime >= 50000000L)
            {
                this.updateLines();
                this.infoUpdateTime = currentTime;
            }

            int x = Configs.Generic.TEXT_POS_X.getIntegerValue();
            int y = Configs.Generic.TEXT_POS_Y.getIntegerValue();
            int textColor = Configs.Colors.TEXT_COLOR.getIntegerValue();
            int bgColor = Configs.Colors.TEXT_BACKGROUND_COLOR.getIntegerValue();
            HudAlignment alignment = (HudAlignment) Configs.Generic.HUD_ALIGNMENT.getOptionListValue();
            boolean useBackground = Configs.Generic.USE_TEXT_BACKGROUND.getBooleanValue();
            boolean useShadow = Configs.Generic.USE_FONT_SHADOW.getBooleanValue();

            RenderUtils.renderText(x, y, Configs.Generic.FONT_SCALE.getDoubleValue(), textColor, bgColor, alignment, useBackground, useShadow, this.lines, drawContext);
            RenderUtils.forceDraw(drawContext);
        }

        if (Configs.Generic.INVENTORY_PREVIEW_ENABLED.getBooleanValue() &&
            Configs.Generic.INVENTORY_PREVIEW.getKeybind().isKeybindHeld())
        {
            var inventory = RayTraceUtils.getTargetInventory(mc, true);

            if (inventory != null)
            {
                fi.dy.masa.minihud.renderer.RenderUtils.renderInventoryOverlay(inventory, drawContext);
            }

            // OG method (Works with Crafters also)
            //fi.dy.masa.minihud.renderer.RenderUtils.renderInventoryOverlay(mc, drawContext);
        }
    }

    @Override
    public void onRenderTooltipLast(DrawContext drawContext, ItemStack stack, int x, int y)
    {
        Item item = stack.getItem();
        if (item instanceof FilledMapItem)
        {
            if (Configs.Generic.MAP_PREVIEW.getBooleanValue() &&
               (Configs.Generic.MAP_PREVIEW_REQUIRE_SHIFT.getBooleanValue() == false || GuiBase.isShiftDown()))
            {
                RenderUtils.renderMapPreview(stack, x, y, Configs.Generic.MAP_PREVIEW_SIZE.getIntegerValue(), false, drawContext);
            }
        }
        else if (stack.getComponents().contains(DataComponentTypes.CONTAINER) && InventoryUtils.shulkerBoxHasItems(stack))
        {
            if (Configs.Generic.SHULKER_BOX_PREVIEW.getBooleanValue() &&
               (Configs.Generic.SHULKER_DISPLAY_REQUIRE_SHIFT.getBooleanValue() == false || GuiBase.isShiftDown()))
            {
                RenderUtils.renderShulkerBoxPreview(stack, x, y, Configs.Generic.SHULKER_DISPLAY_BACKGROUND_COLOR.getBooleanValue(), drawContext);
            }
        }
    }

    @Override
    public void onRenderWorldPreWeather(Matrix4f posMatrix, Matrix4f projMatrix, Frustum frustum, Camera camera, Fog fog, Profiler profiler)
    {
        if (Configs.Generic.MAIN_RENDERING_TOGGLE.getBooleanValue() &&
            this.mc.world != null && this.mc.player != null && this.mc.options.hudHidden == false)
        {
            OverlayRenderer.renderOverlays(posMatrix, projMatrix, this.mc, frustum, camera, fog, profiler);
        }
    }

    public int getSubtitleOffset()
    {
        if (Configs.Generic.OFFSET_SUBTITLE_HUD.getBooleanValue() &&
            Configs.Generic.MAIN_RENDERING_TOGGLE.getBooleanValue() &&
            Configs.Generic.HUD_ALIGNMENT.getOptionListValue() == HudAlignment.BOTTOM_RIGHT)
        {
            int offset = (int) (this.lineWrappers.size() * (StringUtils.getFontHeight() + 2) * Configs.Generic.FONT_SCALE.getDoubleValue());

            return -(offset - 16);
        }

        return 0;
    }

    public void updateData(MinecraftClient mc)
    {
        if (mc.world != null)
        {
            if (RendererToggle.OVERLAY_STRUCTURE_MAIN_TOGGLE.getBooleanValue())
            {
                DataStorage.getInstance().updateStructureData();
            }
        }
    }

    private void updateLines()
    {
        this.lineWrappers.clear();
        this.addedTypes.clear();

        if (this.chunkFutures.size() >= 4)
        {
            this.resetCachedChunks();
        }

        // Get the info line order based on the configs
        List<LinePos> positions = new ArrayList<>();

        for (InfoToggle toggle : InfoToggle.values())
        {
            if (toggle.getBooleanValue())
            {
                positions.add(new LinePos(toggle.getIntegerValue(), toggle));
            }
        }

        Collections.sort(positions);

        for (LinePos pos : positions)
        {
            try
            {
                this.addLine(pos.type);
            }
            catch (Exception e)
            {
                this.addLine(pos.type.getName() + ": exception");
            }
        }

        if (Configs.Generic.SORT_LINES_BY_LENGTH.getBooleanValue())
        {
            Collections.sort(this.lineWrappers);

            if (Configs.Generic.SORT_LINES_REVERSED.getBooleanValue())
            {
                Collections.reverse(this.lineWrappers);
            }
        }

        this.lines.clear();

        for (StringHolder holder : this.lineWrappers)
        {
            this.lines.add(holder.str);
        }
    }

    public void addLine(String text)
    {
        this.lineWrappers.add(new StringHolder(text));
    }

    public void addLineI18n(String translatedName, Object... args)
    {
        this.addLine(StringUtils.translate(translatedName, args));
    }

    private void addLine(InfoToggle type)
    {
        MinecraftClient mc = this.mc;
        Entity entity = mc.getCameraEntity();
        World world = entity.getEntityWorld();
        double y = entity.getY();
        BlockPos pos = BlockPos.ofFloored(entity.getX(), y, entity.getZ());
        ChunkPos chunkPos = new ChunkPos(pos);

        @SuppressWarnings("deprecation")
        boolean isChunkLoaded = mc.world.isChunkLoaded(pos);

        if (isChunkLoaded == false)
        {
            return;
        }

        if (type == InfoToggle.FPS)
        {
            this.addLineI18n("minihud.info_line.fps", mc.getCurrentFps());
        }
        else if (type == InfoToggle.MEMORY_USAGE)
        {
            long memMax = Runtime.getRuntime().maxMemory();
            long memTotal = Runtime.getRuntime().totalMemory();
            long memFree = Runtime.getRuntime().freeMemory();
            long memUsed = memTotal - memFree;

            this.addLineI18n("minihud.info_line.memory_usage",
                             memUsed * 100L / memMax,
                             MiscUtils.bytesToMb(memUsed),
                             MiscUtils.bytesToMb(memMax),
                             memTotal * 100L / memMax,
                             MiscUtils.bytesToMb(memTotal));
        }
        else if (type == InfoToggle.TIME_REAL)
        {
            try
            {
                SimpleDateFormat sdf = new SimpleDateFormat(Configs.Generic.DATE_FORMAT_REAL.getStringValue());
                this.date.setTime(System.currentTimeMillis());
                this.addLine(sdf.format(this.date));
            }
            catch (Exception e)
            {
                this.addLineI18n("minihud.info_line.time.exception");
            }
        }
        else if (type == InfoToggle.TIME_WORLD)
        {
            long current = world.getTimeOfDay();
            long total = world.getTime();
            this.addLineI18n("minihud.info_line.time_world", current, total);
        }
        else if (type == InfoToggle.TIME_WORLD_FORMATTED)
        {
            try
            {
                long timeDay = world.getTimeOfDay();
                long day = (int) (timeDay / 24000);
                // 1 tick = 3.6 seconds in MC (0.2777... seconds IRL)
                int dayTicks = (int) (timeDay % 24000);
                int hour = (int) ((dayTicks / 1000) + 6) % 24;
                int min = (int) (dayTicks / 16.666666) % 60;
                int sec = (int) (dayTicks / 0.277777) % 60;
                // Moonphase has 8 different states in MC
                int moonNumber = (int) day % 8;
                String moon;
                if (moonNumber > 7)
                {
                    moon = StringUtils.translate("minihud.info_line.invalid_value");
                }
                else
                {
                    moon = StringUtils.translate("minihud.info_line.time_world_formatted.moon_" + moonNumber);
                }

                String str = Configs.Generic.DATE_FORMAT_MINECRAFT.getStringValue();
                str = str.replace("{DAY}",  String.format("%d", day));
                str = str.replace("{DAY_1}",String.format("%d", day + 1));
                str = str.replace("{HOUR}", String.format("%02d", hour));
                str = str.replace("{MIN}",  String.format("%02d", min));
                str = str.replace("{SEC}",  String.format("%02d", sec));
                str = str.replace("{MOON}",  String.format("%s", moon));

                this.addLine(str);
            }
            catch (Exception e)
            {
                this.addLineI18n("minihud.info_line.time.exception");
            }
        }
        else if (type == InfoToggle.TIME_DAY_MODULO)
        {
            int mod = Configs.Generic.TIME_DAY_DIVISOR.getIntegerValue();
            long current = world.getTimeOfDay() % mod;
            this.addLineI18n("minihud.info_line.time_day_modulo", mod, current);
        }
        else if (type == InfoToggle.TIME_TOTAL_MODULO)
        {
            int mod = Configs.Generic.TIME_TOTAL_DIVISOR.getIntegerValue();
            long current = world.getTime() % mod;
            this.addLineI18n("minihud.info_line.time_total_modulo", mod, current);
        }
        else if (type == InfoToggle.SERVER_TPS)
        {
            if (this.data.hasIntegratedServer() && (this.data.getIntegratedServer().getTicks() % 10) == 0)
            {
                this.data.updateIntegratedServerTPS();
            }

            if (this.data.hasTPSData())
            {
                double tps = this.data.getServerTPS();
                double mspt = this.data.getServerMSPT();
                String rst = GuiBase.TXT_RST;
                String preTps = tps >= 20.0D ? GuiBase.TXT_GREEN : GuiBase.TXT_RED;
                String preMspt;

                // Carpet server and integrated server have actual meaningful MSPT data available
                if (this.data.hasCarpetServer() || this.data.isSinglePlayer())
                {
                    if      (mspt <= 40) { preMspt = GuiBase.TXT_GREEN; }
                    else if (mspt <= 45) { preMspt = GuiBase.TXT_YELLOW; }
                    else if (mspt <= 50) { preMspt = GuiBase.TXT_GOLD; }
                    else                 { preMspt = GuiBase.TXT_RED; }

                    this.addLineI18n("minihud.info_line.server_tps", preTps, tps, rst, preMspt, mspt, rst);
                }
                else
                {
                    if (mspt <= 51) { preMspt = GuiBase.TXT_GREEN; }
                    else            { preMspt = GuiBase.TXT_RED; }

                    this.addLineI18n("minihud.info_line.server_tps.est", preTps, tps, rst, preMspt, mspt, rst);
                }
            }
            else
            {
                this.addLineI18n("minihud.info_line.server_tps.invalid");
            }
        }
        else if (type == InfoToggle.SERVUX)
        {
            if (EntitiesDataManager.getInstance().hasServuxServer())
            {
                this.addLineI18n("minihud.info_line.servux", EntitiesDataManager.getInstance().getServuxVersion());
            }
            else if (this.getDataStorage().hasServuxServer())
            {
                this.addLineI18n("minihud.info_line.servux", this.getDataStorage().getServuxVersion());
            }
            else if (this.getDataStorage().hasIntegratedServer() == false)
            {
                this.addLineI18n("minihud.info_line.servux.not_connected");
            }
            if (EntitiesDataManager.getInstance().hasServuxServer())
            {
                this.addLineI18n("minihud.info_line.servux.entity_sync",
                                 EntitiesDataManager.getInstance().getBlockEntityCacheCount(),
                                 EntitiesDataManager.getInstance().getPendingBlockEntitiesCount(),
                                 EntitiesDataManager.getInstance().getEntityCacheCount(),
                                 EntitiesDataManager.getInstance().getPendingEntitiesCount()
                );
            }
            if (this.getDataStorage().hasServuxServer())
            {
                this.addLineI18n("minihud.info_line.servux.structures",
                                 this.getDataStorage().getStrucutreCount(),
                                 this.getHudData().getSpawnChunkRadius(),
                                 this.getHudData().getWorldSpawn().toShortString(),
                                 this.getHudData().isWorldSpawnKnown() ? StringUtils.translate("minihud.info_line.slime_chunk.yes") : StringUtils.translate("minihud.info_line.slime_chunk.no")
                );
            }
            else if (this.getDataStorage().hasIntegratedServer())
            {
                this.addLineI18n("minihud.info_line.servux.structures_integrated",
                                 this.getDataStorage().getStrucutreCount(),
                                 this.getHudData().getSpawnChunkRadius(),
                                 this.getHudData().getWorldSpawn().toShortString(),
                                 this.getHudData().isWorldSpawnKnown() ? StringUtils.translate("minihud.info_line.slime_chunk.yes") : StringUtils.translate("minihud.info_line.slime_chunk.no")
                );
            }
        }
        else if (type == InfoToggle.WEATHER)
        {
            World bestWorld = WorldUtils.getBestWorld(mc);
            String weatherType = "clear";
            int weatherTime = -1;

            if (bestWorld == null)
            {
                return;
            }
            if (this.getHudData().isWeatherClear())
            {
                weatherType = "clear";
                weatherTime = this.getHudData().getClearTime();
            }
            else if (this.getHudData().isWeatherRain())
            {
                weatherType = "raining";
                weatherTime = this.getHudData().getRainTime();
            }
            else if (this.getHudData().isWeatherThunder())
            {
                weatherType = "thundering";
                weatherTime = this.getHudData().getThunderTime();
            }
            /*
            if (bestWorld.getLevelProperties().isThundering())
            {
                weatherType = "thundering";
                if (bestWorld.getLevelProperties() instanceof LevelProperties lp)
                {
                    weatherTime = lp.getThunderTime();
                }
            }
            else if (bestWorld.getLevelProperties().isRaining())
            {
                weatherType = "raining";
                if (bestWorld.getLevelProperties() instanceof LevelProperties lp)
                {
                    weatherTime = lp.getRainTime();
                }
            }
             */

            if (weatherTime == -1)
            {
                this.addLineI18n("minihud.info_line.weather", StringUtils.translate("minihud.info_line.weather." + weatherType), "");
            }
            else
            {
                // 50 = 1000 (ms/s) / 20 (ticks/s)
                this.addLineI18n("minihud.info_line.weather",
                                 StringUtils.translate("minihud.info_line.weather." + weatherType),
                                 ", " + StringUtils.getDurationString(weatherTime * 50L)
                                 + " " + StringUtils.translate("minihud.info_line.remaining")
                );
            }
        }
        else if (type == InfoToggle.MOB_CAPS)
        {
            MobCapDataHandler mobCapData = this.data.getMobCapData();

            if (mc.isIntegratedServerRunning() && (mc.getServer().getTicks() % 100) == 0)
            {
                mobCapData.updateIntegratedServerMobCaps();
            }

            if (mobCapData.getHasValidData())
            {
                this.addLine(mobCapData.getFormattedInfoLine());
            }
        }
        else if (type == InfoToggle.PING)
        {
            PlayerListEntry info = mc.player.networkHandler.getPlayerListEntry(mc.player.getUuid());

            if (info != null)
            {
                this.addLineI18n("minihud.info_line.ping", info.getLatency());
            }
        }
        else if (type == InfoToggle.COORDINATES ||
                 type == InfoToggle.COORDINATES_SCALED ||
                 type == InfoToggle.DIMENSION)
        {
            // Don't add the same line multiple times
            if (this.addedTypes.contains(InfoToggle.COORDINATES) ||
                this.addedTypes.contains(InfoToggle.COORDINATES_SCALED) ||
                this.addedTypes.contains(InfoToggle.DIMENSION))
            {
                return;
            }

            String pre = "";
            StringBuilder str = new StringBuilder(128);
            String fmtStr = Configs.Generic.COORDINATE_FORMAT_STRING.getStringValue();
            double x = entity.getX();
            double z = entity.getZ();

            if (InfoToggle.COORDINATES.getBooleanValue())
            {
                if (Configs.Generic.USE_CUSTOMIZED_COORDINATES.getBooleanValue())
                {
                    try
                    {
                        str.append(String.format(fmtStr, x, y, z));
                    }
                    // Uh oh, someone done goofed their format string... :P
                    catch (Exception e)
                    {
                        str.append(StringUtils.translate("minihud.info_line.coordinates.exception"));
                    }
                }
                else
                {
                    str.append(StringUtils.translate("minihud.info_line.coordinates.format", x, y, z));
                }

                pre = " / ";
            }

            if (InfoToggle.COORDINATES_SCALED.getBooleanValue() &&
                (world.getRegistryKey() == World.NETHER || world.getRegistryKey() == World.OVERWORLD))
            {
                boolean isNether = world.getRegistryKey() == World.NETHER;
                double scale = isNether ? 8.0 : 1.0 / 8.0;
                x *= scale;
                z *= scale;

                str.append(pre);

                if (isNether)
                {
                    str.append(StringUtils.translate("minihud.info_line.coordinates_scaled.overworld"));
                }
                else
                {
                    str.append(StringUtils.translate("minihud.info_line.coordinates_scaled.nether"));
                }

                if (Configs.Generic.USE_CUSTOMIZED_COORDINATES.getBooleanValue())
                {
                    try
                    {
                        str.append(String.format(fmtStr, x, y, z));
                    }
                    // Uh oh, someone done goofed their format string... :P
                    catch (Exception e)
                    {
                        str.append(StringUtils.translate("minihud.info_line.coordinates.exception"));
                    }
                }
                else
                {
                    str.append(StringUtils.translate("minihud.info_line.coordinates.format", x, y, z));
                }

                pre = " / ";
            }

            if (InfoToggle.DIMENSION.getBooleanValue())
            {
                String dimName = world.getRegistryKey().getValue().toString();
                str.append(pre).append(StringUtils.translate("minihud.info_line.dimension")).append(dimName);
            }

            this.addLine(str.toString());

            this.addedTypes.add(InfoToggle.COORDINATES);
            this.addedTypes.add(InfoToggle.COORDINATES_SCALED);
            this.addedTypes.add(InfoToggle.DIMENSION);
        }
        else if (type == InfoToggle.BLOCK_POS ||
                 type == InfoToggle.CHUNK_POS ||
                 type == InfoToggle.REGION_FILE)
        {
            // Don't add the same line multiple times
            if (this.addedTypes.contains(InfoToggle.BLOCK_POS) ||
                this.addedTypes.contains(InfoToggle.CHUNK_POS) ||
                this.addedTypes.contains(InfoToggle.REGION_FILE))
            {
                return;
            }

            String pre = "";
            StringBuilder str = new StringBuilder(256);

            if (InfoToggle.BLOCK_POS.getBooleanValue())
            {
                try
                {
                    String fmt = Configs.Generic.BLOCK_POS_FORMAT_STRING.getStringValue();
                    str.append(String.format(fmt, pos.getX(), pos.getY(), pos.getZ()));
                }
                // Uh oh, someone done goofed their format string... :P
                catch (Exception e)
                {
                    str.append(StringUtils.translate("minihud.info_line.block_pos.exception"));
                }

                pre = " / ";
            }

            if (InfoToggle.CHUNK_POS.getBooleanValue())
            {
                str.append(pre).append(StringUtils.translate("minihud.info_line.chunk_pos", chunkPos.x, pos.getY() >> 4, chunkPos.z));
                pre = " / ";
            }

            if (InfoToggle.REGION_FILE.getBooleanValue())
            {
                str.append(pre).append(StringUtils.translate("minihud.info_line.region_file", pos.getX() >> 9, pos.getZ() >> 9));
            }

            this.addLine(str.toString());

            this.addedTypes.add(InfoToggle.BLOCK_POS);
            this.addedTypes.add(InfoToggle.CHUNK_POS);
            this.addedTypes.add(InfoToggle.REGION_FILE);
        }
        else if (type == InfoToggle.BLOCK_IN_CHUNK)
        {
            this.addLineI18n("minihud.info_line.block_in_chunk",
                        pos.getX() & 0xF, pos.getY() & 0xF, pos.getZ() & 0xF,
                        chunkPos.x, pos.getY() >> 4, chunkPos.z);
        }
        else if (type == InfoToggle.BLOCK_BREAK_SPEED)
        {
            this.addLineI18n("minihud.info_line.block_break_speed", DataStorage.getInstance().getBlockBreakingSpeed());
        }
        else if (type == InfoToggle.SPRINTING && mc.player.isSprinting())
        {
            this.addLineI18n("minihud.info_line.sprinting");
        }
        else if (type == InfoToggle.DISTANCE)
        {
            Vec3d ref = DataStorage.getInstance().getDistanceReferencePoint();
            double dist = Math.sqrt(ref.squaredDistanceTo(entity.getX(), entity.getY(), entity.getZ()));
            this.addLineI18n("minihud.info_line.distance",
                    dist, entity.getX() - ref.x, entity.getY() - ref.y, entity.getZ() - ref.z, ref.x, ref.y, ref.z);
        }
        else if (type == InfoToggle.FACING)
        {
            Direction facing = entity.getHorizontalFacing();
            String facingName = StringUtils.translate("minihud.info_line.facing." + facing.getName() + ".name");
            String str;

            if (facingName.contains("minihud.info_line.facing." + facing.getName() + ".name"))
            {
                facingName = facing.name();
                str = StringUtils.translate("minihud.info_line.invalid_value");
            }
            else
            {
                str = StringUtils.translate("minihud.info_line.facing." + facing.getName());
            }

            this.addLineI18n("minihud.info_line.facing", facingName, str);
        }
        else if (type == InfoToggle.LIGHT_LEVEL)
        {
            WorldChunk clientChunk = this.getClientChunk(chunkPos);

            if (clientChunk.isEmpty() == false)
            {
                LightingProvider lightingProvider = world.getChunkManager().getLightingProvider();

                this.addLineI18n("minihud.info_line.light_level", lightingProvider.get(LightType.BLOCK).getLightLevel(pos));
            }
        }
        else if (type == InfoToggle.BEE_COUNT)
        {
            World bestWorld = WorldUtils.getBestWorld(mc);
            Pair<BlockEntity, NbtCompound> pair = this.getTargetedBlockEntity(bestWorld, mc);

            if (pair == null)
            {
                return;
            }
            if (Configs.Generic.INFO_LINES_USES_NBT.getBooleanValue() &&
                BlockUtils.getBlockEntityTypeFromNbt(pair.getRight()).equals(BlockEntityType.BEEHIVE) &&
                !pair.getRight().isEmpty())
            {
                Pair<List<BeehiveBlockEntity.BeeData>, BlockPos> bees = BlockUtils.getBeesDataFromNbt(pair.getRight());
                this.addLineI18n("minihud.info_line.bee_count.flower_pos", bees.getLeft().size(), bees.getRight().toShortString());
            }
            else if (pair.getLeft() instanceof BeehiveBlockEntity be)
            {
                this.addLineI18n("minihud.info_line.bee_count", ((BeehiveBlockEntity) be).getBeeCount());
            }
        }
        else if (type == InfoToggle.HONEY_LEVEL)
        {
            BlockState state = this.getTargetedBlock(mc);

            if (state != null && state.getBlock() instanceof BeehiveBlock)
            {
                this.addLineI18n("minihud.info_line.honey_level", BeehiveBlockEntity.getHoneyLevel(state));
            }
        }
        else if (type == InfoToggle.FURNACE_XP)
        {
            World bestWorld = WorldUtils.getBestWorld(mc);
            Pair<BlockEntity, NbtCompound> pair = this.getTargetedBlockEntity(bestWorld, mc);

            if (pair == null)
            {
                return;
            }

            if (Configs.Generic.INFO_LINES_USES_NBT.getBooleanValue() && !pair.getRight().isEmpty())
            {
                BlockEntityType<?> beType = BlockUtils.getBlockEntityTypeFromNbt(pair.getRight());

                if (beType.equals(BlockEntityType.FURNACE) ||
                    beType.equals(BlockEntityType.BLAST_FURNACE) ||
                    beType.equals(BlockEntityType.SMOKER))
                {
                    if (bestWorld instanceof ServerWorld serverWorld)
                    {
                        int exp = MiscUtils.getFurnaceXpAmount(serverWorld, pair.getRight());

                        if (exp > 0)
                        {
                            this.addLineI18n("minihud.info_line.furnace_xp", exp);
                        }
                    }
                    else if (this.getHudData().hasServuxServer() && this.getHudData().hasRecipes())
                    {
                        int exp = MiscUtils.getFurnaceXpAmount(pair.getRight());

                        if (exp > 0)
                        {
                            this.addLineI18n("minihud.info_line.furnace_xp", exp);
                        }
                    }
                }
            }
            else if (pair.getLeft() instanceof AbstractFurnaceBlockEntity furnace)
            {
                if (bestWorld instanceof ServerWorld serverWorld)
                {
                    int exp = MiscUtils.getFurnaceXpAmount(serverWorld, furnace);

                    if (exp > 0)
                    {
                        this.addLineI18n("minihud.info_line.furnace_xp", exp);
                    }
                }
                else if (this.getHudData().hasServuxServer() && this.getHudData().hasRecipes())
                {
                    int exp = MiscUtils.getFurnaceXpAmount(furnace);

                    if (exp > 0)
                    {
                        this.addLineI18n("minihud.info_line.furnace_xp", exp);
                    }
                }
            }
        }
        else if (type == InfoToggle.HORSE_SPEED ||
                 type == InfoToggle.HORSE_JUMP)
        {
            if (this.addedTypes.contains(InfoToggle.HORSE_SPEED) ||
                this.addedTypes.contains(InfoToggle.HORSE_JUMP))
            {
                return;
            }

            World bestWorld = WorldUtils.getBestWorld(mc);
            Pair<Entity, NbtCompound> pair = this.getTargetEntity(bestWorld, mc);
            Entity vehicle;

            if (pair == null)
            {
                vehicle = mc.player.getVehicle();
            }
            else
            {
                vehicle = pair.getLeft() == null ? mc.player.getVehicle() : pair.getLeft();
            }

            if (vehicle instanceof AbstractHorseEntity == false)
            {
                return;
            }

            AbstractHorseEntity horse = (AbstractHorseEntity) vehicle;
            String AnimalType = horse.getType().getName().getString();
            float speed = 0f;
            double jump = 0d;

            if (pair != null && Configs.Generic.INFO_LINES_USES_NBT.getBooleanValue() && !pair.getRight().isEmpty())
            {
                NbtCompound nbt = pair.getRight();
                EntityType<?> entityType = EntityUtils.getEntityTypeFromNbt(nbt);

                if (entityType.equals(EntityType.CAMEL) ||
                    entityType.equals(EntityType.DONKEY) ||
                    entityType.equals(EntityType.HORSE) ||
                    entityType.equals(EntityType.LLAMA) ||
                    entityType.equals(EntityType.MULE) ||
                    entityType.equals(EntityType.SKELETON_HORSE) ||
                    entityType.equals(EntityType.TRADER_LLAMA) ||
                    entityType.equals(EntityType.ZOMBIE_HORSE))
                {
                    Pair<Float, Float> horsePair = EntityUtils.getSpeedAndJumpStrengthFromNbt(nbt);
                    speed = horse.getMovementSpeed() > 0 ? horse.getMovementSpeed() : (float) horsePair.getLeft();
                    jump = horsePair.getRight();
                }
            }
            else
            {
                speed = horse.getMovementSpeed() > 0 ? horse.getMovementSpeed() : (float) horse.getAttributeValue(EntityAttributes.MOVEMENT_SPEED);
                jump = horse.getAttributeValue(EntityAttributes.JUMP_STRENGTH);
            }
            if (InfoToggle.HORSE_SPEED.getBooleanValue() && speed > 0f)
            {
                speed *= 42.1629629629629f;
                this.addLineI18n("minihud.info_line.horse_speed", AnimalType, speed);
                this.addedTypes.add(InfoToggle.HORSE_SPEED);
            }
            if (InfoToggle.HORSE_JUMP.getBooleanValue() && jump > 0d)
            {
                double calculatedJumpHeight =
                        -0.1817584952d * jump * jump * jump +
                                3.689713992d * jump * jump +
                                2.128599134d * jump +
                                -0.343930367;
                this.addLineI18n("minihud.info_line.horse_jump", AnimalType, calculatedJumpHeight);
                this.addedTypes.add(InfoToggle.HORSE_JUMP);
            }
        }
        else if (type == InfoToggle.ROTATION_YAW ||
                 type == InfoToggle.ROTATION_PITCH ||
                 type == InfoToggle.SPEED)
        {
            // Don't add the same line multiple times
            if (this.addedTypes.contains(InfoToggle.ROTATION_YAW) ||
                this.addedTypes.contains(InfoToggle.ROTATION_PITCH) ||
                this.addedTypes.contains(InfoToggle.SPEED))
            {
                return;
            }

            String pre = "";
            StringBuilder str = new StringBuilder(128);

            if (InfoToggle.ROTATION_YAW.getBooleanValue())
            {
                str.append(StringUtils.translate("minihud.info_line.rotation_yaw", MathHelper.wrapDegrees(entity.getYaw())));
                pre = " / ";
            }

            if (InfoToggle.ROTATION_PITCH.getBooleanValue())
            {
                str.append(pre).append(StringUtils.translate("minihud.info_line.rotation_pitch", MathHelper.wrapDegrees(entity.getPitch())));
                pre = " / ";
            }

            if (InfoToggle.SPEED.getBooleanValue())
            {
                double dx = entity.getX() - entity.lastRenderX;
                double dy = entity.getY() - entity.lastRenderY;
                double dz = entity.getZ() - entity.lastRenderZ;
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                str.append(pre).append(StringUtils.translate("minihud.info_line.speed", dist * 20));
            }

            this.addLine(str.toString());

            this.addedTypes.add(InfoToggle.ROTATION_YAW);
            this.addedTypes.add(InfoToggle.ROTATION_PITCH);
            this.addedTypes.add(InfoToggle.SPEED);
        }
        else if (type == InfoToggle.SPEED_HV)
        {
            double dx = entity.getX() - entity.lastRenderX;
            double dy = entity.getY() - entity.lastRenderY;
            double dz = entity.getZ() - entity.lastRenderZ;
            this.addLineI18n("minihud.info_line.speed_hv", Math.sqrt(dx * dx + dz * dz) * 20, dy * 20);
        }
        else if (type == InfoToggle.SPEED_AXIS)
        {
            double dx = entity.getX() - entity.lastRenderX;
            double dy = entity.getY() - entity.lastRenderY;
            double dz = entity.getZ() - entity.lastRenderZ;
            this.addLineI18n("minihud.info_line.speed_axis", dx * 20, dy * 20, dz * 20);
        }
        else if (type == InfoToggle.CHUNK_SECTIONS)
        {
            this.addLineI18n("minihud.info_line.chunk_sections", ((IMixinWorldRenderer) mc.worldRenderer).minihud_getRenderedChunksInvoker());
        }
        else if (type == InfoToggle.CHUNK_SECTIONS_FULL)
        {
            this.addLine(mc.worldRenderer.getChunksDebugString());
        }
        else if (type == InfoToggle.CHUNK_UPDATES)
        {
            this.addLine("TODO" /*String.format("Chunk updates: %d", ChunkRenderer.chunkUpdateCount)*/);
        }
        else if (type == InfoToggle.LOADED_CHUNKS_COUNT)
        {
            String chunksClient = mc.world.asString();
            World worldServer = WorldUtils.getBestWorld(mc);

            if (worldServer != null && worldServer != mc.world)
            {
                int chunksServer = worldServer.getChunkManager().getLoadedChunkCount();
                int chunksServerTot = ((ServerChunkManager) worldServer.getChunkManager()).getTotalChunksLoadedCount();
                this.addLineI18n("minihud.info_line.loaded_chunks_count.server", chunksServer, chunksServerTot, chunksClient);
            }
            else
            {
                this.addLine(chunksClient);
            }
        }
        else if (type == InfoToggle.PANDA_GENE)
        {
            Pair<Entity, NbtCompound> pair = this.getTargetEntity(world, mc);

            if (pair == null)
            {
                return;
            }
            if (Configs.Generic.INFO_LINES_USES_NBT.getBooleanValue() && !pair.getRight().isEmpty())
            {
                NbtCompound nbt = pair.getRight();
                EntityType<?> entityType = EntityUtils.getEntityTypeFromNbt(nbt);

                if (entityType.equals(EntityType.PANDA))
                {
                    Pair<PandaEntity.Gene, PandaEntity.Gene> genes = EntityUtils.getPandaGenesFromNbt(nbt);

                    if (genes.getLeft() != null && genes.getRight() != null)
                    {
                        this.addLineI18n("minihud.info_line.panda_gene.main_gene",
                                         StringUtils.translate("minihud.info_line.panda_gene.gene." + genes.getLeft().asString()),
                                         genes.getLeft().isRecessive() ? StringUtils.translate("minihud.info_line.panda_gene.recessive_gene") : StringUtils.translate("minihud.info_line.panda_gene.dominant_gene")
                        );
                        this.addLineI18n("minihud.info_line.panda_gene.hidden_gene",
                                         StringUtils.translate("minihud.info_line.panda_gene.gene." + genes.getRight().asString()),
                                         genes.getRight().isRecessive() ? StringUtils.translate("minihud.info_line.panda_gene.recessive_gene") : StringUtils.translate("minihud.info_line.panda_gene.dominant_gene")
                        );
                    }
                }
            }
            else if (pair.getLeft() instanceof PandaEntity panda)
            {
                this.addLineI18n("minihud.info_line.panda_gene.main_gene",
                        StringUtils.translate("minihud.info_line.panda_gene.gene." + panda.getMainGene().asString()),
                        panda.getMainGene().isRecessive() ? StringUtils.translate("minihud.info_line.panda_gene.recessive_gene") : StringUtils.translate("minihud.info_line.panda_gene.dominant_gene")
                );
                this.addLineI18n("minihud.info_line.panda_gene.hidden_gene",
                        StringUtils.translate("minihud.info_line.panda_gene.gene." + panda.getHiddenGene().asString()),
                        panda.getHiddenGene().isRecessive() ? StringUtils.translate("minihud.info_line.panda_gene.recessive_gene") : StringUtils.translate("minihud.info_line.panda_gene.dominant_gene")
                );
            }
        }
        else if (type == InfoToggle.PARTICLE_COUNT)
        {
            this.addLineI18n("minihud.info_line.particle_count", mc.particleManager.getDebugString());
        }
        else if (type == InfoToggle.DIFFICULTY)
        {
            long chunkInhabitedTime = 0L;
            float moonPhaseFactor = 0.0F;
            WorldChunk serverChunk = this.getChunk(chunkPos);

            if (serverChunk != null)
            {
                moonPhaseFactor = mc.world.getMoonSize();
                chunkInhabitedTime = serverChunk.getInhabitedTime();
            }

            LocalDifficulty diff = new LocalDifficulty(mc.world.getDifficulty(), mc.world.getTimeOfDay(), chunkInhabitedTime, moonPhaseFactor);
            this.addLineI18n("minihud.info_line.difficulty",
                    diff.getLocalDifficulty(), diff.getClampedLocalDifficulty(), mc.world.getTimeOfDay() / 24000L);
        }
        else if (type == InfoToggle.BIOME)
        {
            WorldChunk clientChunk = this.getClientChunk(chunkPos);

            if (clientChunk.isEmpty() == false)
            {
                Biome biome = mc.world.getBiome(pos).value();
                Identifier id = mc.world.getRegistryManager().getOrThrow(RegistryKeys.BIOME).getId(biome);
                String translationKey = "biome." + id.toString().replace(":", ".");
                String biomeName = StringUtils.translate(translationKey);
                if (biomeName.equals(translationKey))
                {
                    biomeName = StringUtils.prettifyRawTranslationPath(id.getPath());
                }

                this.addLineI18n("minihud.info_line.biome", biomeName);
            }
        }
        else if (type == InfoToggle.BIOME_REG_NAME)
        {
            WorldChunk clientChunk = this.getClientChunk(chunkPos);

            if (clientChunk.isEmpty() == false)
            {
                Biome biome = mc.world.getBiome(pos).value();
                Identifier rl = mc.world.getRegistryManager().getOrThrow(RegistryKeys.BIOME).getId(biome);
                String name = rl != null ? rl.toString() : "?";
                this.addLineI18n("minihud.info_line.biome_reg_name", name);
            }
        }
        else if (type == InfoToggle.ENTITIES)
        {
            String ent = mc.worldRenderer.getEntitiesDebugString();

            int p = ent.indexOf(",");

            if (p != -1)
            {
                ent = ent.substring(0, p);
            }

            this.addLine(ent);
        }
        else if (type == InfoToggle.TILE_ENTITIES)
        {
            // TODO 1.17
            //this.addLine(String.format("Client world TE - L: %d, T: %d", mc.world.blockEntities.size(), mc.world.tickingBlockEntities.size()));
            this.addLineI18n("minihud.info_line.tile_entities");
        }
        else if (type == InfoToggle.ENTITIES_CLIENT_WORLD)
        {
            int countClient = mc.world.getRegularEntityCount();

            if (mc.isIntegratedServerRunning())
            {
                World serverWorld = WorldUtils.getBestWorld(mc);

                if (serverWorld instanceof ServerWorld)
                {
                    IServerEntityManager manager = (IServerEntityManager) ((IMixinServerWorld) serverWorld).minihud_getEntityManager();
                    int indexSize = manager.minihud$getIndexSize();
                    this.addLineI18n("minihud.info_line.entities_client_world.server", countClient, indexSize);
                    return;
                }
            }

            this.addLineI18n("minihud.info_line.entities_client_world", countClient);
        }
        else if (type == InfoToggle.SLIME_CHUNK)
        {
            if (MiscUtils.isOverworld(world) == false)
            {
                return;
            }

            String result;

            if (this.getHudData().isWorldSeedKnown(world))
            {
                long seed = this.getHudData().getWorldSeed(world);

                if (MiscUtils.canSlimeSpawnAt(pos.getX(), pos.getZ(), seed))
                {
                    result = StringUtils.translate("minihud.info_line.slime_chunk.yes");
                }
                else
                {
                    result = StringUtils.translate("minihud.info_line.slime_chunk.no");
                }
            }
            else
            {
                result = StringUtils.translate("minihud.info_line.slime_chunk.no_seed");
            }

            this.addLineI18n("minihud.info_line.slime_chunk", result);
        }
        else if (type == InfoToggle.LOOKING_AT_ENTITY)
        {
            if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY)
            {
                Pair<Entity, NbtCompound> pair = this.getTargetEntity(world, mc);

                if (pair == null)
                {
                    return;
                }
                if (Configs.Generic.INFO_LINES_USES_NBT.getBooleanValue() &&
                    pair.getLeft() instanceof LivingEntity living && !pair.getRight().isEmpty())
                {
                    NbtCompound nbt = pair.getRight();
                    Pair<Float, Float> healthPair = EntityUtils.getHealthFromNbt(nbt);
                    Pair<UUID, ItemStack> ownerPair = EntityUtils.getOwnerAndSaddle(nbt, world.getRegistryManager());
                    Pair<Integer, Integer> agePair = EntityUtils.getAgeFromNbt(nbt);

                    float health = healthPair.getLeft();
                    float maxHealth = healthPair.getRight();

                    // Update the Health, as it might not be timely otherwise.
                    if (living.getHealth() != health)
                    {
                        health = living.getHealth();
                    }

                    String entityLine = StringUtils.translate("minihud.info_line.looking_at_entity.livingentity", pair.getLeft().getName().getString(), health, maxHealth);

                    if (ownerPair.getLeft() != Util.NIL_UUID)
                    {
                        LivingEntity owner = world.getPlayerByUuid(ownerPair.getLeft());

                        if (owner != null)
                        {
                            entityLine = entityLine + " - " + StringUtils.translate("minihud.info_line.looking_at_entity.owner") + ": " + owner.getName().getLiteralString();
                        }
                    }
                    if (agePair.getLeft() < 0)
                    {
                        int untilGrown = agePair.getLeft() * (-1);
                        entityLine = entityLine+ " [" + StringUtils.getDurationString(untilGrown * 50) + " " + StringUtils.translate("minihud.info_line.remaining") + "]";
                    }
                    this.addLine(entityLine);
                }
                else if (pair.getLeft() instanceof LivingEntity living)
                {
                    String entityLine = StringUtils.translate("minihud.info_line.looking_at_entity.livingentity", living.getName().getString(), living.getHealth(), living.getMaxHealth());

                    if (living instanceof Tameable tamable)
                    {
                        LivingEntity owner = tamable.getOwner();
                        if (owner != null)
                        {
                            entityLine = entityLine + " - " + StringUtils.translate("minihud.info_line.looking_at_entity.owner") + ": " + owner.getName().getLiteralString();
                        }
                    }
                    if (living instanceof PassiveEntity passive)
                    {
                        if (passive.getBreedingAge() < 0)
                        {
                            int untilGrown = ((IMixinPassiveEntity) passive).minihud_getRealBreedingAge() * (-1);
                            entityLine = entityLine+ " [" + StringUtils.getDurationString(untilGrown * 50) + " " + StringUtils.translate("minihud.info_line.remaining") + "]";
                        }
                    }

                    this.addLine(entityLine);
                }
                else
                {
                    this.addLineI18n("minihud.info_line.looking_at_entity", pair.getLeft().getName().getString());
                }
            }
        }
        else if (type == InfoToggle.ENTITY_VARIANT)
        {
            if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY)
            {
                Pair<Entity, NbtCompound> pair = this.getTargetEntity(world, mc);

                if (pair == null)
                {
                    return;
                }
                if (Configs.Generic.INFO_LINES_USES_NBT.getBooleanValue() &&
                        pair.getLeft() instanceof LivingEntity living && !pair.getRight().isEmpty())
                {
                    NbtCompound nbt = pair.getRight();
                    EntityType<?> entityType = EntityUtils.getEntityTypeFromNbt(nbt);

                    if (entityType.equals(EntityType.AXOLOTL))
                    {
                        AxolotlEntity.Variant variant = EntityUtils.getAxolotlVariantFromNbt(nbt);

                        if (variant != null)
                        {
                            this.addLineI18n("minihud.info_line.entity_variant.axolotl", variant.getName());
                        }
                    }
                    else if (entityType.equals(EntityType.CAT))
                    {
                        Pair<RegistryKey<CatVariant>, DyeColor> catPair = EntityUtils.getCatVariantFromNbt(nbt);

                        if (catPair.getLeft() != null)
                        {
                            this.addLineI18n("minihud.info_line.entity_variant.cat", catPair.getLeft().getValue().getPath(), catPair.getRight().getName());
                        }
                    }
                    else if (entityType.equals(EntityType.FROG))
                    {
                        RegistryKey<FrogVariant> variant = EntityUtils.getFrogVariantFromNbt(nbt);

                        if (variant != null)
                        {
                            this.addLineI18n("minihud.info_line.entity_variant.frog", variant.getValue().getPath());
                        }
                    }
                    else if (entityType.equals(EntityType.HORSE))
                    {
                        Pair<HorseColor, HorseMarking> horsePair = EntityUtils.getHorseVariantFromNbt(nbt);

                        if (horsePair.getLeft() != null)
                        {
                            this.addLineI18n("minihud.info_line.entity_variant.horse", horsePair.getLeft().asString(), horsePair.getRight().name().toLowerCase());
                        }
                    }
                    else if (entityType.equals(EntityType.PARROT))
                    {
                        ParrotEntity.Variant variant = EntityUtils.getParrotVariantFromNbt(nbt);

                        if (variant != null)
                        {
                            this.addLineI18n("minihud.info_line.entity_variant.parrot", variant.asString());
                        }
                    }
                    else if (entityType.equals(EntityType.SHEEP))
                    {
                        DyeColor color = EntityUtils.getSheepColorFromNbt(nbt);

                        if (color != null)
                        {
                            this.addLineI18n("minihud.info_line.entity_variant.sheep", color.getName());
                        }
                    }
                    else if (entityType.equals(EntityType.TROPICAL_FISH))
                    {
                        TropicalFishEntity.Variety variant = EntityUtils.getFishVariantFromNbt(nbt);

                        if (variant != null)
                        {
                            this.addLineI18n("minihud.info_line.entity_variant.tropical_fish", variant.asString());
                        }
                    }
                    else if (entityType.equals(EntityType.WOLF))
                    {
                        Pair<RegistryKey<WolfVariant>, DyeColor> wolfPair = EntityUtils.getWolfVariantFromNbt(nbt);

                        if (wolfPair.getLeft() != null)
                        {
                            this.addLineI18n("minihud.info_line.entity_variant.wolf", wolfPair.getLeft().getValue().getPath(), wolfPair.getRight().getName());
                        }
                    }
                }
                else if (pair.getLeft() instanceof AxolotlEntity axolotl)
                {
                    this.addLineI18n("minihud.info_line.entity_variant.axolotl", axolotl.getVariant().getName());
                }
                else if (pair.getLeft() instanceof CatEntity cat)
                {
                    RegistryKey<CatVariant> variant = cat.getVariant().getKey().orElse(CatVariant.ALL_BLACK);
                    this.addLineI18n("minihud.info_line.entity_variant.cat", variant.getValue().getPath(), cat.getCollarColor().getName());
                }
                else if (pair.getLeft() instanceof FrogEntity frog)
                {
                    RegistryKey<FrogVariant> variant = frog.getVariant().getKey().orElse(FrogVariant.TEMPERATE);
                    this.addLineI18n("minihud.info_line.entity_variant.frog", variant.getValue().getPath());
                }
                else if (pair.getLeft() instanceof HorseEntity horse)
                {
                    this.addLineI18n("minihud.info_line.entity_variant.horse", horse.getVariant().asString(), horse.getMarking().name().toLowerCase());
                }
                else if (pair.getLeft() instanceof ParrotEntity parrot)
                {
                    this.addLineI18n("minihud.info_line.entity_variant.parrot", parrot.getVariant().asString());
                }
                else if (pair.getLeft() instanceof SheepEntity sheep)
                {
                    this.addLineI18n("minihud.info_line.entity_variant.sheep", sheep.getColor().getName());
                }
                else if (pair.getLeft() instanceof TropicalFishEntity fish)
                {
                    this.addLineI18n("minihud.info_line.entity_variant.tropical_fish", fish.getVariant().asString());
                }
                else if (pair.getLeft() instanceof WolfEntity wolf)
                {
                    RegistryKey<WolfVariant> variant = wolf.getVariant().getKey().orElse(WolfVariants.PALE);
                    this.addLineI18n("minihud.info_line.entity_variant.wolf", variant.getValue().getPath(), wolf.getCollarColor().getName());
                }
            }
        }
        else if (type == InfoToggle.LOOKING_AT_EFFECTS)
        {
            if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY)
            {
                Pair<Entity, NbtCompound> pair = this.getTargetEntity(world, mc);

                if (pair == null)
                {
                    return;
                }
                if (Configs.Generic.INFO_LINES_USES_NBT.getBooleanValue() &&
                    pair.getLeft() instanceof LivingEntity && !pair.getRight().isEmpty())
                {
                    NbtCompound nbt = pair.getRight();
                    Map<RegistryEntry<StatusEffect>, StatusEffectInstance> effects = EntityUtils.getActiveStatusEffectsFromNbt(nbt);

                    if (effects == null || effects.isEmpty())
                    {
                        return;
                    }

                    for (RegistryEntry<StatusEffect> effectType : effects.keySet())
                    {
                        StatusEffectInstance effect = effects.get(effectType);

                        if (effect.isInfinite() || effect.getDuration() > 0)
                        {
                            this.addLineI18n("minihud.info_line.looking_at_effects",
                                             effectType.value().getName().getString(),
                                             effect.getAmplifier() > 0 ? StringUtils.translate("minihud.info_line.looking_at_effects.amplifier", effect.getAmplifier() + 1) : "",
                                             effect.isInfinite() ? StringUtils.translate("minihud.info_line.looking_at_effects.infinite") :
                                             StringUtils.getDurationString((effect.getDuration() / 20) * 1000L),
                                             StringUtils.translate("minihud.info_line.remaining")
                            );
                        }
                    }
                }
                else if (pair.getLeft() instanceof LivingEntity living)
                {
                    Collection<StatusEffectInstance> effects = living.getStatusEffects();
                    Iterator<StatusEffectInstance> iter = effects.iterator();

                    while (iter.hasNext())
                    {
                        StatusEffectInstance effect = iter.next();

                        if (effect.isInfinite() || effect.getDuration() > 0)
                        {
                            this.addLineI18n("minihud.info_line.looking_at_effects",
                                    effect.getEffectType().value().getName().getString(),
                                    effect.getAmplifier() > 0 ? StringUtils.translate("minihud.info_line.looking_at_effects.amplifier", effect.getAmplifier() + 1) : "",
                                    effect.isInfinite() ? StringUtils.translate("minihud.info_line.looking_at_effects.infinite") :
                                    StringUtils.getDurationString((effect.getDuration() / 20) * 1000L),
                                    StringUtils.translate("minihud.info_line.remaining")
                            );
                        }
                    }
                }
            }
        }
        else if (type == InfoToggle.ZOMBIE_CONVERSION)
        {
            if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY)
            {
                Pair<Entity, NbtCompound> pair = this.getTargetEntity(world, mc);

                if (pair == null)
                {
                    return;
                }

                String zombieType = pair.getLeft().getType().getName().getString();
                int conversionTimer = -1;

                if (Configs.Generic.INFO_LINES_USES_NBT.getBooleanValue() && !pair.getRight().isEmpty())
                {
                    NbtCompound nbt = pair.getRight();
                    EntityType<?> entityType = EntityUtils.getEntityTypeFromNbt(nbt);

                    //MiniHUD.logger.error("ZombieDoctor: type [{}], raw nbt [{}]", entityType.getName().getString(), nbt.toString());

                    if (entityType.equals(EntityType.ZOMBIE_VILLAGER))
                    {
                        Pair<Integer, UUID> zombieDoctor = EntityUtils.getZombieConversionTimerFromNbt(nbt);
                        conversionTimer = zombieDoctor.getLeft();
                    }
                    else if (entityType.equals(EntityType.ZOMBIE))
                    {
                        Pair<Integer, Integer> zombieDoctor = EntityUtils.getDrownedConversionTimerFromNbt(nbt);
                        conversionTimer = zombieDoctor.getLeft();
                    }
                    else if (entityType.equals(EntityType.SKELETON))
                    {
                        conversionTimer = EntityUtils.getStrayConversionTimeFromNbt(nbt);
                    }
                }
                else
                {
                    if (pair.getLeft() instanceof ZombieVillagerEntity zombie)
                    {
                        conversionTimer = ((IMixinZombieVillagerEntity) zombie).minihud_conversionTimer();
                    }
                    else if (pair.getLeft() instanceof ZombieEntity zombert)
                    {
                        conversionTimer = ((IMixinZombieEntity) zombert).minihud_ticksUntilWaterConversion();
                    }
                    else if (pair.getLeft() instanceof SkeletonEntity skeleton)
                    {
                        conversionTimer = ((IMixinSkeletonEntity) skeleton).minihud_conversionTime();
                    }
                }
                if (conversionTimer > 0)
                {
                    this.addLineI18n("minihud.info_line.zombie_conversion", zombieType, StringUtils.getDurationString((conversionTimer / 20) * 1000L));
                }
            }
        }
        else if (type == InfoToggle.ENTITY_REG_NAME)
        {
            if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY)
            {
                Pair<Entity, NbtCompound> pair = this.getTargetEntity(world, mc);

                if (pair == null)
                {
                    return;
                }
                Identifier regName = EntityType.getId(pair.getLeft().getType());

                if (regName != null)
                {
                    this.addLineI18n("minihud.info_line.entity_reg_name", regName);
                }
            }
        }
        else if (type == InfoToggle.PLAYER_EXPERIENCE)
        {
            if (mc.player != null)
            {
                this.addLineI18n("minihud.info_line.player_experience", mc.player.experienceLevel, mc.player.experienceProgress, mc.player.totalExperience);
            }
        }
        else if (type == InfoToggle.LOOKING_AT_PLAYER_EXP)
        {
            if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY)
            {
                Pair<Entity, NbtCompound> pair = this.getTargetEntity(world, mc);

                if (pair == null)
                {
                    return;
                }
                if (Configs.Generic.INFO_LINES_USES_NBT.getBooleanValue() && !pair.getRight().isEmpty())
                {
                    NbtCompound nbt = pair.getRight();
                    EntityType<?> entityType = EntityUtils.getEntityTypeFromNbt(nbt);

                    if (entityType.equals(EntityType.PLAYER))
                    {
                        Triple<Integer, Integer, Float> triple = EntityUtils.getPlayerExpFromNbt(nbt);

                        if (triple.getLeft() > 0)
                        {
                            this.addLineI18n("minihud.info_line.looking_at_player_exp", triple.getLeft(), triple.getRight(), triple.getMiddle());
                        }
                    }
                }
                else if (pair.getLeft() instanceof ServerPlayerEntity player)
                {
                    this.addLineI18n("minihud.info_line.looking_at_player_exp", player.experienceLevel, player.experienceProgress, player.totalExperience);
                }
            }
        }
        else if (type == InfoToggle.LOOKING_AT_BLOCK ||
                 type == InfoToggle.LOOKING_AT_BLOCK_CHUNK)
        {
            // Don't add the same line multiple times
            if (this.addedTypes.contains(InfoToggle.LOOKING_AT_BLOCK) ||
                this.addedTypes.contains(InfoToggle.LOOKING_AT_BLOCK_CHUNK))
            {
                return;
            }

            if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK)
            {
                BlockPos lookPos = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
                String pre = "";
                StringBuilder str = new StringBuilder(128);

                if (InfoToggle.LOOKING_AT_BLOCK.getBooleanValue())
                {
                    str.append(StringUtils.translate("minihud.info_line.looking_at_block", lookPos.getX(), lookPos.getY(), lookPos.getZ()));
                    pre = " // ";
                }

                if (InfoToggle.LOOKING_AT_BLOCK_CHUNK.getBooleanValue())
                {
                    str.append(pre).append(StringUtils.translate("minihud.info_line.looking_at_block_chunk",
                            lookPos.getX() & 0xF, lookPos.getY() & 0xF, lookPos.getZ() & 0xF,
                            lookPos.getX() >> 4, lookPos.getY() >> 4, lookPos.getZ() >> 4));
                }

                this.addLine(str.toString());

                this.addedTypes.add(InfoToggle.LOOKING_AT_BLOCK);
                this.addedTypes.add(InfoToggle.LOOKING_AT_BLOCK_CHUNK);
            }
        }
        else if (type == InfoToggle.BLOCK_PROPS)
        {
            this.getBlockProperties(mc);
        }
    }

    @Nullable
    public Pair<Entity, NbtCompound> getTargetEntity(World world, MinecraftClient mc)
    {
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY)
        {
            Entity lookedEntity = ((EntityHitResult) mc.crosshairTarget).getEntity();
            World bestWorld = WorldUtils.getBestWorld(mc);
            Pair<Entity, NbtCompound> pair = null;

            if (bestWorld instanceof ServerWorld serverWorld)
            {
                Entity serverEntity = serverWorld.getEntityById(lookedEntity.getId());
                NbtCompound nbt = new NbtCompound();
                serverEntity.saveSelfNbt(nbt);
                pair = Pair.of(serverEntity, nbt);
            }
            else
            {
                pair = EntitiesDataManager.getInstance().requestEntity(lookedEntity.getId());
            }

            // Remember the last entity so the "refresh time" is smoothed over.
            if (pair == null && this.lastEntity != null &&
                this.lastEntity.getLeft().getId() == lookedEntity.getId())
            {
                pair = this.lastEntity;
            }
            else if (pair != null)
            {
                this.lastEntity = pair;
            }

            return pair;
        }

        return null;
    }

    /*
    @Nullable
    public BlockEntity getTargetedBlockEntity(World world, MinecraftClient mc)
    {
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK)
        {
            BlockPos posLooking = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
            WorldChunk chunk = this.getChunk(new ChunkPos(posLooking));

            requestBlockEntityAt(world, posLooking);
            // The method in World now checks that the caller is from the same thread...
            return chunk != null ? chunk.getBlockEntity(posLooking) : null;
        }

        return null;
    }
     */

    @Nullable
    public Pair<BlockEntity, NbtCompound> getTargetedBlockEntity(World world, MinecraftClient mc)
    {
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK)
        {
            BlockPos posLooking = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
            World bestWorld = WorldUtils.getBestWorld(mc);
            BlockState state = bestWorld.getBlockState(posLooking);
            Pair<BlockEntity, NbtCompound> pair = null;

            if (state.getBlock() instanceof BlockEntityProvider)
            {
                if (bestWorld instanceof ServerWorld)
                {
                    NbtCompound nbt = new NbtCompound();
                    BlockEntity be = bestWorld.getWorldChunk(posLooking).getBlockEntity(posLooking);
                    pair = Pair.of(be, be != null ? be.createNbtWithIdentifyingData(bestWorld.getRegistryManager()) : nbt);
                }
                else
                {
                    pair = EntitiesDataManager.getInstance().requestBlockEntity(world, posLooking);
                }

                // Remember the last entity so the "refresh time" is smoothed over.
                if (pair == null && this.lastBlockEntity != null &&
                    this.lastBlockEntity.getLeft().getPos().equals(posLooking))
                {
                    pair = this.lastBlockEntity;
                }
                else if (pair != null)
                {
                    this.lastBlockEntity = pair;
                }

                return pair;
            }
        }

        return null;
    }

    @Nullable
    public Pair<BlockEntity, NbtCompound> requestBlockEntityAt(World world, BlockPos pos)
    {
        if (!(world instanceof ServerWorld))
        {
            Pair<BlockEntity, NbtCompound> pair = EntitiesDataManager.getInstance().requestBlockEntity(world, pos);

            BlockState state = world.getBlockState(pos);

            if (state.getBlock() instanceof ChestBlock)
            {
                ChestType type = state.get(ChestBlock.CHEST_TYPE);

                if (type != ChestType.SINGLE)
                {
                    return EntitiesDataManager.getInstance().requestBlockEntity(world, pos.offset(ChestBlock.getFacing(state)));
                }
            }

            return pair;
        }

        return null;
    }

    @Nullable
    private BlockState getTargetedBlock(MinecraftClient mc)
    {
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK)
        {
            BlockPos posLooking = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
            return mc.world.getBlockState(posLooking);
        }

        return null;
    }

    private <T extends Comparable<T>> void getBlockProperties(MinecraftClient mc)
    {
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK)
        {
            BlockPos posLooking = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
            BlockState state = mc.world.getBlockState(posLooking);
            Identifier rl = Registries.BLOCK.getId(state.getBlock());

            this.addLine(rl != null ? rl.toString() : "<null>");

            for (String line : BlockUtils.getFormattedBlockStateProperties(state))
            {
                this.addLine(line);
            }
        }
    }

    @Nullable
    private WorldChunk getChunk(ChunkPos chunkPos)
    {
        CompletableFuture<OptionalChunk<Chunk>> future = this.chunkFutures.get(chunkPos);

        if (future == null)
        {
            future = this.setupChunkFuture(chunkPos);
        }

        OptionalChunk<Chunk> chunkResult = future.getNow(null);
        if (chunkResult == null)
        {
            return null;
        }
        else
        {
            Chunk chunk = chunkResult.orElse(null);
            if (chunk instanceof WorldChunk)
            {
                return (WorldChunk) chunk;
            }
            else
            {
                return null;
            }
        }
    }

    private CompletableFuture<OptionalChunk<Chunk>> setupChunkFuture(ChunkPos chunkPos)
    {
        IntegratedServer server = this.getDataStorage().getIntegratedServer();
        CompletableFuture<OptionalChunk<Chunk>> future = null;

        if (server != null)
        {
            ServerWorld world = server.getWorld(this.mc.world.getRegistryKey());

            if (world != null)
            {
                future = world.getChunkManager().getChunkFutureSyncOnMainThread(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false)
                        .thenApply((either) -> either.map((chunk) -> (WorldChunk) chunk) );
            }
        }

        if (future == null)
        {
            future = CompletableFuture.completedFuture(OptionalChunk.of(this.getClientChunk(chunkPos)));
        }

        this.chunkFutures.put(chunkPos, future);

        return future;
    }

    private WorldChunk getClientChunk(ChunkPos chunkPos)
    {
        if (this.cachedClientChunk == null || this.cachedClientChunk.getPos().equals(chunkPos) == false)
        {
            this.cachedClientChunk = this.mc.world.getChunk(chunkPos.x, chunkPos.z);
        }

        return this.cachedClientChunk;
    }

    private void resetCachedChunks()
    {
        this.chunkFutures.clear();
        this.cachedClientChunk = null;
    }

    private class StringHolder implements Comparable<StringHolder>
    {
        public final String str;

        public StringHolder(String str)
        {
            this.str = str;
        }

        @Override
        public int compareTo(StringHolder other)
        {
            int lenThis = this.str.length();
            int lenOther = other.str.length();

            if (lenThis == lenOther)
            {
                return 0;
            }

            return this.str.length() > other.str.length() ? -1 : 1;
        }
    }

    private static class LinePos implements Comparable<LinePos>
    {
        private final int position;
        private final InfoToggle type;

        private LinePos(int position, InfoToggle type)
        {
            this.position = position;
            this.type = type;
        }

        @Override
        public int compareTo(@Nonnull LinePos other)
        {
            if (this.position < 0)
            {
                return other.position >= 0 ? 1 : 0;
            }
            else if (other.position < 0 && this.position >= 0)
            {
                return -1;
            }

            return this.position < other.position ? -1 : (this.position > other.position ? 1 : 0);
        }
    }
}
