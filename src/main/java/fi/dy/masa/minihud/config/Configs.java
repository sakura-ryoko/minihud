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

    private static final String GENERIC_KEY = Reference.MOD_ID+".config.generic";
    public static class Generic
    {
        public static final ConfigBoolean       AXOLOTL_TOOLTIPS                    = new ConfigBoolean("axolotlTooltips", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       BEE_TOOLTIPS                        = new ConfigBoolean("beeTooltips", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       HONEY_TOOLTIPS                      = new ConfigBoolean("honeyTooltips", false).apply(GENERIC_KEY);
        public static final ConfigInteger       BIOME_OVERLAY_RANGE                 = new ConfigInteger("biomeOverlayRange", 4, 0, 32).apply(GENERIC_KEY);
        public static final ConfigInteger       BIOME_OVERLAY_RANGE_VERTICAL        = new ConfigInteger("biomeOverlayRangeVertical", 0, 0, 32).apply(GENERIC_KEY);
        public static final ConfigBoolean       BIOME_OVERLAY_SINGLE_COLOR          = new ConfigBoolean("biomeOverlaySingleColor", true).apply(GENERIC_KEY);
        public static final ConfigString        BLOCK_POS_FORMAT_STRING             = new ConfigString("blockPosFormat", "Block: %d, %d, %d").apply(GENERIC_KEY);
        public static final ConfigOptionList    BLOCK_GRID_OVERLAY_MODE             = new ConfigOptionList("blockGridOverlayMode", BlockGridMode.ALL).apply(GENERIC_KEY);
        public static final ConfigInteger       BLOCK_GRID_OVERLAY_RADIUS           = new ConfigInteger("blockGridOverlayRadius", 32, 0, 128).apply(GENERIC_KEY);
        public static final ConfigBoolean       BUNDLE_PREVIEW                      = new ConfigBoolean("bundlePreview", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       BUNDLE_DISPLAY_BACKGROUND_COLOR     = new ConfigBoolean("bundleDisplayBgColor", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       BUNDLE_DISPLAY_REQUIRE_SHIFT        = new ConfigBoolean("bundleDisplayRequireShift", true).apply(GENERIC_KEY);
        public static final ConfigString        COORDINATE_FORMAT_STRING            = new ConfigString("coordinateFormat", "x: %.1f y: %.1f z: %.1f").apply(GENERIC_KEY);
        public static final ConfigString        DATE_FORMAT_REAL                    = new ConfigString("dateFormatReal", "yyyy-MM-dd HH:mm:ss").apply(GENERIC_KEY);
        public static final ConfigString        DATE_FORMAT_MINECRAFT               = new ConfigString("dateFormatMinecraft", "MC time: (day {DAY}) {HOUR}:{MIN}:xx").apply(GENERIC_KEY);
        public static final ConfigBoolean       DEBUG_MESSAGES                      = new ConfigBoolean("debugMessages", false).apply(GENERIC_KEY);
        //public static final ConfigBoolean       DEBUG_DEVELOPMENT_MODE              = new ConfigBoolean("debugDevelopmentMode", false).apply(GENERIC_KEY);
        //public static final ConfigBoolean       DEBUG_RENDERER_PATH_MAX_DIST        = new ConfigBoolean("debugRendererPathFindingEnablePointWidth", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       DONT_RESET_SEED_ON_DIMENSION_CHANGE = new ConfigBoolean("dontClearStoredSeedOnDimensionChange", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       ENTITY_DATA_SYNC                    = new ConfigBoolean("entityDataSync", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       ENTITY_DATA_SYNC_BACKUP             = new ConfigBoolean("entityDataSyncBackup", true).apply(GENERIC_KEY);
        public static final ConfigFloat         ENTITY_DATA_SYNC_CACHE_TIMEOUT      = new ConfigFloat("entityDataSyncCacheTimeout", 1.0f, 0.25f, 5.0f).apply(GENERIC_KEY);
        public static final ConfigBoolean       ENTITY_DATA_LOAD_NBT                = new ConfigBoolean("entityDataSyncLoadNbt", false).apply(GENERIC_KEY);
        //public static final ConfigBoolean       FIX_VANILLA_DEBUG_RENDERERS         = new ConfigBoolean("enableVanillaDebugRendererFix", true).apply(GENERIC_KEY);
        public static final ConfigDouble        FONT_SCALE                          = new ConfigDouble("fontScale", 0.5, 0.01, 100.0).apply(GENERIC_KEY);
        public static final ConfigOptionList    HUD_ALIGNMENT                       = new ConfigOptionList("hudAlignment", HudAlignment.TOP_LEFT).apply(GENERIC_KEY);
        public static final ConfigBoolean       INFO_LINES_USES_NBT                 = new ConfigBoolean("infoLinesUsesNbt", true).apply(GENERIC_KEY);
        public static final ConfigHotkey        INVENTORY_PREVIEW                   = new ConfigHotkey("inventoryPreview", "LEFT_ALT", KeybindSettings.PRESS_ALLOWEXTRA).apply(GENERIC_KEY);
        public static final ConfigBoolean       INVENTORY_PREVIEW_ENABLED           = new ConfigBoolean("inventoryPreviewEnabled", false).apply(GENERIC_KEY);
        public static final ConfigHotkey        INVENTORY_PREVIEW_TOGGLE_SCREEN     = new ConfigHotkey("inventoryPreviewToggleScreen", "BUTTON_3", KeybindSettings.create(KeybindSettings.Context.ANY, KeyAction.PRESS, true, true, false, true)).apply(GENERIC_KEY);
        public static final ConfigBoolean       LIGHT_LEVEL_AUTO_HEIGHT             = new ConfigBoolean("lightLevelAutoHeight", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       LIGHT_LEVEL_COLORED_NUMBERS         = new ConfigBoolean("lightLevelColoredNumbers", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       LIGHT_LEVEL_COLLISION_CHECK         = new ConfigBoolean("lightLevelCollisionCheck", false).apply(GENERIC_KEY);
        public static final ConfigOptionList    LIGHT_LEVEL_MARKER_CONDITION        = new ConfigOptionList("lightLevelMarkerCondition", LightLevelRenderCondition.SPAWNABLE).apply(GENERIC_KEY);
        public static final ConfigOptionList    LIGHT_LEVEL_MARKER_MODE             = new ConfigOptionList("lightLevelMarkers", LightLevelMarkerMode.SQUARE).apply(GENERIC_KEY);
        public static final ConfigDouble        LIGHT_LEVEL_MARKER_SIZE             = new ConfigDouble("lightLevelMarkerSize", 0.84, 0.0, 1.0).apply(GENERIC_KEY);
        public static final ConfigOptionList    LIGHT_LEVEL_NUMBER_CONDITION        = new ConfigOptionList("lightLevelNumberCondition", LightLevelRenderCondition.ALWAYS).apply(GENERIC_KEY);
        public static final ConfigOptionList    LIGHT_LEVEL_NUMBER_MODE             = new ConfigOptionList("lightLevelNumbers", LightLevelNumberMode.BLOCK).apply(GENERIC_KEY);
        public static final ConfigDouble        LIGHT_LEVEL_NUMBER_OFFSET_BLOCK_X   = new ConfigDouble("lightLevelNumberOffsetBlockX", 0.26, 0.0, 1.0).apply(GENERIC_KEY);
        public static final ConfigDouble        LIGHT_LEVEL_NUMBER_OFFSET_BLOCK_Y   = new ConfigDouble("lightLevelNumberOffsetBlockY", 0.32, 0.0, 1.0).apply(GENERIC_KEY);
        public static final ConfigDouble        LIGHT_LEVEL_NUMBER_OFFSET_SKY_X     = new ConfigDouble("lightLevelNumberOffsetSkyX", 0.42, 0.0, 1.0).apply(GENERIC_KEY);
        public static final ConfigDouble        LIGHT_LEVEL_NUMBER_OFFSET_SKY_Y     = new ConfigDouble("lightLevelNumberOffsetSkyY", 0.56, 0.0, 1.0).apply(GENERIC_KEY);
        public static final ConfigBoolean       LIGHT_LEVEL_NUMBER_ROTATION         = new ConfigBoolean("lightLevelNumberRotation", true).apply(GENERIC_KEY);
        public static final ConfigInteger       LIGHT_LEVEL_RANGE                   = new ConfigInteger("lightLevelRange", 24, 1, 64).apply(GENERIC_KEY);
        public static final ConfigDouble        LIGHT_LEVEL_RENDER_OFFSET           = new ConfigDouble("lightLevelRenderOffset", 0.005, 0.0, 1.0).apply(GENERIC_KEY);
        public static final ConfigBoolean       LIGHT_LEVEL_RENDER_THROUGH          = new ConfigBoolean("lightLevelRenderThrough", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       LIGHT_LEVEL_SKIP_BLOCK_CHECK        = new ConfigBoolean("lightLevelSkipBlockCheck", false).apply(GENERIC_KEY);
        public static final ConfigInteger       LIGHT_LEVEL_THRESHOLD_DIM           = new ConfigInteger("lightLevelThresholdDim", 0, 0, 15).apply(GENERIC_KEY);
        public static final ConfigInteger       LIGHT_LEVEL_THRESHOLD_SAFE          = new ConfigInteger("lightLevelThresholdSafe", 1, 0, 15).apply(GENERIC_KEY);
        public static final ConfigBoolean       LIGHT_LEVEL_UNDER_WATER             = new ConfigBoolean("lightLevelUnderWater", false).apply(GENERIC_KEY);
        public static final ConfigBooleanHotkeyed MAIN_RENDERING_TOGGLE             = new ConfigBooleanHotkeyed("mainRenderingToggle", true, "H", KeybindSettings.RELEASE_EXCLUSIVE).apply(GENERIC_KEY);
        public static final ConfigBoolean       MAP_PREVIEW                         = new ConfigBoolean("mapPreview", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       MAP_PREVIEW_REQUIRE_SHIFT           = new ConfigBoolean("mapPreviewRequireShift", true).apply(GENERIC_KEY);
        public static final ConfigInteger       MAP_PREVIEW_SIZE                    = new ConfigInteger("mapPreviewSize", 160, 16, 512).apply(GENERIC_KEY);
        public static final ConfigHotkey        MOVE_SHAPE_TO_PLAYER                = new ConfigHotkey("moveShapeToPlayer", "").apply(GENERIC_KEY);
        public static final ConfigBoolean       OFFSET_SUBTITLE_HUD                 = new ConfigBoolean("offsetSubtitleHud", true).apply(GENERIC_KEY);
        public static final ConfigHotkey        OPEN_CONFIG_GUI                     = new ConfigHotkey("openConfigGui", "H,C").apply(GENERIC_KEY);
        public static final ConfigBoolean       REQUIRE_SNEAK                       = new ConfigBoolean("requireSneak", false).apply(GENERIC_KEY);
        public static final ConfigHotkey        REQUIRED_KEY                        = new ConfigHotkey("requiredKey", "", KeybindSettings.MODIFIER_INGAME_EMPTY).apply(GENERIC_KEY);
        public static final ConfigInteger       SERVER_NBT_REQUEST_RATE             = new ConfigInteger("serverNbtRequestRate", 2).apply(GENERIC_KEY);
        public static final ConfigHotkey        SET_DISTANCE_REFERENCE_POINT        = new ConfigHotkey("setDistanceReferencePoint", "").apply(GENERIC_KEY);
        public static final ConfigHotkey        SHAPE_EDITOR                        = new ConfigHotkey("shapeEditor", "").apply(GENERIC_KEY);
        public static final ConfigBoolean       SHULKER_BOX_PREVIEW                 = new ConfigBoolean("shulkerBoxPreview", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       SHULKER_DISPLAY_BACKGROUND_COLOR    = new ConfigBoolean("shulkerDisplayBgColor", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       SHULKER_DISPLAY_ENDER_CHEST         = new ConfigBoolean("shulkerDisplayEnderChest", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       SHULKER_DISPLAY_REQUIRE_SHIFT       = new ConfigBoolean("shulkerDisplayRequireShift", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       SLIME_CHUNK_TOP_TO_PLAYER           = new ConfigBoolean("slimeChunkTopToPlayer", true).apply(GENERIC_KEY);
        public static final ConfigInteger       SLIME_CHUNK_OVERLAY_RADIUS          = new ConfigInteger("slimeChunkOverlayRadius", -1, -1, 40).apply(GENERIC_KEY);
        public static final ConfigBoolean       SORT_LINES_BY_LENGTH                = new ConfigBoolean("sortLinesByLength", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       SORT_LINES_REVERSED                 = new ConfigBoolean("sortLinesReversed", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       SPAWN_PLAYER_OUTER_OVERLAY_ENABLED  = new ConfigBoolean("spawnPlayerOuterOverlayEnabled", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       SPAWN_PLAYER_REDSTONE_OVERLAY_ENABLED= new ConfigBoolean("spawnPlayerRedstoneOverlayEnabled", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       SPAWN_REAL_OUTER_OVERLAY_ENABLED    = new ConfigBoolean("spawnRealOuterOverlayEnabled", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       SPAWN_REAL_REDSTONE_OVERLAY_ENABLED = new ConfigBoolean("spawnRealRedstoneOverlayEnabled", false).apply(GENERIC_KEY);
        public static final ConfigInteger       SPAWNABLE_COLUMNS_OVERLAY_RADIUS    = new ConfigInteger("spawnableColumnHeightsOverlayRadius", 40, 0, 128).apply(GENERIC_KEY);
        public static final ConfigBoolean       STRUCTURES_RENDER_THROUGH           = new ConfigBoolean("structuresRenderThrough", false).apply(GENERIC_KEY);
        public static final ConfigInteger       TEXT_POS_X                          = new ConfigInteger("textPosX", 4, 0, 8192).apply(GENERIC_KEY);
        public static final ConfigInteger       TEXT_POS_Y                          = new ConfigInteger("textPosY", 4, 0, 8192).apply(GENERIC_KEY);
        public static final ConfigInteger       TIME_DAY_DIVISOR                    = new ConfigInteger("timeDayDivisor", 24000, 1, Integer.MAX_VALUE).apply(GENERIC_KEY);
        public static final ConfigInteger       TIME_TOTAL_DIVISOR                  = new ConfigInteger("timeTotalDivisor", 24000, 1, Integer.MAX_VALUE).apply(GENERIC_KEY);
        public static final ConfigBoolean       USE_CUSTOMIZED_COORDINATES          = new ConfigBoolean("useCustomizedCoordinateFormat", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       USE_FONT_SHADOW                     = new ConfigBoolean("useFontShadow", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       USE_TEXT_BACKGROUND                 = new ConfigBoolean("useTextBackground", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       VILLAGER_CONVERSION_TICKS           = new ConfigBoolean("villagerConversionTicks", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       VILLAGER_OFFER_ENCHANTMENT_BOOKS    = new ConfigBoolean("villagerOfferEnchantmentBooks", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       VILLAGER_OFFER_PRICE_RANGE          = new ConfigBoolean("villagerOfferPriceRange", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       VILLAGER_OFFER_HIGHEST_LEVEL_ONLY   = new ConfigBoolean("villagerOfferHighestLevelOnly", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       VILLAGER_OFFER_LOWEST_PRICE_NEARBY  = new ConfigBoolean("villagerOfferLowestPriceNearby" , false).apply(GENERIC_KEY);
        public static final ConfigDouble        VILLAGER_OFFER_PRICE_THRESHOLD      = new ConfigDouble("villagerOfferPriceThreshold", 1, 0, 1).apply(GENERIC_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                AXOLOTL_TOOLTIPS,
                BEE_TOOLTIPS,
                HONEY_TOOLTIPS,
                BIOME_OVERLAY_SINGLE_COLOR,
                BUNDLE_PREVIEW,
                BUNDLE_DISPLAY_BACKGROUND_COLOR,
                BUNDLE_DISPLAY_REQUIRE_SHIFT,
                DEBUG_MESSAGES,
                //DEBUG_DEVELOPMENT_MODE,
                //DEBUG_RENDERER_PATH_MAX_DIST,
                DONT_RESET_SEED_ON_DIMENSION_CHANGE,
                ENTITY_DATA_SYNC,
                ENTITY_DATA_SYNC_BACKUP,
                ENTITY_DATA_SYNC_CACHE_TIMEOUT,
                ENTITY_DATA_LOAD_NBT,
                INFO_LINES_USES_NBT,
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
                SHULKER_DISPLAY_ENDER_CHEST,
                SHULKER_DISPLAY_REQUIRE_SHIFT,
                SLIME_CHUNK_TOP_TO_PLAYER,
                SORT_LINES_BY_LENGTH,
                SORT_LINES_REVERSED,
                SPAWN_PLAYER_OUTER_OVERLAY_ENABLED,
                SPAWN_PLAYER_REDSTONE_OVERLAY_ENABLED,
                SPAWN_REAL_OUTER_OVERLAY_ENABLED,
                SPAWN_REAL_REDSTONE_OVERLAY_ENABLED,
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
                VILLAGER_OFFER_PRICE_RANGE,
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

    private static final String COLORS_KEY = Reference.MOD_ID+".config.colors";
    public static class Colors
    {
        public static final ConfigColor BEACON_RANGE_LVL1_OVERLAY_COLOR         = new ConfigColor("beaconRangeLvl1",                    "#20E060FF").apply(COLORS_KEY);
        public static final ConfigColor BEACON_RANGE_LVL2_OVERLAY_COLOR         = new ConfigColor("beaconRangeLvl2",                    "#20FFB040").apply(COLORS_KEY);
        public static final ConfigColor BEACON_RANGE_LVL3_OVERLAY_COLOR         = new ConfigColor("beaconRangeLvl3",                    "#20FFF040").apply(COLORS_KEY);
        public static final ConfigColor BEACON_RANGE_LVL4_OVERLAY_COLOR         = new ConfigColor("beaconRangeLvl4",                    "#2060FF40").apply(COLORS_KEY);
        public static final ConfigColor BLOCK_GRID_OVERLAY_COLOR                = new ConfigColor("blockGridOverlayColor",              "#80FFFFFF").apply(COLORS_KEY);
        public static final ConfigColor CONDUIT_RANGE_OVERLAY_COLOR             = new ConfigColor("conduitRange",                       "#2030FFFF").apply(COLORS_KEY);
        public static final ConfigColor LIGHT_LEVEL_MARKER_BLOCK_LIT            = new ConfigColor("lightLevelMarkerBlockLit",           "#FF209040").apply(COLORS_KEY);
        public static final ConfigColor LIGHT_LEVEL_MARKER_DARK                 = new ConfigColor("lightLevelMarkerDark",               "#FFFF4848").apply(COLORS_KEY);
        public static final ConfigColor LIGHT_LEVEL_MARKER_DIM                  = new ConfigColor("lightLevelMarkerDim",                "#FFC0C040").apply(COLORS_KEY);
        public static final ConfigColor LIGHT_LEVEL_MARKER_SKY_LIT              = new ConfigColor("lightLevelMarkerSkyLit",             "#FFFFFF33").apply(COLORS_KEY);
        public static final ConfigColor LIGHT_LEVEL_NUMBER_BLOCK_DARK           = new ConfigColor("lightLevelNumberBlockDark",          "#FFC03030").apply(COLORS_KEY);
        public static final ConfigColor LIGHT_LEVEL_NUMBER_BLOCK_DIM            = new ConfigColor("lightLevelNumberBlockDim",           "#FFC0C040").apply(COLORS_KEY);
        public static final ConfigColor LIGHT_LEVEL_NUMBER_BLOCK_LIT            = new ConfigColor("lightLevelNumberBlockLit",           "#FF20FF40").apply(COLORS_KEY);
        public static final ConfigColor LIGHT_LEVEL_NUMBER_SKY_DARK             = new ConfigColor("lightLevelNumberSkyDark",            "#FFFFF030").apply(COLORS_KEY);
        public static final ConfigColor LIGHT_LEVEL_NUMBER_SKY_DIM              = new ConfigColor("lightLevelNumberSkyDim",             "#FFC0C030").apply(COLORS_KEY);
        public static final ConfigColor LIGHT_LEVEL_NUMBER_SKY_LIT              = new ConfigColor("lightLevelNumberSkyLit",             "#FF40E0FF").apply(COLORS_KEY);
        public static final ConfigColor RANDOM_TICKS_FIXED_OVERLAY_COLOR        = new ConfigColor("randomTicksFixedOverlayColor",       "#30F9F225").apply(COLORS_KEY);
        public static final ConfigColor RANDOM_TICKS_PLAYER_OVERLAY_COLOR       = new ConfigColor("randomTicksPlayerOverlayColor",      "#3030FE73").apply(COLORS_KEY);
        public static final ConfigColor REGION_OVERLAY_COLOR                    = new ConfigColor("regionOverlayColor",                 "#30FF8019").apply(COLORS_KEY);
        public static final ConfigColor SHAPE_ADJUSTABLE_SPAWN_SPHERE           = new ConfigColor("shapeAdjustableSpawnSphere",         "#6030B0B0").apply(COLORS_KEY);
        public static final ConfigColor SHAPE_BOX                               = new ConfigColor("shapeBox",                           "#6050A0A0").apply(COLORS_KEY);
        public static final ConfigColor SHAPE_CAN_DESPAWN_SPHERE                = new ConfigColor("shapeCanDespawnSphere",              "#60A04050").apply(COLORS_KEY);
        public static final ConfigColor SHAPE_CAN_SPAWN_SPHERE                  = new ConfigColor("shapeCanSpawnSphere",                "#60A04050").apply(COLORS_KEY);
        public static final ConfigColor SHAPE_CIRCLE                            = new ConfigColor("shapeCircle",                        "#6030B0B0").apply(COLORS_KEY);
        public static final ConfigColor SHAPE_DESPAWN_SPHERE                    = new ConfigColor("shapeDespawnSphere",                 "#60A04050").apply(COLORS_KEY);
        public static final ConfigColor SHAPE_LINE_BLOCKY                       = new ConfigColor("shapeLineBlocky",                    "#6030F0B0").apply(COLORS_KEY);
        public static final ConfigColor SHAPE_SPHERE_BLOCKY                     = new ConfigColor("shapeSphereBlocky",                  "#6030B0B0").apply(COLORS_KEY);
        public static final ConfigColor SLIME_CHUNKS_OVERLAY_COLOR              = new ConfigColor("slimeChunksOverlayColor",            "#3020F020").apply(COLORS_KEY);
        public static final ConfigColor SPAWN_PLAYER_ENTITY_OVERLAY_COLOR       = new ConfigColor("spawnPlayerEntityOverlayColor",      "#302050D0").apply(COLORS_KEY);
        public static final ConfigColor SPAWN_PLAYER_REDSTONE_OVERLAY_COLOR     = new ConfigColor("spawnPlayerRedstoneOverlayColor",    "#30F8D641").apply(COLORS_KEY);
        public static final ConfigColor SPAWN_PLAYER_LAZY_OVERLAY_COLOR         = new ConfigColor("spawnPlayerLazyOverlayColor",        "#30D030D0").apply(COLORS_KEY);
        public static final ConfigColor SPAWN_PLAYER_OUTER_OVERLAY_COLOR        = new ConfigColor("spawnPlayerOuterOverlayColor",       "#306900D2").apply(COLORS_KEY);
        public static final ConfigColor SPAWN_REAL_ENTITY_OVERLAY_COLOR         = new ConfigColor("spawnRealEntityOverlayColor",        "#3030FF20").apply(COLORS_KEY);
        public static final ConfigColor SPAWN_REAL_REDSTONE_OVERLAY_COLOR       = new ConfigColor("spawnRealRedstoneOverlayColor",      "#30F8D641").apply(COLORS_KEY);
        public static final ConfigColor SPAWN_REAL_LAZY_OVERLAY_COLOR           = new ConfigColor("spawnRealLazyOverlayColor",          "#30FF3020").apply(COLORS_KEY);
        public static final ConfigColor SPAWN_REAL_OUTER_OVERLAY_COLOR          = new ConfigColor("spawnRealOuterOverlayColor",         "#309D581A").apply(COLORS_KEY);
        public static final ConfigColor SPAWNABLE_COLUMNS_OVERLAY_COLOR         = new ConfigColor("spawnableColumnHeightsOverlayColor", "#A0FF00FF").apply(COLORS_KEY);
        public static final ConfigColor TEXT_BACKGROUND_COLOR                   = new ConfigColor("textBackgroundColor",                "#A0505050").apply(COLORS_KEY);
        public static final ConfigColor TEXT_COLOR                              = new ConfigColor("textColor",                          "#FFE0E0E0").apply(COLORS_KEY);

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
                SPAWN_PLAYER_REDSTONE_OVERLAY_COLOR,
                SPAWN_PLAYER_LAZY_OVERLAY_COLOR,
                SPAWN_PLAYER_OUTER_OVERLAY_COLOR,
                SPAWN_REAL_ENTITY_OVERLAY_COLOR,
                SPAWN_REAL_REDSTONE_OVERLAY_COLOR,
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
