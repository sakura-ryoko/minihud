package fi.dy.masa.minihud.config;

import java.io.File;
import java.util.List;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.malilib.config.*;
import fi.dy.masa.malilib.config.options.*;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.renderer.OverlayRendererLightLevel;
import fi.dy.masa.minihud.renderer.OverlayRendererStructures;
import fi.dy.masa.minihud.util.BlockGridMode;
import fi.dy.masa.minihud.util.LightLevelMarkerMode;
import fi.dy.masa.minihud.util.LightLevelNumberMode;
import fi.dy.masa.minihud.util.LightLevelRenderCondition;

public class Configs implements IConfigHandler
{
    private static final String CONFIG_FILE_NAME = Reference.MOD_ID + ".json";
    private static final int CONFIG_VERSION = 1;

    public static class Generic
    {
        public static final ConfigBoolean       AXOLOTL_TOOLTIPS                    = new ConfigBoolean("axolotlTooltips", false, "minihud.config.generic.comment.axolotlTooltips").translatedName("minihud.config.generic.name.axolotlTooltips");
        public static final ConfigBoolean       BEE_TOOLTIPS                        = new ConfigBoolean("beeTooltips", false, "minihud.config.generic.comment.beeTooltips").translatedName("minihud.config.generic.name.beeTooltips");
        public static final ConfigBoolean       HONEY_TOOLTIPS                      = new ConfigBoolean("honeyTooltips", false, "minihud.config.generic.comment.honeyTooltips").translatedName("minihud.config.generic.name.honeyTooltips");
        public static final ConfigInteger       BIOME_OVERLAY_RANGE                 = new ConfigInteger("biomeOverlayRange", 4, 0, 32, "minihud.config.generic.comment.biomeOverlayRange").translatedName("minihud.config.generic.name.biomeOverlayRange");
        public static final ConfigInteger       BIOME_OVERLAY_RANGE_VERTICAL        = new ConfigInteger("biomeOverlayRangeVertical", 0, 0, 32, "minihud.config.generic.comment.biomeOverlayRangeVertical").translatedName("minihud.config.generic.name.biomeOverlayRangeVertical");
        public static final ConfigBoolean       BIOME_OVERLAY_SINGLE_COLOR          = new ConfigBoolean("biomeOverlaySingleColor", true, "minihud.config.generic.comment.biomeOverlaySingleColor").translatedName("minihud.config.generic.name.biomeOverlaySingleColor");
        public static final ConfigString        BLOCK_POS_FORMAT_STRING             = new ConfigString("blockPosFormat", "Block: %d, %d, %d", "minihud.config.generic.comment.blockPosFormat").translatedName("minihud.config.generic.name.blockPosFormat");
        public static final ConfigOptionList    BLOCK_GRID_OVERLAY_MODE             = new ConfigOptionList("blockGridOverlayMode", BlockGridMode.ALL, "minihud.config.generic.comment.blockGridOverlayMode").translatedName("minihud.config.generic.name.blockGridOverlayMode");
        public static final ConfigInteger       BLOCK_GRID_OVERLAY_RADIUS           = new ConfigInteger("blockGridOverlayRadius", 32, 0, 128, "minihud.config.generic.comment.blockGridOverlayRadius").translatedName("minihud.config.generic.name.blockGridOverlayRadius");
        public static final ConfigString        COORDINATE_FORMAT_STRING            = new ConfigString("coordinateFormat", "x: %.1f y: %.1f z: %.1f", "minihud.config.generic.comment.coordinateFormat").translatedName("minihud.config.generic.name.coordinateFormat");
        public static final ConfigString        DATE_FORMAT_REAL                    = new ConfigString("dateFormatReal", "yyyy-MM-dd HH:mm:ss", "minihud.config.generic.comment.dateFormatReal").translatedName("minihud.config.generic.name.dateFormatReal");
        public static final ConfigString        DATE_FORMAT_MINECRAFT               = new ConfigString("dateFormatMinecraft", "MC time: (day {DAY}) {HOUR}:{MIN}:xx", "minihud.config.generic.comment.dateFormatMinecraft").translatedName("minihud.config.generic.name.dateFormatMinecraft");
        public static final ConfigBoolean       DEBUG_MESSAGES                      = new ConfigBoolean("debugMessages", false, "minihud.config.generic.comment.debugMessages").translatedName("minihud.config.generic.name.debugMessages");
        //public static final ConfigBoolean       DEBUG_RENDERER_PATH_MAX_DIST        = new ConfigBoolean("debugRendererPathFindingEnablePointWidth", true, "minihud.config.generic.comment.debugRendererPathFindingEnablePointWidth").translatedName("minihud.config.generic.name.debugRendererPathFindingEnablePointWidth");
        public static final ConfigBoolean       DONT_RESET_SEED_ON_DIMENSION_CHANGE = new ConfigBoolean("dontClearStoredSeedOnDimensionChange", true, "minihud.config.generic.comment.dontClearStoredSeedOnDimensionChange").translatedName("minihud.config.generic.name.dontClearStoredSeedOnDimensionChange");
        public static final ConfigBoolean       ENTITY_DATA_SYNC                    = new ConfigBoolean("entityDataSync", true, "minihud.config.generic.comment.entityDataSync").translatedName("minihud.config.generic.name.entityDataSync");
        public static final ConfigBoolean       ENTITY_DATA_SYNC_BACKUP             = new ConfigBoolean("entityDataSyncBackup", true, "minihud.config.generic.comment.entityDataSyncBackup").translatedName("minihud.config.generic.name.entityDataSyncBackup");
        //public static final ConfigBoolean       FIX_VANILLA_DEBUG_RENDERERS         = new ConfigBoolean("enableVanillaDebugRendererFix", true, "minihud.config.generic.comment.enableVanillaDebugRendererFix").translatedName("minihud.config.generic.name.enableVanillaDebugRendererFix");
        public static final ConfigDouble        FONT_SCALE                          = new ConfigDouble("fontScale", 0.5, 0.01, 100.0, "minihud.config.generic.comment.fontScale").translatedName("minihud.config.generic.name.fontScale");
        public static final ConfigOptionList    HUD_ALIGNMENT                       = new ConfigOptionList("hudAlignment", HudAlignment.TOP_LEFT, "minihud.config.generic.comment.hudAlignment").translatedName("minihud.config.generic.name.hudAlignment");
        public static final ConfigHotkey        INVENTORY_PREVIEW                   = new ConfigHotkey("inventoryPreview", "LEFT_ALT", KeybindSettings.PRESS_ALLOWEXTRA, "minihud.config.generic.comment.inventoryPreview").translatedName("minihud.config.generic.name.inventoryPreview");
        public static final ConfigBoolean       INVENTORY_PREVIEW_ENABLED           = new ConfigBoolean("inventoryPreviewEnabled", false, "minihud.config.generic.comment.inventoryPreviewEnabled").translatedName("minihud.config.generic.name.inventoryPreviewEnabled");
        public static final ConfigHotkey        INVENTORY_PREVIEW_TOGGLE_SCREEN     = new ConfigHotkey("inventoryPreviewToggleScreen", "BUTTON_3", KeybindSettings.create(KeybindSettings.Context.ANY, KeyAction.PRESS, true, true, false, true), "minihud.config.generic.comment.inventoryPreviewToggleScreen").translatedName("minihud.config.generic.name.inventoryPreviewToggleScreen");
        public static final ConfigBoolean       LIGHT_LEVEL_AUTO_HEIGHT             = new ConfigBoolean("lightLevelAutoHeight", false, "minihud.config.generic.comment.lightLevelAutoHeight").translatedName("minihud.config.generic.name.lightLevelAutoHeight");
        public static final ConfigBoolean       LIGHT_LEVEL_COLORED_NUMBERS         = new ConfigBoolean("lightLevelColoredNumbers", true, "minihud.config.generic.comment.lightLevelColoredNumbers").translatedName("minihud.config.generic.name.lightLevelColoredNumbers");
        public static final ConfigBoolean       LIGHT_LEVEL_COLLISION_CHECK         = new ConfigBoolean("lightLevelCollisionCheck", false, "minihud.config.generic.comment.lightLevelCollisionCheck").translatedName("minihud.config.generic.name.lightLevelCollisionCheck");
        public static final ConfigOptionList    LIGHT_LEVEL_MARKER_CONDITION        = new ConfigOptionList("lightLevelMarkerCondition", LightLevelRenderCondition.SPAWNABLE, "minihud.config.generic.comment.lightLevelMarkerCondition").translatedName("minihud.config.generic.name.lightLevelMarkerCondition");
        public static final ConfigOptionList    LIGHT_LEVEL_MARKER_MODE             = new ConfigOptionList("lightLevelMarkers", LightLevelMarkerMode.SQUARE, "minihud.config.generic.comment.lightLevelMarkers").translatedName("minihud.config.generic.name.lightLevelMarkers");
        public static final ConfigDouble        LIGHT_LEVEL_MARKER_SIZE             = new ConfigDouble("lightLevelMarkerSize", 0.84, 0.0, 1.0, "minihud.config.generic.comment.lightLevelMarkerSize").translatedName("minihud.config.generic.name.lightLevelMarkerSize");
        public static final ConfigOptionList    LIGHT_LEVEL_NUMBER_CONDITION        = new ConfigOptionList("lightLevelNumberCondition", LightLevelRenderCondition.ALWAYS, "minihud.config.generic.comment.lightLevelNumberCondition").translatedName("minihud.config.generic.name.lightLevelNumberCondition");
        public static final ConfigOptionList    LIGHT_LEVEL_NUMBER_MODE             = new ConfigOptionList("lightLevelNumbers", LightLevelNumberMode.BLOCK, "minihud.config.generic.comment.lightLevelNumbers").translatedName("minihud.config.generic.name.lightLevelNumbers");
        public static final ConfigDouble        LIGHT_LEVEL_NUMBER_OFFSET_BLOCK_X   = new ConfigDouble("lightLevelNumberOffsetBlockX", 0.26, 0.0, 1.0, "minihud.config.generic.comment.lightLevelNumberOffsetBlockX").translatedName("minihud.config.generic.name.lightLevelNumberOffsetBlockX");
        public static final ConfigDouble        LIGHT_LEVEL_NUMBER_OFFSET_BLOCK_Y   = new ConfigDouble("lightLevelNumberOffsetBlockY", 0.32, 0.0, 1.0, "minihud.config.generic.comment.lightLevelNumberOffsetBlockY").translatedName("minihud.config.generic.name.lightLevelNumberOffsetBlockY");
        public static final ConfigDouble        LIGHT_LEVEL_NUMBER_OFFSET_SKY_X     = new ConfigDouble("lightLevelNumberOffsetSkyX", 0.42, 0.0, 1.0, "minihud.config.generic.comment.lightLevelNumberOffsetSkyX").translatedName("minihud.config.generic.name.lightLevelNumberOffsetSkyX");
        public static final ConfigDouble        LIGHT_LEVEL_NUMBER_OFFSET_SKY_Y     = new ConfigDouble("lightLevelNumberOffsetSkyY", 0.56, 0.0, 1.0, "minihud.config.generic.comment.lightLevelNumberOffsetSkyY").translatedName("minihud.config.generic.name.lightLevelNumberOffsetSkyY");
        public static final ConfigBoolean       LIGHT_LEVEL_NUMBER_ROTATION         = new ConfigBoolean("lightLevelNumberRotation", true, "minihud.config.generic.comment.lightLevelNumberRotation").translatedName("minihud.config.generic.name.lightLevelNumberRotation");
        public static final ConfigInteger       LIGHT_LEVEL_RANGE                   = new ConfigInteger("lightLevelRange", 24, 1, 64, "minihud.config.generic.comment.lightLevelRange").translatedName("minihud.config.generic.name.lightLevelRange");
        public static final ConfigDouble        LIGHT_LEVEL_RENDER_OFFSET           = new ConfigDouble("lightLevelRenderOffset", 0.005, 0.0, 1.0, "minihud.config.generic.comment.lightLevelRenderOffset").translatedName("minihud.config.generic.name.lightLevelRenderOffset");
        public static final ConfigBoolean       LIGHT_LEVEL_RENDER_THROUGH          = new ConfigBoolean("lightLevelRenderThrough", false, "minihud.config.generic.comment.lightLevelRenderThrough").translatedName("minihud.config.generic.name.lightLevelRenderThrough");
        public static final ConfigBoolean       LIGHT_LEVEL_SKIP_BLOCK_CHECK        = new ConfigBoolean("lightLevelSkipBlockCheck", false, "minihud.config.generic.comment.lightLevelSkipBlockCheck").translatedName("minihud.config.generic.name.lightLevelSkipBlockCheck");
        public static final ConfigInteger       LIGHT_LEVEL_THRESHOLD_DIM           = new ConfigInteger("lightLevelThresholdDim", 0, 0, 15, "minihud.config.generic.comment.lightLevelThresholdDim").translatedName("minihud.config.generic.name.lightLevelThresholdDim");
        public static final ConfigInteger       LIGHT_LEVEL_THRESHOLD_SAFE          = new ConfigInteger("lightLevelThresholdSafe", 1, 0, 15, "minihud.config.generic.comment.lightLevelThresholdSafe").translatedName("minihud.config.generic.name.lightLevelThresholdSafe");
        public static final ConfigBoolean       LIGHT_LEVEL_UNDER_WATER             = new ConfigBoolean("lightLevelUnderWater", false, "minihud.config.generic.comment.lightLevelUnderWater").translatedName("minihud.config.generic.name.lightLevelUnderWater");
        public static final ConfigBooleanHotkeyed MAIN_RENDERING_TOGGLE             = new ConfigBooleanHotkeyed("mainRenderingToggle", true, "H", KeybindSettings.RELEASE_EXCLUSIVE, "minihud.config.generic.comment.mainRenderingToggle", "minihud.config.generic.prettyName.mainRenderingToggle").translatedName("minihud.config.generic.name.mainRenderingToggle");
        public static final ConfigBoolean       MAP_PREVIEW                         = new ConfigBoolean("mapPreview", false, "minihud.config.generic.comment.mapPreview").translatedName("minihud.config.generic.name.mapPreview");
        public static final ConfigBoolean       MAP_PREVIEW_REQUIRE_SHIFT           = new ConfigBoolean("mapPreviewRequireShift", true, "minihud.config.generic.comment.mapPreviewRequireShift").translatedName("minihud.config.generic.name.mapPreviewRequireShift");
        public static final ConfigInteger       MAP_PREVIEW_SIZE                    = new ConfigInteger("mapPreviewSize", 160, 16, 512, "minihud.config.generic.comment.mapPreviewSize").translatedName("minihud.config.generic.name.mapPreviewSize");
        public static final ConfigHotkey        MOVE_SHAPE_TO_PLAYER                = new ConfigHotkey("moveShapeToPlayer", "", "minihud.config.generic.comment.moveShapeToPlayer").translatedName("minihud.config.generic.name.moveShapeToPlayer");
        public static final ConfigBoolean       OFFSET_SUBTITLE_HUD                 = new ConfigBoolean("offsetSubtitleHud", true, "minihud.config.generic.comment.offsetSubtitleHud").translatedName("minihud.config.generic.name.offsetSubtitleHud");
        public static final ConfigHotkey        OPEN_CONFIG_GUI                     = new ConfigHotkey("openConfigGui", "H,C", "minihud.config.generic.comment.openConfigGui").translatedName("minihud.config.generic.name.openConfigGui");
        public static final ConfigBoolean       REQUIRE_SNEAK                       = new ConfigBoolean("requireSneak", false, "minihud.config.generic.comment.requireSneak").translatedName("minihud.config.generic.name.requireSneak");
        public static final ConfigHotkey        REQUIRED_KEY                        = new ConfigHotkey("requiredKey", "", KeybindSettings.MODIFIER_INGAME_EMPTY, "minihud.config.generic.comment.requiredKey").translatedName("minihud.config.generic.name.requiredKey");
        public static final ConfigInteger       SERVER_NBT_REQUEST_RATE             = new ConfigInteger("serverNbtRequestRate", 2, "minihud.config.generic.comment.serverNbtRequestRate").translatedName("minihud.config.generic.name.serverNbtRequestRate");
        public static final ConfigHotkey        SET_DISTANCE_REFERENCE_POINT        = new ConfigHotkey("setDistanceReferencePoint", "", "minihud.config.generic.comment.setDistanceReferencePoint").translatedName("minihud.config.generic.name.setDistanceReferencePoint");
        public static final ConfigHotkey        SHAPE_EDITOR                        = new ConfigHotkey("shapeEditor", "", "minihud.config.generic.comment.shapeEditor").translatedName("minihud.config.generic.name.shapeEditor");
        public static final ConfigBoolean       SHULKER_BOX_PREVIEW                 = new ConfigBoolean("shulkerBoxPreview", false, "minihud.config.generic.comment.shulkerBoxPreview").translatedName("minihud.config.generic.name.shulkerBoxPreview");
        public static final ConfigBoolean       SHULKER_DISPLAY_BACKGROUND_COLOR    = new ConfigBoolean("shulkerDisplayBgColor", true, "minihud.config.generic.comment.shulkerDisplayBgColor").translatedName("minihud.config.generic.name.shulkerDisplayBgColor");
        public static final ConfigBoolean       SHULKER_DISPLAY_REQUIRE_SHIFT       = new ConfigBoolean("shulkerDisplayRequireShift", true, "minihud.config.generic.comment.shulkerDisplayRequireShift").translatedName("minihud.config.generic.name.shulkerDisplayRequireShift");
        public static final ConfigBoolean       SLIME_CHUNK_TOP_TO_PLAYER           = new ConfigBoolean("slimeChunkTopToPlayer", true, "minihud.config.generic.comment.slimeChunkTopToPlayer").translatedName("minihud.config.generic.name.slimeChunkTopToPlayer");
        public static final ConfigInteger       SLIME_CHUNK_OVERLAY_RADIUS          = new ConfigInteger("slimeChunkOverlayRadius", -1, -1, 40, "minihud.config.generic.comment.slimeChunkOverlayRadius").translatedName("minihud.config.generic.name.slimeChunkOverlayRadius");
        public static final ConfigBoolean       SORT_LINES_BY_LENGTH                = new ConfigBoolean("sortLinesByLength", false, "minihud.config.generic.comment.sortLinesByLength").translatedName("minihud.config.generic.name.sortLinesByLength");
        public static final ConfigBoolean       SORT_LINES_REVERSED                 = new ConfigBoolean("sortLinesReversed", false, "minihud.config.generic.comment.sortLinesReversed").translatedName("minihud.config.generic.name.sortLinesReversed");
        public static final ConfigInteger       SPAWNABLE_COLUMNS_OVERLAY_RADIUS    = new ConfigInteger("spawnableColumnHeightsOverlayRadius", 40, 0, 128, "minihud.config.generic.comment.spawnableColumnHeightsOverlayRadius").translatedName("minihud.config.generic.name.spawnableColumnHeightsOverlayRadius");
        public static final ConfigBoolean       STRUCTURES_RENDER_THROUGH           = new ConfigBoolean("structuresRenderThrough", false, "minihud.config.generic.comment.structuresRenderThrough").translatedName("minihud.config.generic.name.structuresRenderThrough");
        public static final ConfigInteger       TEXT_POS_X                          = new ConfigInteger("textPosX", 4, 0, 8192, "minihud.config.generic.comment.textPosX").translatedName("minihud.config.generic.name.textPosX");
        public static final ConfigInteger       TEXT_POS_Y                          = new ConfigInteger("textPosY", 4, 0, 8192, "minihud.config.generic.comment.textPosY").translatedName("minihud.config.generic.name.textPosY");
        public static final ConfigInteger       TIME_DAY_DIVISOR                    = new ConfigInteger("timeDayDivisor", 24000, 1, Integer.MAX_VALUE, "minihud.config.generic.comment.timeDayDivisor").translatedName("minihud.config.generic.name.timeDayDivisor");
        public static final ConfigInteger       TIME_TOTAL_DIVISOR                  = new ConfigInteger("timeTotalDivisor", 24000, 1, Integer.MAX_VALUE, "minihud.config.generic.comment.timeTotalDivisor").translatedName("minihud.config.generic.name.timeTotalDivisor");
        public static final ConfigBoolean       USE_CUSTOMIZED_COORDINATES          = new ConfigBoolean("useCustomizedCoordinateFormat", true, "minihud.config.generic.comment.useCustomizedCoordinateFormat").translatedName("minihud.config.generic.name.useCustomizedCoordinateFormat");
        public static final ConfigBoolean       USE_FONT_SHADOW                     = new ConfigBoolean("useFontShadow", false, "minihud.config.generic.comment.useFontShadow").translatedName("minihud.config.generic.name.useFontShadow");
        public static final ConfigBoolean       USE_TEXT_BACKGROUND                 = new ConfigBoolean("useTextBackground", true, "minihud.config.generic.comment.useTextBackground").translatedName("minihud.config.generic.name.useTextBackground");
        public static final ConfigBoolean       VILLAGER_CONVERSION_TICKS           = new ConfigBoolean("villagerConversionTicks", true, "minihud.config.generic.comment.villagerConversionTicks").translatedName("minihud.config.generic.name.villagerConversionTicks");
        public static final ConfigBoolean       VILLAGER_OFFER_ENCHANTMENT_BOOKS    = new ConfigBoolean("villagerOfferEnchantmentBooks", true, "minihud.config.generic.comment.villagerOfferEnchantmentBooks").translatedName("minihud.config.generic.name.villagerOfferEnchantmentBooks");
        public static final ConfigBoolean       VILLAGER_OFFER_HIGHEST_LEVEL_ONLY   = new ConfigBoolean("villagerOfferHighestLevelOnly", false, "minihud.config.generic.comment.villagerOfferHighestLevelOnly").translatedName("minihud.config.generic.name.villagerOfferHighestLevelOnly");
        public static final ConfigBoolean       VILLAGER_OFFER_LOWEST_PRICE_NEARBY  = new ConfigBoolean("villagerOfferLowestPriceNearby" , false, "minihud.config.generic.comment.villagerOfferLowestPriceNearby").translatedName("minihud.config.generic.name.villagerOfferLowestPriceNearby");
        public static final ConfigDouble        VILLAGER_OFFER_PRICE_THRESHOLD      = new ConfigDouble("villagerOfferPriceThreshold", 1, 0, 1, "minihud.config.generic.comment.villagerOfferPriceThreshold").translatedName("minihud.config.generic.name.villagerOfferPriceThreshold");

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                AXOLOTL_TOOLTIPS,
                BEE_TOOLTIPS,
                HONEY_TOOLTIPS,
                BIOME_OVERLAY_SINGLE_COLOR,
                DEBUG_MESSAGES,
                //DEBUG_RENDERER_PATH_MAX_DIST,
                DONT_RESET_SEED_ON_DIMENSION_CHANGE,
                ENTITY_DATA_SYNC,
                ENTITY_DATA_SYNC_BACKUP,
                //FIX_VANILLA_DEBUG_RENDERERS,
                LIGHT_LEVEL_AUTO_HEIGHT,
                LIGHT_LEVEL_COLLISION_CHECK,
                LIGHT_LEVEL_COLORED_NUMBERS,
                LIGHT_LEVEL_NUMBER_ROTATION,
                LIGHT_LEVEL_RENDER_THROUGH,
                LIGHT_LEVEL_SKIP_BLOCK_CHECK,
                LIGHT_LEVEL_UNDER_WATER,
                MAP_PREVIEW,
                MAP_PREVIEW_REQUIRE_SHIFT,
                OFFSET_SUBTITLE_HUD,
                REQUIRE_SNEAK,
                SHULKER_BOX_PREVIEW,
                SHULKER_DISPLAY_BACKGROUND_COLOR,
                SHULKER_DISPLAY_REQUIRE_SHIFT,
                SLIME_CHUNK_TOP_TO_PLAYER,
                SORT_LINES_BY_LENGTH,
                SORT_LINES_REVERSED,
                STRUCTURES_RENDER_THROUGH,
                USE_CUSTOMIZED_COORDINATES,
                USE_FONT_SHADOW,
                USE_TEXT_BACKGROUND,

                MAIN_RENDERING_TOGGLE,
                MOVE_SHAPE_TO_PLAYER,
                OPEN_CONFIG_GUI,
                REQUIRED_KEY,
                SERVER_NBT_REQUEST_RATE,
                SET_DISTANCE_REFERENCE_POINT,
                SHAPE_EDITOR,

                BLOCK_GRID_OVERLAY_MODE,
                LIGHT_LEVEL_MARKER_CONDITION,
                LIGHT_LEVEL_MARKER_MODE,
                LIGHT_LEVEL_NUMBER_CONDITION,
                LIGHT_LEVEL_NUMBER_MODE,
                HUD_ALIGNMENT,

                BIOME_OVERLAY_RANGE,
                BIOME_OVERLAY_RANGE_VERTICAL,
                BLOCK_POS_FORMAT_STRING,
                BLOCK_GRID_OVERLAY_RADIUS,
                COORDINATE_FORMAT_STRING,
                DATE_FORMAT_REAL,
                DATE_FORMAT_MINECRAFT,
                FONT_SCALE,
                LIGHT_LEVEL_MARKER_SIZE,
                LIGHT_LEVEL_NUMBER_OFFSET_BLOCK_X,
                LIGHT_LEVEL_NUMBER_OFFSET_BLOCK_Y,
                LIGHT_LEVEL_NUMBER_OFFSET_SKY_X,
                LIGHT_LEVEL_NUMBER_OFFSET_SKY_Y,
                LIGHT_LEVEL_RANGE,
                LIGHT_LEVEL_THRESHOLD_DIM,
                LIGHT_LEVEL_THRESHOLD_SAFE,
                LIGHT_LEVEL_RENDER_OFFSET,
                MAP_PREVIEW_SIZE,
                SLIME_CHUNK_OVERLAY_RADIUS,
                SPAWNABLE_COLUMNS_OVERLAY_RADIUS,
                TEXT_POS_X,
                TEXT_POS_Y,
                TIME_DAY_DIVISOR,
                TIME_TOTAL_DIVISOR,
                INVENTORY_PREVIEW,
                INVENTORY_PREVIEW_ENABLED,
                INVENTORY_PREVIEW_TOGGLE_SCREEN,
                VILLAGER_CONVERSION_TICKS,
                VILLAGER_OFFER_ENCHANTMENT_BOOKS,
                VILLAGER_OFFER_HIGHEST_LEVEL_ONLY,
                VILLAGER_OFFER_LOWEST_PRICE_NEARBY,
                VILLAGER_OFFER_PRICE_THRESHOLD
        );

        public static final List<IHotkey> HOTKEY_LIST = ImmutableList.of(
                MAIN_RENDERING_TOGGLE,
                MOVE_SHAPE_TO_PLAYER,
                OPEN_CONFIG_GUI,
                REQUIRED_KEY,
                SET_DISTANCE_REFERENCE_POINT,
                SHAPE_EDITOR,
                INVENTORY_PREVIEW,
                INVENTORY_PREVIEW_TOGGLE_SCREEN
        );
    }

    public static class Colors
    {
        public static final ConfigColor BEACON_RANGE_LVL1_OVERLAY_COLOR         = new ConfigColor("beaconRangeLvl1",                    "#20E060FF", "minihud.config.colors.comment.beaconRangeLvl1").translatedName("minihud.config.colors.name.beaconRangeLvl1");
        public static final ConfigColor BEACON_RANGE_LVL2_OVERLAY_COLOR         = new ConfigColor("beaconRangeLvl2",                    "#20FFB040", "minihud.config.colors.comment.beaconRangeLvl2").translatedName("minihud.config.colors.name.beaconRangeLvl2");
        public static final ConfigColor BEACON_RANGE_LVL3_OVERLAY_COLOR         = new ConfigColor("beaconRangeLvl3",                    "#20FFF040", "minihud.config.colors.comment.beaconRangeLvl3").translatedName("minihud.config.colors.name.beaconRangeLvl3");
        public static final ConfigColor BEACON_RANGE_LVL4_OVERLAY_COLOR         = new ConfigColor("beaconRangeLvl4",                    "#2060FF40", "minihud.config.colors.comment.beaconRangeLvl4").translatedName("minihud.config.colors.name.beaconRangeLvl4");
        public static final ConfigColor BLOCK_GRID_OVERLAY_COLOR                = new ConfigColor("blockGridOverlayColor",              "#80FFFFFF", "minihud.config.colors.comment.blockGridOverlayColor").translatedName("minihud.config.colors.name.blockGridOverlayColor");
        public static final ConfigColor CONDUIT_RANGE_OVERLAY_COLOR             = new ConfigColor("conduitRange",                       "#2030FFFF", "minihud.config.colors.comment.conduitRange").translatedName("minihud.config.colors.name.conduitRange");
        public static final ConfigColor LIGHT_LEVEL_MARKER_BLOCK_LIT            = new ConfigColor("lightLevelMarkerBlockLit",           "#FF209040", "minihud.config.colors.comment.lightLevelMarkerBlockLit").translatedName("minihud.config.colors.name.lightLevelMarkerBlockLit");
        public static final ConfigColor LIGHT_LEVEL_MARKER_DARK                 = new ConfigColor("lightLevelMarkerDark",               "#FFFF4848", "minihud.config.colors.comment.lightLevelMarkerDark").translatedName("minihud.config.colors.name.lightLevelMarkerDark");
        public static final ConfigColor LIGHT_LEVEL_MARKER_DIM                  = new ConfigColor("lightLevelMarkerDim",                "#FFC0C040", "minihud.config.colors.comment.lightLevelMarkerDim").translatedName("minihud.config.colors.name.lightLevelMarkerDim");
        public static final ConfigColor LIGHT_LEVEL_MARKER_SKY_LIT              = new ConfigColor("lightLevelMarkerSkyLit",             "#FFFFFF33", "minihud.config.colors.comment.lightLevelMarkerSkyLit").translatedName("minihud.config.colors.name.lightLevelMarkerSkyLit");
        public static final ConfigColor LIGHT_LEVEL_NUMBER_BLOCK_DARK           = new ConfigColor("lightLevelNumberBlockDark",          "#FFC03030", "minihud.config.colors.comment.lightLevelNumberBlockDark").translatedName("minihud.config.colors.name.lightLevelNumberBlockDark");
        public static final ConfigColor LIGHT_LEVEL_NUMBER_BLOCK_DIM            = new ConfigColor("lightLevelNumberBlockDim",           "#FFC0C040", "minihud.config.colors.comment.lightLevelNumberBlockDim").translatedName("minihud.config.colors.name.lightLevelNumberBlockDim");
        public static final ConfigColor LIGHT_LEVEL_NUMBER_BLOCK_LIT            = new ConfigColor("lightLevelNumberBlockLit",           "#FF20FF40", "minihud.config.colors.comment.lightLevelNumberBlockLit").translatedName("minihud.config.colors.name.lightLevelNumberBlockLit");
        public static final ConfigColor LIGHT_LEVEL_NUMBER_SKY_DARK             = new ConfigColor("lightLevelNumberSkyDark",            "#FFFFF030", "minihud.config.colors.comment.lightLevelNumberSkyDark").translatedName("minihud.config.colors.name.lightLevelNumberSkyDark");
        public static final ConfigColor LIGHT_LEVEL_NUMBER_SKY_DIM              = new ConfigColor("lightLevelNumberSkyDim",             "#FFC0C030", "minihud.config.colors.comment.lightLevelNumberSkyDim").translatedName("minihud.config.colors.name.lightLevelNumberSkyDim");
        public static final ConfigColor LIGHT_LEVEL_NUMBER_SKY_LIT              = new ConfigColor("lightLevelNumberSkyLit",             "#FF40E0FF", "minihud.config.colors.comment.lightLevelNumberSkyLit").translatedName("minihud.config.colors.name.lightLevelNumberSkyLit");
        public static final ConfigColor RANDOM_TICKS_FIXED_OVERLAY_COLOR        = new ConfigColor("randomTicksFixedOverlayColor",       "#30F9F225", "minihud.config.colors.comment.randomTicksFixedOverlayColor").translatedName("minihud.config.colors.name.randomTicksFixedOverlayColor");
        public static final ConfigColor RANDOM_TICKS_PLAYER_OVERLAY_COLOR       = new ConfigColor("randomTicksPlayerOverlayColor",      "#3030FE73", "minihud.config.colors.comment.randomTicksPlayerOverlayColor").translatedName("minihud.config.colors.name.randomTicksPlayerOverlayColor");
        public static final ConfigColor REGION_OVERLAY_COLOR                    = new ConfigColor("regionOverlayColor",                 "#30FF8019", "minihud.config.colors.comment.regionOverlayColor").translatedName("minihud.config.colors.name.regionOverlayColor");
        public static final ConfigColor SHAPE_ADJUSTABLE_SPAWN_SPHERE           = new ConfigColor("shapeAdjustableSpawnSphere",         "#6030B0B0", "minihud.config.colors.comment.shapeAdjustableSpawnSphere").translatedName("minihud.config.colors.name.shapeAdjustableSpawnSphere");
        public static final ConfigColor SHAPE_BOX                               = new ConfigColor("shapeBox",                           "#6050A0A0", "minihud.config.colors.comment.shapeBox").translatedName("minihud.config.colors.name.shapeBox");
        public static final ConfigColor SHAPE_CAN_DESPAWN_SPHERE                = new ConfigColor("shapeCanDespawnSphere",              "#60A04050", "minihud.config.colors.comment.shapeCanDespawnSphere").translatedName("minihud.config.colors.name.shapeCanDespawnSphere");
        public static final ConfigColor SHAPE_CAN_SPAWN_SPHERE                  = new ConfigColor("shapeCanSpawnSphere",                "#60A04050", "minihud.config.colors.comment.shapeCanSpawnSphere").translatedName("minihud.config.colors.name.shapeCanSpawnSphere");
        public static final ConfigColor SHAPE_CIRCLE                            = new ConfigColor("shapeCircle",                        "#6030B0B0", "minihud.config.colors.comment.shapeCircle").translatedName("minihud.config.colors.name.shapeCircle");
        public static final ConfigColor SHAPE_DESPAWN_SPHERE                    = new ConfigColor("shapeDespawnSphere",                 "#60A04050", "minihud.config.colors.comment.shapeDespawnSphere").translatedName("minihud.config.colors.name.shapeDespawnSphere");
        public static final ConfigColor SHAPE_LINE_BLOCKY                       = new ConfigColor("shapeLineBlocky",                    "#6030F0B0", "minihud.config.colors.comment.shapeLineBlocky").translatedName("minihud.config.colors.name.shapeLineBlocky");
        public static final ConfigColor SHAPE_SPHERE_BLOCKY                     = new ConfigColor("shapeSphereBlocky",                  "#6030B0B0", "minihud.config.colors.comment.shapeSphereBlocky").translatedName("minihud.config.colors.name.shapeSphereBlocky");
        public static final ConfigColor SLIME_CHUNKS_OVERLAY_COLOR              = new ConfigColor("slimeChunksOverlayColor",            "#3020F020", "minihud.config.colors.comment.slimeChunksOverlayColor").translatedName("minihud.config.colors.name.slimeChunksOverlayColor");
        public static final ConfigColor SPAWN_PLAYER_ENTITY_OVERLAY_COLOR       = new ConfigColor("spawnPlayerEntityOverlayColor",      "#302050D0", "minihud.config.colors.comment.spawnPlayerEntityOverlayColor").translatedName("minihud.config.colors.name.spawnPlayerEntityOverlayColor");
        public static final ConfigColor SPAWN_PLAYER_LAZY_OVERLAY_COLOR         = new ConfigColor("spawnPlayerLazyOverlayColor",        "#30D030D0", "minihud.config.colors.comment.spawnPlayerLazyOverlayColor").translatedName("minihud.config.colors.name.spawnPlayerLazyOverlayColor");
        public static final ConfigColor SPAWN_PLAYER_OUTER_OVERLAY_COLOR        = new ConfigColor("spawnPlayerOuterOverlayColor",       "#306900D2", "minihud.config.colors.comment.spawnPlayerOuterOverlayColor").translatedName("minihud.config.colors.name.spawnPlayerOuterOverlayColor");
        public static final ConfigColor SPAWN_REAL_ENTITY_OVERLAY_COLOR         = new ConfigColor("spawnRealEntityOverlayColor",        "#3030FF20", "minihud.config.colors.comment.spawnRealEntityOverlayColor").translatedName("minihud.config.colors.name.spawnRealEntityOverlayColor");
        public static final ConfigColor SPAWN_REAL_LAZY_OVERLAY_COLOR           = new ConfigColor("spawnRealLazyOverlayColor",          "#30FF3020", "minihud.config.colors.comment.spawnRealLazyOverlayColor").translatedName("minihud.config.colors.name.spawnRealLazyOverlayColor");
        public static final ConfigColor SPAWN_REAL_OUTER_OVERLAY_COLOR          = new ConfigColor("spawnRealOuterOverlayColor",         "#309D581A", "minihud.config.colors.comment.spawnRealOuterOverlayColor").translatedName("minihud.config.colors.name.spawnRealOuterOverlayColor");
        public static final ConfigColor SPAWNABLE_COLUMNS_OVERLAY_COLOR         = new ConfigColor("spawnableColumnHeightsOverlayColor", "#A0FF00FF", "minihud.config.colors.comment.spawnableColumnHeightsOverlayColor").translatedName("minihud.config.colors.name.spawnableColumnHeightsOverlayColor");
        public static final ConfigColor TEXT_BACKGROUND_COLOR                   = new ConfigColor("textBackgroundColor",                "#A0505050", "minihud.config.colors.comment.textBackgroundColor").translatedName("minihud.config.colors.name.textBackgroundColor");
        public static final ConfigColor TEXT_COLOR                              = new ConfigColor("textColor",                          "#FFE0E0E0", "minihud.config.colors.comment.textColor").translatedName("minihud.config.colors.name.textColor");

        public static final ImmutableList<IConfigValue> OPTIONS = ImmutableList.of(
                BEACON_RANGE_LVL1_OVERLAY_COLOR,
                BEACON_RANGE_LVL2_OVERLAY_COLOR,
                BEACON_RANGE_LVL3_OVERLAY_COLOR,
                BEACON_RANGE_LVL4_OVERLAY_COLOR,
                BLOCK_GRID_OVERLAY_COLOR,
                CONDUIT_RANGE_OVERLAY_COLOR,
                LIGHT_LEVEL_MARKER_BLOCK_LIT,
                LIGHT_LEVEL_MARKER_DARK,
                LIGHT_LEVEL_MARKER_DIM,
                LIGHT_LEVEL_MARKER_SKY_LIT,
                LIGHT_LEVEL_NUMBER_BLOCK_DARK,
                LIGHT_LEVEL_NUMBER_BLOCK_DIM,
                LIGHT_LEVEL_NUMBER_BLOCK_LIT,
                LIGHT_LEVEL_NUMBER_SKY_DARK,
                LIGHT_LEVEL_NUMBER_SKY_DIM,
                LIGHT_LEVEL_NUMBER_SKY_LIT,
                RANDOM_TICKS_FIXED_OVERLAY_COLOR,
                RANDOM_TICKS_PLAYER_OVERLAY_COLOR,
                REGION_OVERLAY_COLOR,
                SHAPE_ADJUSTABLE_SPAWN_SPHERE,
                SHAPE_BOX,
                SHAPE_CAN_DESPAWN_SPHERE,
                SHAPE_CAN_SPAWN_SPHERE,
                SHAPE_CIRCLE,
                SHAPE_DESPAWN_SPHERE,
                SHAPE_LINE_BLOCKY,
                SHAPE_SPHERE_BLOCKY,
                SLIME_CHUNKS_OVERLAY_COLOR,
                SPAWN_PLAYER_ENTITY_OVERLAY_COLOR,
                SPAWN_PLAYER_LAZY_OVERLAY_COLOR,
                SPAWN_PLAYER_OUTER_OVERLAY_COLOR,
                SPAWN_REAL_ENTITY_OVERLAY_COLOR,
                SPAWN_REAL_LAZY_OVERLAY_COLOR,
                SPAWN_REAL_OUTER_OVERLAY_COLOR,
                SPAWNABLE_COLUMNS_OVERLAY_COLOR,
                TEXT_BACKGROUND_COLOR,
                TEXT_COLOR
        );
    }

    public static void loadFromFile()
    {
        File configFile = new File(FileUtils.getConfigDirectory(), CONFIG_FILE_NAME);

        if (configFile.exists() && configFile.isFile() && configFile.canRead())
        {
            JsonElement element = JsonUtils.parseJsonFile(configFile);

            if (element != null && element.isJsonObject())
            {
                JsonObject root = element.getAsJsonObject();
                JsonObject objInfoLineOrders = JsonUtils.getNestedObject(root, "InfoLineOrders", false);

                ConfigUtils.readConfigBase(root, "Colors", Configs.Colors.OPTIONS);
                ConfigUtils.readConfigBase(root, "Generic", Configs.Generic.OPTIONS);
                ConfigUtils.readHotkeyToggleOptions(root, "InfoHotkeys", "InfoTypeToggles", InfoToggle.VALUES);
                ConfigUtils.readHotkeyToggleOptions(root, "RendererHotkeys", "RendererToggles", RendererToggle.VALUES);
                ConfigUtils.readConfigBase(root, "StructureColors", StructureToggle.COLOR_CONFIGS);
                ConfigUtils.readConfigBase(root, "StructureHotkeys", StructureToggle.HOTKEY_CONFIGS);
                ConfigUtils.readConfigBase(root, "StructureToggles", StructureToggle.TOGGLE_CONFIGS);

                int version = JsonUtils.getIntegerOrDefault(root, "config_version", 0);

                if (objInfoLineOrders != null && version >= 1)
                {
                    for (InfoToggle toggle : InfoToggle.VALUES)
                    {
                        if (JsonUtils.hasInteger(objInfoLineOrders, toggle.getName()))
                        {
                            toggle.setIntegerValue(JsonUtils.getInteger(objInfoLineOrders, toggle.getName()));
                        }
                    }
                }
            }

            OverlayRendererLightLevel.INSTANCE.setRenderThrough(Configs.Generic.LIGHT_LEVEL_RENDER_THROUGH.getBooleanValue());
            OverlayRendererStructures.INSTANCE.setRenderThrough(Configs.Generic.STRUCTURES_RENDER_THROUGH.getBooleanValue());
        }
    }

    public static void saveToFile()
    {
        File dir = FileUtils.getConfigDirectory();

        if ((dir.exists() && dir.isDirectory()) || dir.mkdirs())
        {
            JsonObject root = new JsonObject();
            JsonObject objInfoLineOrders = JsonUtils.getNestedObject(root, "InfoLineOrders", true);

            ConfigUtils.writeConfigBase(root, "Colors", Configs.Colors.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Generic", Configs.Generic.OPTIONS);
            ConfigUtils.writeHotkeyToggleOptions(root, "InfoHotkeys", "InfoTypeToggles", InfoToggle.VALUES);
            ConfigUtils.writeHotkeyToggleOptions(root, "RendererHotkeys", "RendererToggles", RendererToggle.VALUES);
            ConfigUtils.writeConfigBase(root, "StructureColors", StructureToggle.COLOR_CONFIGS);
            ConfigUtils.writeConfigBase(root, "StructureHotkeys", StructureToggle.HOTKEY_CONFIGS);
            ConfigUtils.writeConfigBase(root, "StructureToggles", StructureToggle.TOGGLE_CONFIGS);

            if (objInfoLineOrders != null)
            {
                for (InfoToggle toggle : InfoToggle.VALUES)
                {
                    objInfoLineOrders.add(toggle.getName(), new JsonPrimitive(toggle.getIntegerValue()));
                }
            }

            root.add("config_version", new JsonPrimitive(CONFIG_VERSION));

            JsonUtils.writeJsonToFile(root, new File(dir, CONFIG_FILE_NAME));
        }
    }

    @Override
    public void load()
    {
        loadFromFile();
    }

    @Override
    public void save()
    {
        saveToFile();
    }
}
