package fi.dy.masa.minihud.event;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.info.InfoLine;

import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;

import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.*;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.OptionalChunk;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.LightType;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;

import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.WorldUtils;
import fi.dy.masa.malilib.util.game.BlockUtils;
import fi.dy.masa.malilib.util.nbt.NbtEntityUtils;
import fi.dy.masa.malilib.util.nbt.NbtInventory;
import fi.dy.masa.malilib.util.nbt.NbtKeys;
import fi.dy.masa.malilib.util.nbt.NbtView;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.data.EntitiesDataManager;
import fi.dy.masa.minihud.data.HudDataManager;
import fi.dy.masa.minihud.data.MobCapDataHandler;
import fi.dy.masa.minihud.info.InfoLineChunkCache;
import fi.dy.masa.minihud.mixin.*;
import fi.dy.masa.minihud.mixin.world.IMixinServerWorld;
import fi.dy.masa.minihud.renderer.InventoryOverlayHandler;
import fi.dy.masa.minihud.renderer.OverlayRenderer;
import fi.dy.masa.minihud.util.DataStorage;
import fi.dy.masa.minihud.util.IServerEntityManager;
import fi.dy.masa.minihud.util.MiscUtils;
import fi.dy.masa.minihud.util.SpeedUnits;

public class RenderHandler implements IRenderer
{
    private static final RenderHandler INSTANCE = new RenderHandler();

    private final MinecraftClient mc;
    private final DataStorage data;
    private final HudDataManager hudData;
    private final Date date;
//    private final Map<ChunkPos, CompletableFuture<OptionalChunk<Chunk>>> chunkFutures = new HashMap<>();
    private final Set<InfoToggle> addedTypes = new HashSet<>();
//    @Nullable private WorldChunk cachedClientChunk;
    private long infoUpdateTime;

    private final List<StringHolder> lineWrappers = new ArrayList<>();
    private final List<String> lines = new ArrayList<>();
    private Pair<BlockEntity, NbtCompound> lastBlockEntity = null;
    private Pair<Entity, NbtCompound> lastEntity = null;
    private Pair<Entity, NbtCompound> lastEnderItems = null;

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
//            this.resetCachedChunks();
            InfoLineChunkCache.INSTANCE.onReset();
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

            RenderUtils.renderText(drawContext, x, y, Configs.Generic.FONT_SCALE.getDoubleValue(), textColor, bgColor, alignment,
                                   useBackground, useShadow, Configs.Generic.HUD_STATUS_EFFECTS_SHIFT.getBooleanValue(),
                                   this.lines);
        }

        if (Configs.Generic.INVENTORY_PREVIEW_ENABLED.getBooleanValue() &&
            Configs.Generic.INVENTORY_PREVIEW.getKeybind().isKeybindHeld())
        {
            /*
            var inventory = RayTraceUtils.getTargetInventory(mc, true);

            if (inventory != null)
            {
                fi.dy.masa.minihud.renderer.RenderUtils.renderInventoryOverlay(inventory, drawContext);
            }
             */

            InventoryOverlayHandler.getInstance().getRenderContext(drawContext, profiler, mc);

            // OG method (Works with Crafters also)
            //fi.dy.masa.minihud.renderer.RenderUtils.renderInventoryOverlay(mc, drawContext);
        }
    }

    @Override
    public void onRenderWorldPreWeather(Framebuffer fb, Matrix4f posMatrix, Matrix4f projMatrix, Frustum frustum, Camera camera, BufferBuilderStorage buffers, Profiler profiler)
    {
//        if (Configs.Generic.MAIN_RENDERING_TOGGLE.getBooleanValue() &&
//            this.mc.world != null && this.mc.player != null && this.mc.options.hudHidden == false)
//        {
//            OverlayRenderer.renderOverlays(posMatrix, projMatrix, this.mc, frustum, camera, fog, profiler);
//        }
    }

    @Override
    public void onRenderWorldLastAdvanced(Framebuffer fb, Matrix4f posMatrix, Matrix4f projMatrix, Frustum frustum, Camera camera, BufferBuilderStorage buffers, Profiler profiler)
    {
        if (Configs.Generic.MAIN_RENDERING_TOGGLE.getBooleanValue() &&
            this.mc.world != null && this.mc.player != null && this.mc.options.hudHidden == false)
        {
            OverlayRenderer.renderOverlays(posMatrix, projMatrix, this.mc, frustum, camera, profiler);
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
                RenderUtils.renderMapPreview(drawContext, stack, x, y, Configs.Generic.MAP_PREVIEW_SIZE.getIntegerValue(), false);
            }
        }
        else if (stack.getComponents().contains(DataComponentTypes.CONTAINER) && InventoryUtils.shulkerBoxHasItems(stack))
        {
            if (Configs.Generic.SHULKER_BOX_PREVIEW.getBooleanValue() &&
               (Configs.Generic.SHULKER_DISPLAY_REQUIRE_SHIFT.getBooleanValue() == false || GuiBase.isShiftDown()))
            {
                RenderUtils.renderShulkerBoxPreview(drawContext, stack, x, y, Configs.Generic.SHULKER_DISPLAY_BACKGROUND_COLOR.getBooleanValue());
            }
        }
        else if (stack.isOf(Items.ENDER_CHEST) && Configs.Generic.SHULKER_DISPLAY_ENDER_CHEST.getBooleanValue())
        {
            if (Configs.Generic.SHULKER_BOX_PREVIEW.getBooleanValue() &&
                (Configs.Generic.SHULKER_DISPLAY_REQUIRE_SHIFT.getBooleanValue() == false || GuiBase.isShiftDown()))
            {
                World world = WorldUtils.getBestWorld(this.mc);
                PlayerEntity player = world.getPlayerByUuid(this.mc.player.getUuid());

                if (player != null)
                {
                    Pair<Entity, NbtCompound> pair = EntitiesDataManager.getInstance().requestEntity(world, player.getId());
                    EnderChestInventory inv;

                    if (pair != null && pair.getRight() != null && pair.getRight().contains(NbtKeys.ENDER_ITEMS))
                    {
                        inv = InventoryUtils.getPlayerEnderItemsFromNbt(pair.getRight(), world.getRegistryManager());
                        this.lastEnderItems = pair;
                    }
                    else if (pair != null && pair.getLeft() instanceof PlayerEntity pe && !pe.getEnderChestInventory().isEmpty())
                    {
                        inv = pe.getEnderChestInventory();
                    }
                    else if (this.lastEnderItems != null)
                    {
                        inv = InventoryUtils.getPlayerEnderItemsFromNbt(this.lastEnderItems.getRight(), world.getRegistryManager());
                    }
                    else
                    {
                        // Last Ditch effort
                        inv = player.getEnderChestInventory();
                    }

                    if (inv != null)
                    {
                        try (NbtInventory nbtInv = NbtInventory.fromInventory(inv))
                        {
                            NbtCompound nbt = new NbtCompound();
                            NbtList list = nbtInv.toNbtList(world.getRegistryManager());

                            nbt.put(NbtKeys.ENDER_ITEMS, list);
                            fi.dy.masa.malilib.render.RenderUtils.renderNbtItemsPreview(drawContext, stack, nbt, x, y, false);
                        }
                        catch (Exception ignored) { }
                    }
                }
            }
        }
        else if (stack.getComponents().contains(DataComponentTypes.BUNDLE_CONTENTS) && InventoryUtils.bundleHasItems(stack))
        {
            if (Configs.Generic.BUNDLE_PREVIEW.getBooleanValue() &&
                (Configs.Generic.BUNDLE_DISPLAY_REQUIRE_SHIFT.getBooleanValue() == false || GuiBase.isShiftDown()))
            {
                RenderUtils.renderBundlePreview(drawContext, stack, x, y, Configs.Generic.BUNDLE_DISPLAY_ROW_WIDTH.getIntegerValue(), Configs.Generic.BUNDLE_DISPLAY_BACKGROUND_COLOR.getBooleanValue());
            }
        }
    }

    @Override
    public Supplier<String> getProfilerSectionSupplier()
    {
        return () -> Reference.MOD_ID+"_renderer";
    }

    @Override
    public void onRenderTooltipComponentInsertFirst(Item.TooltipContext context, ItemStack stack, Consumer<Text> list)
    {
        // NO-OP
    }

    @Override
    public void onRenderTooltipComponentInsertMiddle(Item.TooltipContext context, ItemStack stack, Consumer<Text> list)
    {
        if (Configs.Generic.BUNDLE_TOOLTIPS.getBooleanValue() &&
            stack.getItem() instanceof BundleItem)
        {
            MiscUtils.addBundleTooltip(stack, list);
        }
    }

    @Override
    public void onRenderTooltipComponentInsertLast(Item.TooltipContext context, ItemStack stack, Consumer<Text> list)
    {
        if (Configs.Generic.AXOLOTL_TOOLTIPS.getBooleanValue() &&
            stack.getItem() == Items.AXOLOTL_BUCKET)
        {
            MiscUtils.addAxolotlTooltip(stack, list);
        }

        if (Configs.Generic.BEE_TOOLTIPS.getBooleanValue() &&
            //stack.getItem() instanceof BlockItem blockItem &&
            //blockItem.getBlock() instanceof BeehiveBlock)
            stack.contains(DataComponentTypes.BEES))
        {
            MiscUtils.addBeeTooltip(stack, list);
        }

        if (Configs.Generic.CUSTOM_MODEL_TOOLTIPS.getBooleanValue() &&
            stack.contains(DataComponentTypes.CUSTOM_MODEL_DATA))
        {
            MiscUtils.addCustomModelTooltip(stack, list);
        }

        if (Configs.Generic.FOOD_TOOLTIPS.getBooleanValue() &&
            stack.contains(DataComponentTypes.FOOD))
        {
            MiscUtils.addFoodTooltip(stack, list);
        }

        if (Configs.Generic.HONEY_TOOLTIPS.getBooleanValue() &&
            stack.getItem() instanceof BlockItem blockItem &&
            blockItem.getBlock() instanceof BeehiveBlock)
        {
            MiscUtils.addHoneyTooltip(stack, list);
        }

        if (Configs.Generic.LODESTONE_TOOLTIPS.getBooleanValue() &&
            stack.contains(DataComponentTypes.LODESTONE_TRACKER))
        {
            MiscUtils.addLodestoneTooltip(stack, list);
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

//        if (this.chunkFutures.size() >= 4)
//        {
//            this.resetCachedChunks();
//        }

        InfoLineChunkCache.INSTANCE.onUpdate();

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

    private void processEntries(List<InfoLine.Entry> list)
    {
        if (list == null || list.isEmpty())
        {
            return;
        }

        for (InfoLine.Entry entry : list)
        {
            if (!entry.isEmpty())
            {
                if (entry.isTranslated())
                {
                    this.addLine(entry.format());
                }
                else if (entry.hasArgs())
                {
                    this.addLineI18n(entry.format(), entry.args());
                }
                else
                {
                    this.addLineI18n(entry.format());
                }
            }
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
        World world = entity.getWorld();
        double y = entity.getY();
        BlockPos pos = BlockPos.ofFloored(entity.getX(), y, entity.getZ());
        ChunkPos chunkPos = new ChunkPos(pos);

        @SuppressWarnings("deprecation")
        boolean isChunkLoaded = mc.world.isChunkLoaded(pos);
        
        SpeedUnits speedUnits = (SpeedUnits) Configs.Generic.SPEED_UNITS.getOptionListValue();

        if (isChunkLoaded == false)
        {
            return;
        }

        if (type == InfoToggle.FPS)
        {
            // Make into a generic call
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                InfoLine.Context ctx = new InfoLine.Context(null, null, null, null, null, null);
                this.processEntries(parser.parse(ctx));
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.MEMORY_USAGE)
        {
            // Make into a generic call
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                InfoLine.Context ctx = new InfoLine.Context(null, null, null, null, null, null);
                this.processEntries(parser.parse(ctx));
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.TIME_REAL)
        {
            // Make into a generic call
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                InfoLine.Context ctx = new InfoLine.Context(null, null, null, null, null, null);
                this.processEntries(parser.parse(ctx));
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.TIME_WORLD)
        {
            // Make into a generic call
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                InfoLine.Context ctx = new InfoLine.Context(world, null, null, null, null, null);
                this.processEntries(parser.parse(ctx));
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.TIME_WORLD_FORMATTED)
        {
            // Make into a generic call
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                InfoLine.Context ctx = new InfoLine.Context(world, null, null, null, null, null);
                this.processEntries(parser.parse(ctx));
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.TIME_DAY_MODULO)
        {
            // Make into a generic call
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                InfoLine.Context ctx = new InfoLine.Context(world, null, null, null, null, null);
                this.processEntries(parser.parse(ctx));
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.TIME_TOTAL_MODULO)
        {
            // Make into a generic call
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                InfoLine.Context ctx = new InfoLine.Context(world, null, null, null, null, null);
                this.processEntries(parser.parse(ctx));
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.SERVER_TPS)
        {
            if (this.addedTypes.contains(type))
            {
                return;
            }

            // Make into a generic call
            World bestWorld = WorldUtils.getBestWorld(mc);
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                InfoLine.Context ctx = new InfoLine.Context(bestWorld, null, null, null, null, null);
                this.processEntries(parser.parse(ctx));

                if (parser.succeededType())
                {
                    this.addedTypes.add(type);
                }
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.SERVUX)
        {
            if (this.addedTypes.contains(type))
            {
                return;
            }

            // Make into a generic call
            World bestWorld = WorldUtils.getBestWorld(mc);
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                InfoLine.Context ctx = new InfoLine.Context(bestWorld, null, null, null, null, null);
                this.processEntries(parser.parse(ctx));

                if (parser.succeededType())
                {
                    this.addedTypes.add(type);
                }
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.WEATHER)
        {
            if (this.addedTypes.contains(type))
            {
                return;
            }

            // Make into a generic call
            World bestWorld = WorldUtils.getBestWorld(mc);
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                InfoLine.Context ctx = new InfoLine.Context(bestWorld, null, null, null, null, null);
                this.processEntries(parser.parse(ctx));

                if (parser.succeededType())
                {
                    this.addedTypes.add(type);
                }
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.MOB_CAPS)
        {
            if (this.addedTypes.contains(type))
            {
                return;
            }

            // Make into a generic call
            World bestWorld = WorldUtils.getBestWorld(mc);
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                InfoLine.Context ctx = new InfoLine.Context(bestWorld, null, null, null, null, null);
                this.processEntries(parser.parse(ctx));

                if (parser.succeededType())
                {
                    this.addedTypes.add(type);
                }
            }
            else
            {
                return;
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
            String facingName = StringUtils.translate("minihud.info_line.facing." + facing.name().toLowerCase() + ".name");
            String str;

            if (facingName.contains("minihud.info_line.facing." + facing.name().toLowerCase() + ".name"))
            {
                facingName = facing.name().toLowerCase();
                str = StringUtils.translate("minihud.info_line.invalid_value");
            }
            else
            {
                str = StringUtils.translate("minihud.info_line.facing." + facing.name().toLowerCase());
            }

            this.addLineI18n("minihud.info_line.facing", facingName, str);
        }
        else if (type == InfoToggle.LIGHT_LEVEL)
        {
//            WorldChunk clientChunk = this.getClientChunk(chunkPos);
            WorldChunk clientChunk = InfoLineChunkCache.INSTANCE.getClientChunk(chunkPos);

            if (clientChunk.isEmpty() == false)
            {
                LightingProvider lightingProvider = world.getChunkManager().getLightingProvider();

                this.addLineI18n("minihud.info_line.light_level", lightingProvider.get(LightType.BLOCK).getLightLevel(pos));
            }
        }
        else if (type == InfoToggle.BEE_COUNT)
        {
            // Make into a generic call
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                World bestWorld = WorldUtils.getBestWorld(mc);
                Pair<BlockEntity, NbtCompound> pair = this.getTargetedBlockEntity(bestWorld, mc);

                if (pair != null)
                {
                    InfoLine.Context ctx = new InfoLine.Context(bestWorld, null, pair.getLeft(), null, null, pair.getRight());
                    this.processEntries(parser.parse(ctx));

                    if (parser.succeededType())
                    {
                        this.addedTypes.add(type);
                    }
                }
                else
                {
                    return;
                }
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.COMPARATOR_OUTPUT)
        {
            // Make into a generic call
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                World bestWorld = WorldUtils.getBestWorld(mc);
                Pair<BlockEntity, NbtCompound> pair = this.getTargetedBlockEntity(bestWorld, mc);

                if (pair != null)
                {
                    InfoLine.Context ctx = new InfoLine.Context(bestWorld, null, pair.getLeft(), null, null, pair.getRight());
                    this.processEntries(parser.parse(ctx));

                    if (parser.succeededType())
                    {
                        this.addedTypes.add(type);
                    }
                    else
                    {
                        return;
                    }
                }
                else
                {
                    return;
                }
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.HONEY_LEVEL)
        {
            // Make into a generic call
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                BlockState state = this.getTargetedBlock(mc);

                if (state != null)
                {
                    InfoLine.Context ctx = new InfoLine.Context(world, null, null, null, state, null);
                    this.processEntries(parser.parse(ctx));

                    if (parser.succeededType())
                    {
                        this.addedTypes.add(type);
                    }
                }
                else
                {
                    return;
                }
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.FURNACE_XP)
        {
            // Make into a generic call
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                World bestWorld = WorldUtils.getBestWorld(mc);
                Pair<BlockEntity, NbtCompound> pair = this.getTargetedBlockEntity(bestWorld, mc);

                if (pair != null)
                {
                    InfoLine.Context ctx = new InfoLine.Context(bestWorld, null, pair.getLeft(), null, null, pair.getRight());
                    this.processEntries(parser.parse(ctx));

                    if (parser.succeededType())
                    {
                        this.addedTypes.add(type);
                    }
                }
                else
                {
                    return;
                }
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.HORSE_SPEED ||
                 type == InfoToggle.HORSE_JUMP)
        {
            if (this.addedTypes.contains(type))
            {
                return;
            }

            // Make into a generic call
            World bestWorld = WorldUtils.getBestWorld(mc);
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                Pair<Entity, NbtCompound> pair = this.getTargetEntity(bestWorld, mc);
                InfoLine.Context ctx;

                if (mc.player.hasVehicle() && pair == null)
                {
                    ctx = new InfoLine.Context(bestWorld, mc.player.getVehicle(), null, null, null, null);
                }
                else if (pair != null)
                {
                    ctx = new InfoLine.Context(bestWorld, pair.getLeft(), null, null, null, pair.getRight());
                }
                else
                {
                    return;
                }

                this.processEntries(parser.parse(ctx));

                if (parser.succeededType())
                {
                    this.addedTypes.add(type);
                }
            }
            else
            {
                return;
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
                str.append(pre).append(
                    StringUtils.translate("minihud.info_line.speed_" + speedUnits.suffix,
                        speedUnits.convert(dist * 20)));
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
            this.addLineI18n("minihud.info_line.speed_hv_" + speedUnits.suffix, 
                speedUnits.convert(Math.sqrt(dx * dx + dz * dz) * 20),
                speedUnits.convert(dy * 20));
        }
        else if (type == InfoToggle.SPEED_AXIS)
        {
            double dx = entity.getX() - entity.lastRenderX;
            double dy = entity.getY() - entity.lastRenderY;
            double dz = entity.getZ() - entity.lastRenderZ;
            this.addLineI18n("minihud.info_line.speed_axis_" + speedUnits.suffix, 
                speedUnits.convert(dx * 20),
                speedUnits.convert(dy * 20),
                speedUnits.convert(dz * 20));
        }
        else if (type == InfoToggle.CHUNK_SECTIONS)
        {
            // Make into a generic call
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                InfoLine.Context ctx = new InfoLine.Context(null, null, null, null, null, null);
                this.processEntries(parser.parse(ctx));
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.CHUNK_SECTIONS_FULL)
        {
            // Make into a generic call
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                InfoLine.Context ctx = new InfoLine.Context(null, null, null, null, null, null);
                this.processEntries(parser.parse(ctx));
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.CHUNK_UPDATES)
        {
            // Make into a generic call
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                InfoLine.Context ctx = new InfoLine.Context(null, null, null, null, null, null);
                this.processEntries(parser.parse(ctx));
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.LOADED_CHUNKS_COUNT)
        {
            if (this.addedTypes.contains(type))
            {
                return;
            }

            // Make into a generic call
            World bestWorld = WorldUtils.getBestWorld(mc);
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                InfoLine.Context ctx = new InfoLine.Context(bestWorld, null, null, null, null, null);
                this.processEntries(parser.parse(ctx));

                if (parser.succeededType())
                {
                    this.addedTypes.add(type);
                }
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.PANDA_GENE)
        {
            // Make into a generic call
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                Pair<Entity, NbtCompound> pair = this.getTargetEntity(world, mc);

                if (pair != null)
                {
                    InfoLine.Context ctx = new InfoLine.Context(world, pair.getLeft(), null, null, null, pair.getRight());
                    this.processEntries(parser.parse(ctx));

                    if (parser.succeededType())
                    {
                        this.addedTypes.add(type);
                    }
                }
                else
                {
                    return;
                }
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.PARTICLE_COUNT)
        {
            // Make into a generic call
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                InfoLine.Context ctx = new InfoLine.Context(null, null, null, null, null, null);
                this.processEntries(parser.parse(ctx));
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.DIFFICULTY)
        {
            // Make into a generic call
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                InfoLine.Context ctx = new InfoLine.Context(world, null, null, pos, null, null);
                this.processEntries(parser.parse(ctx));
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.BIOME)
        {
//            WorldChunk clientChunk = this.getClientChunk(chunkPos);
            WorldChunk clientChunk = InfoLineChunkCache.INSTANCE.getClientChunk(chunkPos);

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
//            WorldChunk clientChunk = this.getClientChunk(chunkPos);
            WorldChunk clientChunk = InfoLineChunkCache.INSTANCE.getClientChunk(chunkPos);

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
            // Make into a generic call
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                InfoLine.Context ctx = new InfoLine.Context(world, null, null, pos, null, null);
                this.processEntries(parser.parse(ctx));
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.LOOKING_AT_ENTITY)
        {
            // Make into a generic call
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                Pair<Entity, NbtCompound> pair = this.getTargetEntity(world, mc);

                if (pair != null)
                {
                    InfoLine.Context ctx = new InfoLine.Context(world, pair.getLeft(), null, null, null, pair.getRight());
                    this.processEntries(parser.parse(ctx));

                    if (parser.succeededType())
                    {
                        this.addedTypes.add(type);
                    }
                }
                else
                {
                    return;
                }
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.ENTITY_VARIANT)
        {
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                Pair<Entity, NbtCompound> pair = this.getTargetEntity(world, mc);

                if (pair != null)
                {
                    InfoLine.Context ctx = new InfoLine.Context(world, pair.getLeft(), null, null, null, pair.getRight());
                    this.processEntries(parser.parse(ctx));

                    if (parser.succeededType())
                    {
                        this.addedTypes.add(type);
                    }
                }
                else
                {
                    return;
                }
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.ENTITY_HOME_POS)
        {
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                Pair<Entity, NbtCompound> pair = this.getTargetEntity(world, mc);

                if (pair != null)
                {
                    InfoLine.Context ctx = new InfoLine.Context(world, pair.getLeft(), null, null, null, pair.getRight());
                    this.processEntries(parser.parse(ctx));

                    if (parser.succeededType())
                    {
                        this.addedTypes.add(type);
                    }
                }
                else
                {
                    return;
                }
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.LOOKING_AT_EFFECTS)
        {
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                Pair<Entity, NbtCompound> pair = this.getTargetEntity(world, mc);

                if (pair != null)
                {
                    InfoLine.Context ctx = new InfoLine.Context(world, pair.getLeft(), null, null, null, pair.getRight());
                    this.processEntries(parser.parse(ctx));

                    if (parser.succeededType())
                    {
                        this.addedTypes.add(type);
                    }
                }
                else
                {
                    return;
                }
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.ZOMBIE_CONVERSION)
        {
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                Pair<Entity, NbtCompound> pair = this.getTargetEntity(world, mc);

                if (pair != null)
                {
                    InfoLine.Context ctx = new InfoLine.Context(world, pair.getLeft(), null, null, null, pair.getRight());
                    this.processEntries(parser.parse(ctx));

                    if (parser.succeededType())
                    {
                        this.addedTypes.add(type);
                    }
                }
                else
                {
                    return;
                }
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.DOLPHIN_TREASURE)
        {
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                Pair<Entity, NbtCompound> pair = this.getTargetEntity(world, mc);

                if (pair != null)
                {
                    InfoLine.Context ctx = new InfoLine.Context(world, pair.getLeft(), null, null, null, pair.getRight());
                    this.processEntries(parser.parse(ctx));

                    if (parser.succeededType())
                    {
                        this.addedTypes.add(type);
                    }
                }
                else
                {
                    return;
                }
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.ENTITY_REG_NAME)
        {
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                Pair<Entity, NbtCompound> pair = this.getTargetEntity(world, mc);

                if (pair != null)
                {
                    InfoLine.Context ctx = new InfoLine.Context(world, pair.getLeft(), null, null, null, pair.getRight());
                    this.processEntries(parser.parse(ctx));

                    if (parser.succeededType())
                    {
                        this.addedTypes.add(type);
                    }
                }
                else
                {
                    return;
                }
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.PLAYER_EXPERIENCE)
        {
            if (mc.player != null)
            {
                this.addLineI18n("minihud.info_line.player_experience", mc.player.experienceLevel, 100 * mc.player.experienceProgress, mc.player.totalExperience);
            }
        }
        else if (type == InfoToggle.LOOKING_AT_PLAYER_EXP)
        {
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                Pair<Entity, NbtCompound> pair = this.getTargetEntity(world, mc);

                if (pair != null)
                {
                    InfoLine.Context ctx = new InfoLine.Context(world, pair.getLeft(), null, null, null, pair.getRight());
                    this.processEntries(parser.parse(ctx));

                    if (parser.succeededType())
                    {
                        this.addedTypes.add(type);
                    }
                }
                else
                {
                    return;
                }
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.LOOKING_AT_BLOCK ||
                 type == InfoToggle.LOOKING_AT_BLOCK_CHUNK)
        {
            if (this.addedTypes.contains(type))
            {
                return;
            }

            // Make into a generic call
            World bestWorld = WorldUtils.getBestWorld(mc);
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                BlockState state = this.getTargetedBlock(mc);

                if (state != null)
                {
                    BlockPos lookPos = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
                    InfoLine.Context ctx = new InfoLine.Context(bestWorld, null, null, lookPos, state, null);
                    this.processEntries(parser.parse(ctx));

                    if (parser.succeededType())
                    {
                        this.addedTypes.add(type);
                    }
                }
                else
                {
                    return;
                }
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.BLOCK_PROPS)
        {
            if (this.addedTypes.contains(type))
            {
                return;
            }

            // Make into a generic call
            World bestWorld = WorldUtils.getBestWorld(mc);
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                BlockState state = this.getTargetedBlock(mc);

                if (state != null)
                {
                    BlockPos lookPos = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
                    InfoLine.Context ctx = new InfoLine.Context(bestWorld, null, null, lookPos, state, null);
                    this.processEntries(parser.parse(ctx));

                    if (parser.succeededType())
                    {
                        this.addedTypes.add(type);
                    }
                }
                else
                {
                    return;
                }
            }
            else
            {
                return;
            }
        }
    }

    private boolean isEntityDataValid(@Nonnull NbtCompound nbt)
    {
        // Has a valid Inventory = ServerWorld
        if (InventoryUtils.hasNbtItems(nbt))
        {
            return true;
        }

        for (String key : nbt.getKeys())
        {
            switch (key)
            {
                // If `Fire == 0` instead of `-1` means it's ClientWorld; it's ridiculous, but it works.
                case NbtKeys.FIRE ->
                {
                    int fire = nbt.getShort(NbtKeys.FIRE, (short) -1);

                    if (fire < 0 || fire > 0)
                    {
                        return true;
                    }
                }
                // If `Age == -1 or 1 instead of 0 or > 1 it's ClientWorld; it's ridiculous, but it works.
                case NbtKeys.AGE ->
                {
                    int age = nbt.getInt(NbtKeys.AGE, -1);

                    if (age == 0 || age > 1)
                    {
                        return true;
                    }
                }
                // Has a Brain besides the default = ServerWorld
                case NbtKeys.BRAIN ->
                {
                    NbtCompound tag = nbt.getCompoundOrEmpty(NbtKeys.BRAIN);

                    if (!tag.isEmpty() && !tag.getCompound(NbtKeys.MEMORIES).isEmpty())
                    {
                        return true;
                    }
                }
                case NbtKeys.OFFERS -> { return true; }
                case NbtKeys.TRADE_RECIPES -> { return true; }
                case NbtKeys.ZOMBIE_CONVERSION ->
                {
                    if (nbt.getInt(NbtKeys.ZOMBIE_CONVERSION, -1) > 0)
                    {
                        return true;
                    }
                }
                case NbtKeys.DROWNED_CONVERSION ->
                {
                    if (nbt.getInt(NbtKeys.DROWNED_CONVERSION, -1) > 0)
                    {
                        return true;
                    }
                }
                case NbtKeys.STRAY_CONVERSION ->
                {
                    if (nbt.getInt(NbtKeys.STRAY_CONVERSION, -1) > 0)
                    {
                        return true;
                    }
                }
                case NbtKeys.CONVERSION_PLAYER -> { return true; }
                case NbtKeys.RECIPE_BOOK -> { return true; }
                case NbtKeys.RECIPES -> { return true; }
                //case NbtKeys.SADDLE -> { return true; }
                case NbtKeys.EFFECTS -> { return true; }
            }
        }

        return false;
    }

    @Nullable
    public Pair<Entity, NbtCompound> getTargetEntity(World world, MinecraftClient mc)
    {
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY)
        {
            Entity lookedEntity = ((EntityHitResult) mc.crosshairTarget).getEntity();

            // Don't return the player entity (Apparently this is a thing in modern Minecraft versions)
            if (lookedEntity == null || lookedEntity.getId() == mc.player.getId())
            {
                return null;
            }

            World bestWorld = WorldUtils.getBestWorld(mc);
            Pair<Entity, NbtCompound> pair = null;

            if (bestWorld instanceof ServerWorld serverWorld)
            {
                Entity serverEntity = serverWorld.getEntityById(lookedEntity.getId());
//                NbtView view = NbtView.getWriter(bestWorld.getRegistryManager());
//                serverEntity.writeData(view.getWriter());
//                NbtCompound nbt = view.readNbt();
//                Identifier id = EntityType.getId(serverEntity.getType());
                NbtCompound nbt = NbtEntityUtils.invokeEntityNbtDataNoPassengers(serverEntity, lookedEntity.getId());

//                if (nbt != null && id != null)
                if (!nbt.isEmpty())
                {
//                    nbt.putString("id", id.toString());
                    pair = Pair.of(serverEntity, nbt);
                }
            }
            else
            {
                pair = EntitiesDataManager.getInstance().requestEntity(world, lookedEntity.getId());
            }

            // Remember the last entity so the "refresh time" is smoothed over.
            if (pair == null && this.lastEntity != null &&
                this.lastEntity.getLeft().getId() == lookedEntity.getId())
            {
                pair = this.lastEntity;
            }
            else if (pair != null && pair.getRight() != null &&
                    !pair.getRight().isEmpty() &&
                     this.isEntityDataValid(pair.getRight()))
            {
                this.lastEntity = pair;
            }
            else if (this.lastEntity != null &&
                    this.lastEntity.getLeft().getId() == lookedEntity.getId())
            {
                pair = this.lastEntity;
            }

            return pair;
        }

        return null;
    }

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

    // # Moved to InfoLineChunkCache
//    @Nullable
//    private WorldChunk getChunk(ChunkPos chunkPos)
//    {
//        CompletableFuture<OptionalChunk<Chunk>> future = this.chunkFutures.get(chunkPos);
//
//        if (future == null)
//        {
//            future = this.setupChunkFuture(chunkPos);
//        }
//
//        OptionalChunk<Chunk> chunkResult = future.getNow(null);
//        if (chunkResult == null)
//        {
//            return null;
//        }
//        else
//        {
//            Chunk chunk = chunkResult.orElse(null);
//            if (chunk instanceof WorldChunk)
//            {
//                return (WorldChunk) chunk;
//            }
//            else
//            {
//                return null;
//            }
//        }
//    }

//    private CompletableFuture<OptionalChunk<Chunk>> setupChunkFuture(ChunkPos chunkPos)
//    {
//        IntegratedServer server = this.getDataStorage().getIntegratedServer();
//        CompletableFuture<OptionalChunk<Chunk>> future = null;
//
//        if (server != null)
//        {
//            ServerWorld world = server.getWorld(this.mc.world.getRegistryKey());
//
//            if (world != null)
//            {
//                future = world.getChunkManager().getChunkFutureSyncOnMainThread(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false)
//                        .thenApply((either) -> either.map((chunk) -> (WorldChunk) chunk) );
//            }
//        }
//
//        if (future == null)
//        {
//            future = CompletableFuture.completedFuture(OptionalChunk.of(this.getClientChunk(chunkPos)));
//        }
//
//        this.chunkFutures.put(chunkPos, future);
//
//        return future;
//    }

//    private WorldChunk getClientChunk(ChunkPos chunkPos)
//    {
//        if (this.cachedClientChunk == null || this.cachedClientChunk.getPos().equals(chunkPos) == false)
//        {
//            this.cachedClientChunk = this.mc.world.getChunk(chunkPos.x, chunkPos.z);
//        }
//
//        return this.cachedClientChunk;
//    }

//    private void resetCachedChunks()
//    {
//        this.chunkFutures.clear();
//        this.cachedClientChunk = null;
//    }

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
