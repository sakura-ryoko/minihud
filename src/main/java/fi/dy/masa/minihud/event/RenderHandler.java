package fi.dy.masa.minihud.event;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.WorldUtils;
import fi.dy.masa.malilib.util.data.Constants;
import fi.dy.masa.malilib.util.data.DataEntityUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.ListData;
import fi.dy.masa.malilib.util.data.tag.converter.DataConverterNbt;
import fi.dy.masa.malilib.util.game.BlockUtils;
import fi.dy.masa.malilib.util.nbt.NbtInventory;
import fi.dy.masa.malilib.util.nbt.NbtKeys;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.data.DebugDataManager;
import fi.dy.masa.minihud.data.EntitiesDataManager;
import fi.dy.masa.minihud.data.HudDataManager;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineChunkCache;
import fi.dy.masa.minihud.info.InfoLineContext;
import fi.dy.masa.minihud.renderer.InventoryOverlayHandler;
import fi.dy.masa.minihud.renderer.OverlayRenderer;
import fi.dy.masa.minihud.util.DataStorage;
import fi.dy.masa.minihud.util.MiscUtils;
import fi.dy.masa.minihud.util.SpeedUnits;

public class RenderHandler implements IRenderer
{
    private static final RenderHandler INSTANCE = new RenderHandler();

    private final Minecraft mc;
    private final DataStorage data;
    private final HudDataManager hudData;
    private final Date date;
//    private final Map<ChunkPos, CompletableFuture<OptionalChunk<Chunk>>> chunkFutures = new HashMap<>();
    private final Set<InfoToggle> addedTypes = new HashSet<>();
//    @Nullable private WorldChunk cachedClientChunk;
    private long infoUpdateTime;

    private final List<StringHolder> lineWrappers = new ArrayList<>();
    private final List<String> lines = new ArrayList<>();
    private Pair<BlockEntity, CompoundData> lastBlockEntity = null;
    private Pair<Entity, CompoundData> lastEntity = null;
    private Pair<Entity, CompoundData> lastEnderItems = null;

    public RenderHandler()
    {
        this.mc = Minecraft.getInstance();
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
    public void onRenderGameOverlayPostAdvanced(GuiContext ctx, float partialTicks, ProfilerFiller profiler)
    {
        if (Configs.Generic.MAIN_RENDERING_TOGGLE.getBooleanValue() == false)
        {
//            this.resetCachedChunks();
            InfoLineChunkCache.INSTANCE.onReset();
            return;
        }

		if (DebugDataManager.getInstance().shouldShowDebugHudFix() == false &&
//        if (mc.getDebugHud().shouldShowDebugHud() == false &&
            mc.player != null && mc.options.hideGui == false &&
            (Configs.Generic.REQUIRE_SNEAK.getBooleanValue() == false || mc.player.isShiftKeyDown()) &&
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

            RenderUtils.renderText(ctx, x, y, Configs.Generic.FONT_SCALE.getDoubleValue(), textColor, bgColor, alignment,
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

            InventoryOverlayHandler.getInstance().getRenderContext(ctx, profiler);

            // OG method (Works with Crafters also)
            //fi.dy.masa.minihud.renderer.RenderUtils.renderInventoryOverlay(mc, drawContext);
        }
    }

    @Override
    public void onRenderWorldPreWeather(RenderTarget fb, Matrix4f posMatrix, Matrix4f projMatrix, Frustum frustum, Camera camera, RenderBuffers buffers, ProfilerFiller profiler)
    {
//        if (Configs.Generic.MAIN_RENDERING_TOGGLE.getBooleanValue() &&
//            this.mc.world != null && this.mc.player != null && this.mc.options.hudHidden == false)
//        {
//            OverlayRenderer.renderOverlays(posMatrix, projMatrix, this.mc, frustum, camera, fog, profiler);
//        }
    }

    @Override
    public void onRenderWorldLastAdvanced(RenderTarget fb, Matrix4f posMatrix, Matrix4f projMatrix, Frustum frustum, Camera camera, RenderBuffers buffers, ProfilerFiller profiler)
    {
        if (Configs.Generic.MAIN_RENDERING_TOGGLE.getBooleanValue() &&
            this.mc.level != null && this.mc.player != null && this.mc.options.hideGui == false)
        {
            OverlayRenderer.renderOverlays(posMatrix, projMatrix, this.mc, frustum, camera, profiler);
        }
    }

    @Override
    public void onRenderTooltipLast(GuiContext ctx, ItemStack stack, int x, int y)
    {
        Item item = stack.getItem();
        if (item instanceof MapItem)
        {
            if (Configs.Generic.MAP_PREVIEW.getBooleanValue() &&
               (Configs.Generic.MAP_PREVIEW_REQUIRE_SHIFT.getBooleanValue() == false || GuiBase.isShiftDown()))
            {
                RenderUtils.renderMapPreview(ctx, stack, x, y, Configs.Generic.MAP_PREVIEW_SIZE.getIntegerValue(), false);
            }
        }
        else if (stack.getComponents().has(DataComponents.CONTAINER) && InventoryUtils.shulkerBoxHasItems(stack))
        {
            if (Configs.Generic.SHULKER_BOX_PREVIEW.getBooleanValue() &&
               (Configs.Generic.SHULKER_DISPLAY_REQUIRE_SHIFT.getBooleanValue() == false || GuiBase.isShiftDown()))
            {
                RenderUtils.renderShulkerBoxPreview(ctx, stack, x, y, Configs.Generic.SHULKER_DISPLAY_BACKGROUND_COLOR.getBooleanValue());
            }
        }
        else if (stack.is(Items.ENDER_CHEST) && Configs.Generic.SHULKER_DISPLAY_ENDER_CHEST.getBooleanValue())
        {
            if (Configs.Generic.SHULKER_BOX_PREVIEW.getBooleanValue() &&
                (Configs.Generic.SHULKER_DISPLAY_REQUIRE_SHIFT.getBooleanValue() == false || GuiBase.isShiftDown()))
            {
                Level world = WorldUtils.getBestWorld(this.mc);
                Player player = world.getPlayerByUUID(this.mc.player.getUUID());

                if (player != null)
                {
                    Pair<Entity, CompoundData> pair = EntitiesDataManager.getInstance().requestEntity(world, player.getId());
                    PlayerEnderChestContainer inv;

                    if (pair != null && pair.getRight() != null && pair.getRight().contains(NbtKeys.ENDER_ITEMS, Constants.NBT.TAG_LIST))
                    {
                        inv = InventoryUtils.getPlayerEnderItemsFromData(pair.getRight(), world.registryAccess());
                        this.lastEnderItems = pair;
                    }
                    else if (pair != null && pair.getLeft() instanceof Player pe && !pe.getEnderChestInventory().isEmpty())
                    {
                        inv = pe.getEnderChestInventory();
                    }
                    else if (this.lastEnderItems != null)
                    {
                        inv = InventoryUtils.getPlayerEnderItemsFromData(this.lastEnderItems.getRight(), world.registryAccess());
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
                            CompoundData data = new CompoundData();
                            ListData list = nbtInv.toDataList(world.registryAccess());

                            data.put(NbtKeys.ENDER_ITEMS, list);
                            fi.dy.masa.malilib.render.RenderUtils.renderDataItemsPreview(ctx, stack, data, x, y, false);
                        }
                        catch (Exception ignored) { }
                    }
                }
            }
        }
        else if (stack.getComponents().has(DataComponents.BUNDLE_CONTENTS) && InventoryUtils.bundleHasItems(stack))
        {
            if (Configs.Generic.BUNDLE_PREVIEW.getBooleanValue() &&
                (Configs.Generic.BUNDLE_DISPLAY_REQUIRE_SHIFT.getBooleanValue() == false || GuiBase.isShiftDown()))
            {
                RenderUtils.renderBundlePreview(ctx, stack, x, y, Configs.Generic.BUNDLE_DISPLAY_ROW_WIDTH.getIntegerValue(), Configs.Generic.BUNDLE_DISPLAY_BACKGROUND_COLOR.getBooleanValue());
            }
        }
    }

    @Override
    public Supplier<String> getProfilerSectionSupplier()
    {
        return () -> Reference.MOD_ID+"_renderer";
    }

    @Override
    public void onRenderTooltipComponentInsertFirst(Item.TooltipContext context, ItemStack stack, Consumer<Component> list)
    {
        // NO-OP
    }

    @Override
    public void onRenderTooltipComponentInsertMiddle(Item.TooltipContext context, ItemStack stack, Consumer<Component> list)
    {
        if (Configs.Generic.BUNDLE_TOOLTIPS.getBooleanValue() &&
            stack.getItem() instanceof BundleItem)
        {
            MiscUtils.addBundleTooltip(stack, list);
        }
    }

    @Override
    public void onRenderTooltipComponentInsertLast(Item.TooltipContext context, ItemStack stack, Consumer<Component> list)
    {
        if (Configs.Generic.AXOLOTL_TOOLTIPS.getBooleanValue() &&
            stack.getItem() == Items.AXOLOTL_BUCKET)
        {
            MiscUtils.addAxolotlTooltip(stack, list);
        }

        if (Configs.Generic.BEE_TOOLTIPS.getBooleanValue() &&
            //stack.getItem() instanceof BlockItem blockItem &&
            //blockItem.getBlock() instanceof BeehiveBlock)
            stack.has(DataComponents.BEES))
        {
            MiscUtils.addBeeTooltip(stack, list);
        }

        if (Configs.Generic.CUSTOM_MODEL_TOOLTIPS.getBooleanValue() &&
            stack.has(DataComponents.CUSTOM_MODEL_DATA))
        {
            MiscUtils.addCustomModelTooltip(stack, list);
        }

        if (Configs.Generic.FOOD_TOOLTIPS.getBooleanValue() &&
            stack.has(DataComponents.FOOD))
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
            stack.has(DataComponents.LODESTONE_TRACKER))
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

    public void updateData(Minecraft mc)
    {
        if (mc.level != null)
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
        Minecraft mc = this.mc;
        Entity entity = mc.getCameraEntity();
        Level world = entity != null ? entity.level() : null;
		if (world == null || mc.level == null) return;
        double y = entity.getY();
        BlockPos pos = BlockPos.containing(entity.getX(), y, entity.getZ());
        ChunkPos chunkPos = new ChunkPos(pos);

        @SuppressWarnings("deprecation")
        boolean isChunkLoaded = mc.level.hasChunkAt(pos);
        
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
                InfoLineContext ctx = new InfoLineContext(world, null, null, null, null, null, null);
                this.processEntries(parser.parse(ctx));
            }
            else
            {
                return;
            }
        }
//		else if (type == InfoToggle.GPU)
//		{
//			// Make into a generic call
//			InfoLine parser = type.initParser();
//
//			if (parser != null)
//			{
//				InfoLineContext ctx = new InfoLineContext(world, null, null, null, null, null, null);
//				this.processEntries(parser.parse(ctx));
//			}
//			else
//			{
//				return;
//			}
//		}
        else if (type == InfoToggle.MEMORY_USAGE)
        {
            // Make into a generic call
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                InfoLineContext ctx = new InfoLineContext(world, null, null, null, null, null, null);
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
                InfoLineContext ctx = new InfoLineContext(world, null, null, null, null, null, null);
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
                InfoLineContext ctx = new InfoLineContext(world, null, null, null, null, null, null);
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
                InfoLineContext ctx = new InfoLineContext(world, null, null, null, null, null, null);
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
                InfoLineContext ctx = new InfoLineContext(world, null, null, null, null, null, null);
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
                InfoLineContext ctx = new InfoLineContext(world, null, null, null, null, null, null);
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
            Level bestWorld = WorldUtils.getBestWorld(mc);
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                InfoLineContext ctx = new InfoLineContext(bestWorld, null, null, null, null, null, null);
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
            Level bestWorld = WorldUtils.getBestWorld(mc);
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                InfoLineContext ctx = new InfoLineContext(bestWorld, null, null, null, null, null, null);
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
            Level bestWorld = WorldUtils.getBestWorld(mc);
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                InfoLineContext ctx = new InfoLineContext(bestWorld, null, null, null, null, null, null);
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
            Level bestWorld = WorldUtils.getBestWorld(mc);
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                InfoLineContext ctx = new InfoLineContext(bestWorld, null, null, null, null, null, null);
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
	        InfoLine parser = type.initParser();

	        if (parser != null)
	        {
		        InfoLineContext ctx = new InfoLineContext(world, mc.player, null, null, null, null, null);
		        this.processEntries(parser.parse(ctx));
	        }
	        else
	        {
		        return;
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

            InfoLine parser = type.initParser();

            if (parser != null)
            {
                InfoLineContext ctx = new InfoLineContext(world, entity, null, null, null, null, null);
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

	        InfoLine parser = type.initParser();

	        if (parser != null)
	        {
		        InfoLineContext ctx = new InfoLineContext(world, null, null, pos, null, chunkPos, null);
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
        else if (type == InfoToggle.BLOCK_IN_CHUNK)
        {
	        InfoLine parser = type.initParser();

	        if (parser != null)
	        {
//		        BlockPos lookPos = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
		        InfoLineContext ctx = new InfoLineContext(world, null, null, pos, null, chunkPos, null);
		        this.processEntries(parser.parse(ctx));
	        }
	        else
	        {
		        return;
	        }
        }
        else if (type == InfoToggle.BLOCK_BREAK_SPEED)
        {
	        InfoLine parser = type.initParser();

	        if (parser != null)
	        {
		        InfoLineContext ctx = new InfoLineContext(world, null, null, null, null, null, null);
		        this.processEntries(parser.parse(ctx));
	        }
	        else
	        {
		        return;
	        }
        }
        else if (type == InfoToggle.SPRINTING)
        {
	        InfoLine parser = type.initParser();

	        if (parser != null)
	        {
		        InfoLineContext ctx = new InfoLineContext(world, null, null, null, null, null, null);
		        this.processEntries(parser.parse(ctx));
	        }
	        else
	        {
		        return;
	        }
        }
        else if (type == InfoToggle.DISTANCE)
        {
	        InfoLine parser = type.initParser();

	        if (parser != null)
	        {
		        InfoLineContext ctx = new InfoLineContext(world, entity, null, null, null, null, null);
		        this.processEntries(parser.parse(ctx));
	        }
	        else
	        {
		        return;
	        }
        }
        else if (type == InfoToggle.FACING)
        {
	        InfoLine parser = type.initParser();

	        if (parser != null)
	        {
		        InfoLineContext ctx = new InfoLineContext(world, entity, null, null, null, null, null);
		        this.processEntries(parser.parse(ctx));
	        }
	        else
	        {
		        return;
	        }
        }
        else if (type == InfoToggle.LIGHT_LEVEL)
        {
	        InfoLine parser = type.initParser();

	        if (parser != null)
	        {
		        InfoLineContext ctx = new InfoLineContext(world, null, null, pos, null, chunkPos, null);
		        this.processEntries(parser.parse(ctx));
	        }
	        else
	        {
		        return;
	        }
        }
        else if (type == InfoToggle.BEE_COUNT)
        {
            // Make into a generic call
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                Level bestWorld = WorldUtils.getBestWorld(mc);
                Pair<BlockEntity, CompoundData> pair = this.getTargetedBlockEntity(bestWorld, mc);

                if (pair != null)
                {
                    InfoLineContext ctx = new InfoLineContext(bestWorld, null, pair.getLeft(), null, null, null, pair.getRight());
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
                Level bestWorld = WorldUtils.getBestWorld(mc);
                Pair<BlockEntity, CompoundData> pair = this.getTargetedBlockEntity(bestWorld, mc);

                if (pair != null)
                {
                    InfoLineContext ctx = new InfoLineContext(bestWorld, null, pair.getLeft(), null, null, null, pair.getRight());
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
                    InfoLineContext ctx = new InfoLineContext(world, null, null, null, state, null, null);
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
                Level bestWorld = WorldUtils.getBestWorld(mc);
                Pair<BlockEntity, CompoundData> pair = this.getTargetedBlockEntity(bestWorld, mc);

                if (pair != null)
                {
                    InfoLineContext ctx = new InfoLineContext(bestWorld, null, pair.getLeft(), null, null, null, pair.getRight());
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
                 type == InfoToggle.HORSE_JUMP ||
                 type == InfoToggle.HORSE_MAX_HEALTH)
        {
            if (this.addedTypes.contains(type))
            {
                return;
            }

            // Make into a generic call
            Level bestWorld = WorldUtils.getBestWorld(mc);
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                Pair<Entity, CompoundData> pair = this.getTargetEntity(bestWorld, mc);
                InfoLineContext ctx;

                if (mc.player.isPassenger() && pair == null)
                {
                    ctx = new InfoLineContext(bestWorld, mc.player.getVehicle(), null, null, null, null, null);
                }
                else if (pair != null)
                {
                    ctx = new InfoLineContext(bestWorld, pair.getLeft(), null, null, null, null, pair.getRight());
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

	        InfoLine parser = type.initParser();

	        if (parser != null)
	        {
		        InfoLineContext ctx = new InfoLineContext(world, entity, null, null, null, null, null);
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
        else if (type == InfoToggle.SPEED_HV)
        {
	        InfoLine parser = type.initParser();

	        if (parser != null)
	        {
		        InfoLineContext ctx = new InfoLineContext(world, entity, null, null, null, null, null);
		        this.processEntries(parser.parse(ctx));
	        }
	        else
	        {
		        return;
	        }
        }
        else if (type == InfoToggle.SPEED_AXIS)
        {
	        InfoLine parser = type.initParser();

	        if (parser != null)
	        {
		        InfoLineContext ctx = new InfoLineContext(world, entity, null, null, null, null, null);
		        this.processEntries(parser.parse(ctx));
	        }
	        else
	        {
		        return;
	        }
        }
        else if (type == InfoToggle.CHUNK_SECTIONS)
        {
            // Make into a generic call
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                InfoLineContext ctx = new InfoLineContext(world, null, null, null, null, null, null);
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
                InfoLineContext ctx = new InfoLineContext(world, null, null, null, null, null, null);
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
                InfoLineContext ctx = new InfoLineContext(world, null, null, null, null, null, null);
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
            Level bestWorld = WorldUtils.getBestWorld(mc);
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                InfoLineContext ctx = new InfoLineContext(bestWorld, null, null, null, null, null, null);
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
                Pair<Entity, CompoundData> pair = this.getTargetEntity(world, mc);

                if (pair != null)
                {
                    InfoLineContext ctx = new InfoLineContext(world, pair.getLeft(), null, null, null, null, pair.getRight());
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
                InfoLineContext ctx = new InfoLineContext(world, null, null, null, null, null, null);
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
                InfoLineContext ctx = new InfoLineContext(world, null, null, pos, null, null, null);
                this.processEntries(parser.parse(ctx));
            }
            else
            {
                return;
            }
        }
        else if (type == InfoToggle.BIOME)
        {
	        InfoLine parser = type.initParser();

	        if (parser != null)
	        {
		        InfoLineContext ctx = new InfoLineContext(world, null, null, pos, null, chunkPos, null);
		        this.processEntries(parser.parse(ctx));
	        }
	        else
	        {
		        return;
	        }
        }
        else if (type == InfoToggle.BIOME_REG_NAME)
        {
	        InfoLine parser = type.initParser();

	        if (parser != null)
	        {
		        InfoLineContext ctx = new InfoLineContext(world, null, null, pos, null, chunkPos, null);
		        this.processEntries(parser.parse(ctx));
	        }
	        else
	        {
		        return;
	        }
        }
        else if (type == InfoToggle.ENTITIES ||
                 type == InfoToggle.TILE_ENTITIES)
        {
            if (this.addedTypes.contains(InfoToggle.ENTITIES) ||
                this.addedTypes.contains(InfoToggle.TILE_ENTITIES))
            {
                return;
            }

            InfoLine parser = type.initParser();

	        if (parser != null)
	        {
		        InfoLineContext ctx = new InfoLineContext(world, null, null, null, null, null, null);
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
        else if (type == InfoToggle.ENTITIES_CLIENT_WORLD)
        {
	        InfoLine parser = type.initParser();

	        if (parser != null)
	        {
		        InfoLineContext ctx = new InfoLineContext(WorldUtils.getBestWorld(mc), null, null, null, null, null, null);
		        this.processEntries(parser.parse(ctx));
	        }
	        else
	        {
		        return;
	        }
        }
        else if (type == InfoToggle.SLIME_CHUNK)
        {
            // Make into a generic call
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                InfoLineContext ctx = new InfoLineContext(world, null, null, pos, null, null, null);
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
                Pair<Entity, CompoundData> pair = this.getTargetEntity(world, mc);

                if (pair != null)
                {
                    InfoLineContext ctx = new InfoLineContext(world, pair.getLeft(), null, null, null, null, pair.getRight());
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
                Pair<Entity, CompoundData> pair = this.getTargetEntity(world, mc);

                if (pair != null)
                {
                    InfoLineContext ctx = new InfoLineContext(world, pair.getLeft(), null, null, null, null, pair.getRight());
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
                Pair<Entity, CompoundData> pair = this.getTargetEntity(world, mc);

                if (pair != null)
                {
                    InfoLineContext ctx = new InfoLineContext(world, pair.getLeft(), null, null, null, null, pair.getRight());
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
		else if (type == InfoToggle.ENTITY_COPPER_AGING)
		{
			InfoLine parser = type.initParser();

			if (parser != null)
			{
				Pair<Entity, CompoundData> pair = this.getTargetEntity(world, mc);

				if (pair != null)
				{
					InfoLineContext ctx = new InfoLineContext(world, pair.getLeft(), null, null, null, null, pair.getRight());
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
                Pair<Entity, CompoundData> pair = this.getTargetEntity(world, mc);

                if (pair != null)
                {
                    InfoLineContext ctx = new InfoLineContext(world, pair.getLeft(), null, null, null, null, pair.getRight());
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
                Pair<Entity, CompoundData> pair = this.getTargetEntity(world, mc);

                if (pair != null)
                {
                    InfoLineContext ctx = new InfoLineContext(world, pair.getLeft(), null, null, null, null, pair.getRight());
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
                Pair<Entity, CompoundData> pair = this.getTargetEntity(world, mc);

                if (pair != null)
                {
                    InfoLineContext ctx = new InfoLineContext(world, pair.getLeft(), null, null, null, null, pair.getRight());
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
                Pair<Entity, CompoundData> pair = this.getTargetEntity(world, mc);

                if (pair != null)
                {
                    InfoLineContext ctx = new InfoLineContext(world, pair.getLeft(), null, null, null, null, pair.getRight());
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
	        InfoLine parser = type.initParser();

	        if (parser != null && mc.player != null)
	        {
		        InfoLineContext ctx = new InfoLineContext(world, mc.player, null, null, null, null, null);
		        this.processEntries(parser.parse(ctx));
	        }
	        else
	        {
		        return;
	        }
        }
        else if (type == InfoToggle.LOOKING_AT_PLAYER_EXP)
        {
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                Pair<Entity, CompoundData> pair = this.getTargetEntity(world, mc);

                if (pair != null)
                {
                    InfoLineContext ctx = new InfoLineContext(world, pair.getLeft(), null, null, null, null, pair.getRight());
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
            Level bestWorld = WorldUtils.getBestWorld(mc);
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                BlockState state = this.getTargetedBlock(mc);

                if (state != null)
                {
                    BlockPos lookPos = ((BlockHitResult) mc.hitResult).getBlockPos();
                    InfoLineContext ctx = new InfoLineContext(bestWorld, null, null, lookPos, state, null, null);
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
            Level bestWorld = WorldUtils.getBestWorld(mc);
            InfoLine parser = type.initParser();

            if (parser != null)
            {
                BlockState state = this.getTargetedBlock(mc);

                if (state != null)
                {
                    BlockPos lookPos = ((BlockHitResult) mc.hitResult).getBlockPos();
                    InfoLineContext ctx = new InfoLineContext(bestWorld, null, null, lookPos, state, null, null);
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
        else if (type == InfoToggle.SCULK_WARNING_LEVEL)
        {
            if (this.addedTypes.contains(type))
            {
                return;
            }

            // Make into a generic call
            Level bestWorld = WorldUtils.getBestWorld(mc);
            InfoLine parser = type.initParser();

            if (parser != null && mc.player != null)
            {
                InfoLineContext ctx = new InfoLineContext(bestWorld, mc.player, null, null, null, null, null);
                this.processEntries(parser.parse(ctx));
            }
            else
            {
                return;
            }
        }
    }

    private boolean isEntityDataValid(@Nonnull CompoundData data)
    {
        // Has a valid Inventory = ServerWorld
        if (InventoryUtils.hasDataItems(data))
        {
            return true;
        }

        for (String key : data.getKeys())
        {
            switch (key)
            {
                // If `Fire == 0` instead of `-1` means it's ClientWorld; it's ridiculous, but it works.
                case NbtKeys.FIRE ->
                {
                    int fire = data.contains(NbtKeys.FIRE, Constants.NBT.TAG_SHORT) ? data.getShort(NbtKeys.FIRE) : -1;

                    if (fire < 0 || fire > 0)
                    {
                        return true;
                    }
                }
                // If `Age == -1 or 1 instead of 0 or > 1 it's ClientWorld; it's ridiculous, but it works.
                case NbtKeys.AGE ->
                {
                    int age = data.contains(NbtKeys.AGE, Constants.NBT.TAG_INT) ? data.getInt(NbtKeys.AGE) : -1;

                    if (age == 0 || age > 1)
                    {
                        return true;
                    }
                }
                // Has a Brain besides the default = ServerWorld
                case NbtKeys.BRAIN ->
                {
	                CompoundData tag = data.getCompound(NbtKeys.BRAIN);

                    if (!tag.isEmpty() && !tag.getCompound(NbtKeys.MEMORIES).isEmpty())
                    {
                        return true;
                    }
                }
                case NbtKeys.OFFERS -> { return true; }
                case NbtKeys.TRADE_RECIPES -> { return true; }
                case NbtKeys.ZOMBIE_CONVERSION ->
                {
                    if (data.getInt(NbtKeys.ZOMBIE_CONVERSION) > 0)
                    {
                        return true;
                    }
                }
                case NbtKeys.DROWNED_CONVERSION ->
                {
                    if (data.getInt(NbtKeys.DROWNED_CONVERSION) > 0)
                    {
                        return true;
                    }
                }
                case NbtKeys.STRAY_CONVERSION ->
                {
                    if (data.getInt(NbtKeys.STRAY_CONVERSION) > 0)
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
    public Pair<Entity, CompoundData> getTargetEntity(Level world, Minecraft mc)
    {
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY)
        {
            Entity lookedEntity = ((EntityHitResult) mc.hitResult).getEntity();

            // Don't return the player entity (Apparently this is a thing in modern Minecraft versions)
            if (lookedEntity == null || lookedEntity.getId() == mc.player.getId())
            {
                return null;
            }

            Level bestWorld = WorldUtils.getBestWorld(mc);
            Pair<Entity, CompoundData> pair = null;

            if (bestWorld instanceof ServerLevel serverWorld)
            {
                Entity serverEntity = serverWorld.getEntity(lookedEntity.getId());
	            CompoundData data = DataEntityUtils.invokeEntityDataTagNoPassengers(serverEntity, lookedEntity.getId());

                if (!data.isEmpty())
                {
//                    nbt.putString("id", id.toString());
                    pair = Pair.of(serverEntity, data);
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
    public Pair<BlockEntity, CompoundData> getTargetedBlockEntity(Level world, Minecraft mc)
    {
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK)
        {
            BlockPos posLooking = ((BlockHitResult) mc.hitResult).getBlockPos();
            Level bestWorld = WorldUtils.getBestWorld(mc);
            BlockState state = bestWorld.getBlockState(posLooking);
            Pair<BlockEntity, CompoundData> pair = null;

            if (state.getBlock() instanceof EntityBlock)
            {
                if (bestWorld instanceof ServerLevel)
                {
	                CompoundData data = new CompoundData();
                    BlockEntity be = bestWorld.getChunkAt(posLooking).getBlockEntity(posLooking);
                    pair = Pair.of(be, be != null ? DataConverterNbt.fromVanillaCompound(be.saveWithFullMetadata(bestWorld.registryAccess())) : data);
                }
                else
                {
                    pair = EntitiesDataManager.getInstance().requestBlockEntity(world, posLooking);
                }

                // Remember the last entity so the "refresh time" is smoothed over.
                if (pair == null && this.lastBlockEntity != null &&
                    this.lastBlockEntity.getLeft().getBlockPos().equals(posLooking))
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
    public Pair<BlockEntity, CompoundData> requestBlockEntityAt(Level world, BlockPos pos)
    {
        if (!(world instanceof ServerLevel))
        {
            Pair<BlockEntity, CompoundData> pair = EntitiesDataManager.getInstance().requestBlockEntity(world, pos);

            BlockState state = world.getBlockState(pos);

            if (state.getBlock() instanceof ChestBlock)
            {
                ChestType type = state.getValue(ChestBlock.TYPE);

                if (type != ChestType.SINGLE)
                {
                    return EntitiesDataManager.getInstance().requestBlockEntity(world, pos.relative(ChestBlock.getConnectedDirection(state)));
                }
            }

            return pair;
        }

        return null;
    }

    @Nullable
    private BlockState getTargetedBlock(Minecraft mc)
    {
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK)
        {
            BlockPos posLooking = ((BlockHitResult) mc.hitResult).getBlockPos();
            return mc.level.getBlockState(posLooking);
        }

        return null;
    }

    private <T extends Comparable<T>> void getBlockProperties(Minecraft mc)
    {
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK)
        {
            BlockPos posLooking = ((BlockHitResult) mc.hitResult).getBlockPos();
            BlockState state = mc.level.getBlockState(posLooking);
            Identifier rl = BuiltInRegistries.BLOCK.getKey(state.getBlock());

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
