package fi.dy.masa.minihud.event;

import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.BlockUtils;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.WorldUtils;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.data.EntitiesDataStorage;
import fi.dy.masa.minihud.data.InfoLinesHandler;
import fi.dy.masa.minihud.data.MobCapDataHandler;
import fi.dy.masa.minihud.mixin.IMixinPassiveEntity;
import fi.dy.masa.minihud.mixin.IMixinServerWorld;
import fi.dy.masa.minihud.mixin.IMixinWorldRenderer;
import fi.dy.masa.minihud.mixin.IMixinZombieVillagerEntity;
import fi.dy.masa.minihud.network.ServuxEntitiesPacket;
import fi.dy.masa.minihud.renderer.OverlayRenderer;
import fi.dy.masa.minihud.util.*;

import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.BlockEntity;
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
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.PandaEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.OptionalChunk;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
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
import net.minecraft.world.level.LevelProperties;

import com.llamalad7.mixinextras.lib.apache.commons.tuple.Pair;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class RenderHandler implements IRenderer
{
    private static final RenderHandler INSTANCE = new RenderHandler();

    private final MinecraftClient mc;
    private final DataStorage data;
    private final Date date;
    private final Map<ChunkPos, CompletableFuture<OptionalChunk<Chunk>>> chunkFutures = new HashMap<>();
    private final Set<InfoToggle> addedTypes = new HashSet<>();
    @Nullable private WorldChunk cachedClientChunk;
    private long infoUpdateTime;

    private final List<StringHolder> lineWrappers = new ArrayList<>();
    private final List<String> lines = new ArrayList<>();

    public RenderHandler()
    {
        this.mc = MinecraftClient.getInstance();
        this.data = DataStorage.getInstance();
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

    public static void fixDebugRendererState()
    {
        //if (Configs.Generic.FIX_VANILLA_DEBUG_RENDERERS.getBooleanValue())
        //{
            //RenderSystem.disableLighting();
            //RenderUtils.color(1, 1, 1, 1);
            //OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        //}
    }

    @Override
    public void onRenderGameOverlayPostAdvanced(DrawContext drawContext, float partialTicks, Profiler profiler, MinecraftClient mc)
    {
        if (Configs.Generic.MAIN_RENDERING_TOGGLE.getBooleanValue() == false)
        {
            this.resetCachedChunks();
            return;
        }

        if (this.mc.getDebugHud().shouldShowDebugHud() == false &&
            this.mc.player != null && this.mc.options.hudHidden == false &&
            (Configs.Generic.REQUIRE_SNEAK.getBooleanValue() == false || this.mc.player.isSneaking()) &&
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
            var inventory = RayTraceUtils.getTargetInventory(this.mc, true);

            if (inventory != null)
            {
                fi.dy.masa.minihud.renderer.RenderUtils.renderInventoryOverlay(inventory, drawContext);
            }

            // OG method (Works with Crafters also)
            //fi.dy.masa.minihud.renderer.RenderUtils.renderInventoryOverlay(this.mc, drawContext);
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
                fi.dy.masa.malilib.render.RenderUtils.renderMapPreview(stack, x, y, Configs.Generic.MAP_PREVIEW_SIZE.getIntegerValue(), false, drawContext);
            }
        }
        else if (stack.getComponents().contains(DataComponentTypes.CONTAINER) && InventoryUtils.shulkerBoxHasItems(stack))
        {
            if (Configs.Generic.SHULKER_BOX_PREVIEW.getBooleanValue() &&
               (Configs.Generic.SHULKER_DISPLAY_REQUIRE_SHIFT.getBooleanValue() == false || GuiBase.isShiftDown()))
            {
                fi.dy.masa.malilib.render.RenderUtils.renderShulkerBoxPreview(stack, x, y, Configs.Generic.SHULKER_DISPLAY_BACKGROUND_COLOR.getBooleanValue(), drawContext);
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
            InfoLinesHandler.getInstance().addFPS();
        }
        else if (type == InfoToggle.MEMORY_USAGE)
        {
            InfoLinesHandler.getInstance().addMemoryUsage();
        }
        else if (type == InfoToggle.TIME_REAL)
        {
            InfoLinesHandler.getInstance().addRealTime();
        }
        else if (type == InfoToggle.TIME_WORLD)
        {
            InfoLinesHandler.getInstance().addWorldTime(world);
        }
        else if (type == InfoToggle.TIME_WORLD_FORMATTED)
        {
            InfoLinesHandler.getInstance().addWorldTimeFormatted(world);
        }
        else if (type == InfoToggle.TIME_DAY_MODULO)
        {
            InfoLinesHandler.getInstance().addDayTimeModulo(world);
        }
        else if (type == InfoToggle.TIME_TOTAL_MODULO)
        {
            InfoLinesHandler.getInstance().addTotalTimeModulo(world);
        }
        else if (type == InfoToggle.SERVER_TPS)
        {
            InfoLinesHandler.getInstance().addServerTPS();
        }
        else if (type == InfoToggle.SERVUX)
        {
            InfoLinesHandler.getInstance().addServux();
        }
        else if (type == InfoToggle.WEATHER)
        {
            InfoLinesHandler.getInstance().addWeather();
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
            BlockEntity be = this.getTargetedBlockEntity(bestWorld, mc);

            if (be instanceof BeehiveBlockEntity)
            {
                this.addLineI18n("minihud.info_line.bee_count", ((BeehiveBlockEntity) be).getBeeCount());
            }
        }
        else if (type == InfoToggle.FURNACE_XP)
        {
            World bestWorld = WorldUtils.getBestWorld(mc);
            BlockEntity be = this.getTargetedBlockEntity(bestWorld, mc);

            if (be instanceof AbstractFurnaceBlockEntity furnace)
            {
                this.addLineI18n("minihud.info_line.furnace_xp", MiscUtils.getFurnaceXpAmount(furnace));
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
        else if (type == InfoToggle.HORSE_SPEED ||
                 type == InfoToggle.HORSE_JUMP)
        {
            if (this.addedTypes.contains(InfoToggle.HORSE_SPEED) ||
                this.addedTypes.contains(InfoToggle.HORSE_JUMP))
            {
                return;
            }

            World bestWorld = WorldUtils.getBestWorld(mc);
            Entity targeted = this.getTargetEntity(bestWorld, this.mc);
            Entity vehicle = targeted == null ? this.mc.player.getVehicle() : targeted;

            if (vehicle instanceof AbstractHorseEntity == false)
            {
                return;
            }

            AbstractHorseEntity horse = (AbstractHorseEntity) vehicle;
            String AnimalType = horse.getType().getName().getString();

            if (InfoToggle.HORSE_SPEED.getBooleanValue())
            {
                float speed = horse.getMovementSpeed() > 0 ? horse.getMovementSpeed() : (float) horse.getAttributeValue(EntityAttributes.MOVEMENT_SPEED);
                speed *= 42.1629629629629f;
                this.addLineI18n("minihud.info_line.horse_speed", AnimalType, speed);
                this.addedTypes.add(InfoToggle.HORSE_SPEED);
            }

            if (InfoToggle.HORSE_JUMP.getBooleanValue())
            {
                double jump = horse.getAttributeValue(EntityAttributes.JUMP_STRENGTH);
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
            if (this.getTargetEntity(world, mc) instanceof PandaEntity panda)
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
                this.addLineI18n("minihud.info_line.biome", StringUtils.translate("biome." + id.toString().replace(":", ".")));
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

            if (this.data.isWorldSeedKnown(world))
            {
                long seed = this.data.getWorldSeed(world);

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
                Entity lookedEntity = this.getTargetEntity(world, mc);
                if (lookedEntity instanceof LivingEntity living)
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
                            int untilGrown = ((IMixinPassiveEntity) passive).getRealBreedingAge() * (-1);
                            entityLine = entityLine+ " [" + DurationFormatUtils.formatDurationWords(untilGrown * 50, true, true) + " " + StringUtils.translate("minihud.info_line.remaining") + "]";
                        }
                    }

                    this.addLine(entityLine);
                }
                else
                {
                    this.addLineI18n("minihud.info_line.looking_at_entity", lookedEntity.getName().getString());
                }
            }
        }
        else if (type == InfoToggle.LOOKING_AT_EFFECTS)
        {
            if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY)
            {
                Entity lookedEntity = this.getTargetEntity(world, mc);
                if (lookedEntity instanceof LivingEntity living)
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
                                            DurationFormatUtils.formatDurationWords((effect.getDuration() / 20) * 1000L, true, true),
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
                Entity lookedEntity = this.getTargetEntity(world, mc);
                if (lookedEntity instanceof ZombieVillagerEntity zombie)
                {
                    int conversionTimer = ((IMixinZombieVillagerEntity) zombie).minihud_conversionTimer();
                    if (conversionTimer > 0)
                    {
                        this.addLineI18n("minihud.info_line.zombie_conversion", DurationFormatUtils.formatDurationWords((conversionTimer / 20) * 1000L, true, true));
                    }
                }
            }
        }
        else if (type == InfoToggle.ENTITY_REG_NAME)
        {
            if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY)
            {
                Entity lookedEntity = this.getTargetEntity(world, mc);
                Identifier regName = EntityType.getId(lookedEntity.getType());

                if (regName != null)
                {
                    this.addLineI18n("minihud.info_line.entity_reg_name", regName);
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
    public Entity getTargetEntity(World world, MinecraftClient mc)
    {
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY)
        {
            Entity lookedEntity = ((EntityHitResult) mc.crosshairTarget).getEntity();
            if (!(world instanceof ServerWorld))
            {
                EntitiesDataStorage.getInstance().requestEntity(lookedEntity.getId());
            }

            return lookedEntity;
        }
        return null;
    }

    @Nullable
    public NbtCompound getTargetEntityNbt(World world, MinecraftClient mc)
    {
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY)
        {
            Entity lookedEntity = ((EntityHitResult) mc.crosshairTarget).getEntity();
            return EntitiesDataStorage.getInstance().requestEntity(lookedEntity.getId()).getRight();
        }
        return null;
    }

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

    @Nullable
    public NbtCompound getTargetedBlockEntityNbt(World world, MinecraftClient mc)
    {
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK)
        {
            BlockPos posLooking = ((BlockHitResult) mc.crosshairTarget).getBlockPos();

            return requestBlockEntityAt(world, posLooking).getRight();
        }

        return null;
    }

    @Nullable
    public Pair<BlockEntity, NbtCompound> requestBlockEntityAt(World world, BlockPos pos)
    {
        if (!(world instanceof ServerWorld))
        {
            Pair<BlockEntity, NbtCompound> pair = EntitiesDataStorage.getInstance().requestBlockEntity(world, pos);

            BlockState state = world.getBlockState(pos);

            if (state.getBlock() instanceof ChestBlock)
            {
                ChestType type = state.get(ChestBlock.CHEST_TYPE);

                if (type != ChestType.SINGLE)
                {
                    BlockPos posAdj = pos.offset(ChestBlock.getFacing(state));
                    return EntitiesDataStorage.getInstance().requestBlockEntity(world, posAdj);
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
        IntegratedServer server = this.mc.getServer();
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
