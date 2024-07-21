package fi.dy.masa.minihud.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.IConfigInteger;
import fi.dy.masa.malilib.config.IHotkeyTogglable;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyCallbackToggleBoolean;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.minihud.MiniHUD;

public enum InfoToggle implements IConfigInteger, IHotkeyTogglable
{
    BEE_COUNT               ("infoBeeCount",                false, 36, "", "minihud.config.info_toggle.comment.infoBeeCount", "minihud.config.info_toggle.name.infoBeeCount"),
    BIOME                   ("infoBiome",                   false, 19, "", "minihud.config.info_toggle.comment.infoBiome", "minihud.config.info_toggle.name.infoBiome"),
    BIOME_REG_NAME          ("infoBiomeRegistryName",       false, 20, "", "minihud.config.info_toggle.comment.infoBiomeRegistryName", "minihud.config.info_toggle.name.infoBiomeRegistryName"),
    BLOCK_BREAK_SPEED       ("infoBlockBreakSpeed",         false,  6, "", "minihud.config.info_toggle.comment.infoBlockBreakSpeed", "minihud.config.info_toggle.name.infoBlockBreakSpeed"),
    BLOCK_IN_CHUNK          ("infoBlockInChunk",            false, 28, "", "minihud.config.info_toggle.comment.infoBlockInChunk", "minihud.config.info_toggle.name.infoBlockInChunk"),
    BLOCK_POS               ("infoBlockPosition",           false,  6, "", "minihud.config.info_toggle.comment.infoBlockPosition", "minihud.config.info_toggle.name.infoBlockPosition"),
    BLOCK_PROPS             ("infoBlockProperties",         false, 27, "", "minihud.config.info_toggle.comment.infoBlockProperties", "minihud.config.info_toggle.name.infoBlockProperties"),
    CHUNK_POS               ("infoChunkPosition",           false,  7, "", "minihud.config.info_toggle.comment.infoChunkPosition", "minihud.config.info_toggle.name.infoChunkPosition"),
    CHUNK_SECTIONS          ("infoChunkSections",           false, 14, "", "minihud.config.info_toggle.comment.infoChunkSections", "minihud.config.info_toggle.name.infoChunkSections"),
    CHUNK_SECTIONS_FULL     ("infoChunkSectionsLine",       false, 15, "", "minihud.config.info_toggle.comment.infoChunkSectionsLine", "minihud.config.info_toggle.name.infoChunkSectionsLine"),
    CHUNK_UPDATES           ("infoChunkUpdates",            false, 16, "", "minihud.config.info_toggle.comment.infoChunkUpdates", "minihud.config.info_toggle.name.infoChunkUpdates"),
    COORDINATES             ("infoCoordinates",             true,   4, "", "minihud.config.info_toggle.comment.infoCoordinates", "minihud.config.info_toggle.name.infoCoordinates"),
    COORDINATES_SCALED      ("infoCoordinatesScaled",       false,  4, "", "minihud.config.info_toggle.comment.infoCoordinatesScaled", "minihud.config.info_toggle.name.infoCoordinatesScaled"),
    DIFFICULTY              ("infoDifficulty",              false, 18, "", "minihud.config.info_toggle.comment.infoDifficulty", "minihud.config.info_toggle.name.infoDifficulty"),
    DIMENSION               ("infoDimensionId",             false,  5, "", "minihud.config.info_toggle.comment.infoDimensionId", "minihud.config.info_toggle.name.infoDimensionId"),
    DISTANCE                ("infoDistance",                false, 33, "", "minihud.config.info_toggle.comment.infoDistance", "minihud.config.info_toggle.name.infoDistance"),
    ENTITIES                ("infoEntities",                false, 21, "", "minihud.config.info_toggle.comment.infoEntities", "minihud.config.info_toggle.name.infoEntities"),
    ENTITIES_CLIENT_WORLD   ("infoEntitiesClientWorld",     false, 22, "", "minihud.config.info_toggle.comment.infoEntitiesClientWorld", "minihud.config.info_toggle.name.infoEntitiesClientWorld"),
    ENTITY_REG_NAME         ("infoEntityRegistryName",      false, 24, "", "minihud.config.info_toggle.comment.infoEntityRegistryName", "minihud.config.info_toggle.name.infoEntityRegistryName"),
    FACING                  ("infoFacing",                  true,   8, "", "minihud.config.info_toggle.comment.infoFacing", "minihud.config.info_toggle.name.infoFacing"),
    FURNACE_XP              ("infoFurnaceXp",               false, 30, "", "minihud.config.info_toggle.comment.infoFurnaceXp", "minihud.config.info_toggle.name.infoFurnaceXp"),
    FPS                     ("infoFPS",                     false,  0, "", "minihud.config.info_toggle.comment.infoFPS", "minihud.config.info_toggle.name.infoFPS"),
    HONEY_LEVEL             ("infoHoneyLevel",              false, 37, "", "minihud.config.info_toggle.comment.infoHoneyLevel", "minihud.config.info_toggle.name.infoHoneyLevel"),
    HORSE_SPEED             ("infoHorseSpeed",              false, 36, "", "minihud.config.info_toggle.comment.infoHorseSpeed", "minihud.config.info_toggle.name.infoHorseSpeed"),
    HORSE_JUMP              ("infoHorseJump",               false, 37, "", "minihud.config.info_toggle.comment.infoHorseJump", "minihud.config.info_toggle.name.infoHorseJump"),
    LIGHT_LEVEL             ("infoLightLevel",              false, 10, "", "minihud.config.info_toggle.comment.infoLightLevel", "minihud.config.info_toggle.name.infoLightLevel"),
    LOOKING_AT_BLOCK        ("infoLookingAtBlock",          false, 25, "", "minihud.config.info_toggle.comment.infoLookingAtBlock", "minihud.config.info_toggle.name.infoLookingAtBlock"),
    LOOKING_AT_BLOCK_CHUNK  ("infoLookingAtBlockInChunk",   false, 26, "", "minihud.config.info_toggle.comment.infoLookingAtBlockInChunk", "minihud.config.info_toggle.name.infoLookingAtBlockInChunk"),
    LOOKING_AT_ENTITY       ("infoLookingAtEntity",         false, 23, "", "minihud.config.info_toggle.comment.infoLookingAtEntity", "minihud.config.info_toggle.name.infoLookingAtEntity"),
    MEMORY_USAGE            ("infoMemoryUsage",             false,  0, "", "minihud.config.info_toggle.comment.infoMemoryUsage", "minihud.config.info_toggle.name.infoMemoryUsage"),
    MOB_CAPS                ("infoMobCaps",                 false, 10, "", "minihud.config.info_toggle.comment.infoMobCaps", "minihud.config.info_toggle.name.infoMobCaps"),
    LOADED_CHUNKS_COUNT     ("infoLoadedChunksCount",       false, 31, "", "minihud.config.info_toggle.comment.infoLoadedChunksCount", "minihud.config.info_toggle.name.infoLoadedChunksCount"),
    PANDA_GENE              ("infoPandaGene",               false, 37, "", "minihud.config.info_toggle.comment.infoPandaGene", "minihud.config.info_toggle.name.infoPandaGene"),
    PARTICLE_COUNT          ("infoParticleCount",           false, 17, "", "minihud.config.info_toggle.comment.infoParticleCount", "minihud.config.info_toggle.name.infoParticleCount"),
    PING                    ("infoPing",                    false, 36, "", "minihud.config.info_toggle.comment.infoPing", "minihud.config.info_toggle.name.infoPing"),
    REGION_FILE             ("infoRegionFile",              false, 29, "", "minihud.config.info_toggle.comment.infoRegionFile", "minihud.config.info_toggle.name.infoRegionFile"),
    ROTATION_PITCH          ("infoRotationPitch",           false, 12, "", "minihud.config.info_toggle.comment.infoRotationPitch", "minihud.config.info_toggle.name.infoRotationPitch"),
    ROTATION_YAW            ("infoRotationYaw",             false, 11, "", "minihud.config.info_toggle.comment.infoRotationYaw", "minihud.config.info_toggle.name.infoRotationYaw"),
    SERVER_TPS              ("infoServerTPS",               false,  9, "", "minihud.config.info_toggle.comment.infoServerTPS", "minihud.config.info_toggle.name.infoServerTPS"),
    SERVUX                  ("infoServux",                  false, 10, "", "minihud.config.info_toggle.comment.infoServux", "minihud.config.info_toggle.name.infoServux"),
    SLIME_CHUNK             ("infoSlimeChunk",              false, 22, "", "minihud.config.info_toggle.comment.infoSlimeChunk", "minihud.config.info_toggle.name.infoSlimeChunk"),
    SPEED                   ("infoSpeed",                   false, 13, "", "minihud.config.info_toggle.comment.infoSpeed", "minihud.config.info_toggle.name.infoSpeed"),
    SPEED_AXIS              ("infoSpeedAxis",               false, 13, "", "minihud.config.info_toggle.comment.infoSpeedAxis", "minihud.config.info_toggle.name.infoSpeedAxis"),
    SPEED_HV                ("infoSpeedHV",                 false, 13, "", "minihud.config.info_toggle.comment.infoSpeedHV", "minihud.config.info_toggle.name.infoSpeedHV"),
    SPRINTING               ("infoSprinting",               false, 40, "", "minihud.config.info_toggle.comment.infoSprinting", "minihud.config.info_toggle.name.infoSprinting"),
    TILE_ENTITIES           ("infoTileEntities",            false, 32, "", "minihud.config.info_toggle.comment.infoTileEntities", "minihud.config.info_toggle.name.infoTileEntities"),
    TIME_DAY_MODULO         ("infoTimeDayModulo",           false, 35, "", "minihud.config.info_toggle.comment.infoTimeDayModulo", "minihud.config.info_toggle.name.infoTimeDayModulo"),
    TIME_REAL               ("infoTimeIRL",                 true,   1, "", "minihud.config.info_toggle.comment.infoTimeIRL", "minihud.config.info_toggle.name.infoTimeIRL"),
    TIME_TOTAL_MODULO       ("infoTimeTotalModulo",         false, 34, "", "minihud.config.info_toggle.comment.infoTimeTotalModulo", "minihud.config.info_toggle.name.infoTimeTotalModulo"),
    TIME_WORLD              ("infoTimeWorld",               false,  2, "", "minihud.config.info_toggle.comment.infoTimeWorld", "minihud.config.info_toggle.name.infoTimeWorld"),
    TIME_WORLD_FORMATTED    ("infoWorldTimeFormatted",      false,  3, "", "minihud.config.info_toggle.comment.infoWorldTimeFormatted", "minihud.config.info_toggle.name.infoWorldTimeFormatted"),
    WEATHER                 ("infoWeather",                 false, 4, "", "minihud.config.info_toggle.comment.infoWeather", "minihud.config.info_toggle.name.infoWeather"),;

    public static final ImmutableList<InfoToggle> VALUES = ImmutableList.copyOf(values());

    private final String name;
    private final String comment;
    private final String prettyName;
    private final String translatedName;
    private final IKeybind keybind;
    private final boolean defaultValueBoolean;
    private final int defaultLinePosition;
    private boolean valueBoolean;
    private int linePosition;

    InfoToggle(String name, boolean defaultValue, int linePosition, String defaultHotkey, String comment)
    {
        this(name, defaultValue, linePosition, defaultHotkey, comment, KeybindSettings.DEFAULT);
    }

    InfoToggle(String name, boolean defaultValue, int linePosition, String defaultHotkey, String comment, KeybindSettings settings)
    {
        this(name, defaultValue, linePosition, defaultHotkey, comment, settings, name);
    }

    InfoToggle(String name, boolean defaultValue, int linePosition, String defaultHotkey, String comment, String translatedName)
    {
        this(name, defaultValue, linePosition, defaultHotkey, comment, KeybindSettings.DEFAULT, translatedName);
    }

    InfoToggle(String name, boolean defaultValue, int linePosition, String defaultHotkey, String comment, KeybindSettings settings, String translatedName)
    {
        this.name = name;
        this.valueBoolean = defaultValue;
        this.defaultValueBoolean = defaultValue;
        this.keybind = KeybindMulti.fromStorageString(defaultHotkey, settings);
        this.keybind.setCallback(new KeyCallbackToggleBoolean(this));
        this.linePosition = linePosition;
        this.defaultLinePosition = linePosition;
        this.comment = comment;
        this.prettyName = name;
        this.translatedName = translatedName;
    }

    @Override
    public ConfigType getType()
    {
        return ConfigType.HOTKEY;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public String getPrettyName()
    {
        return this.prettyName;
    }

    @Override
    public String getStringValue()
    {
        return String.valueOf(this.valueBoolean);
    }

    @Override
    public String getDefaultStringValue()
    {
        return String.valueOf(this.defaultValueBoolean);
    }

    @Override
    public String getComment()
    {
        return StringUtils.getTranslatedOrFallback("config.comment." + this.getName().toLowerCase(), this.comment);
    }

    @Override
    public String getTranslatedName()
    {
        return this.translatedName;
    }

    @Override
    public boolean getBooleanValue()
    {
        return this.valueBoolean;
    }

    @Override
    public boolean getDefaultBooleanValue()
    {
        return this.defaultValueBoolean;
    }

    @Override
    public void setBooleanValue(boolean value)
    {
        this.valueBoolean = value;
    }

    @Override
    public int getIntegerValue()
    {
        return this.linePosition;
    }

    @Override
    public int getDefaultIntegerValue()
    {
        return this.defaultLinePosition;
    }

    @Override
    public void setIntegerValue(int value)
    {
        this.linePosition = value;
    }

    @Override
    public int getMinIntegerValue()
    {
        return 0;
    }

    @Override
    public int getMaxIntegerValue()
    {
        return InfoToggle.values().length - 1;
    }

    @Override
    public IKeybind getKeybind()
    {
        return this.keybind;
    }

    @Override
    public boolean isModified()
    {
        return this.valueBoolean != this.defaultValueBoolean;
    }

    @Override
    public boolean isModified(String newValue)
    {
        return String.valueOf(this.defaultValueBoolean).equals(newValue) == false;
    }

    @Override
    public void resetToDefault()
    {
        this.valueBoolean = this.defaultValueBoolean;
    }

    @Override
    public void setValueFromString(String value)
    {
        try
        {
            this.valueBoolean = Boolean.parseBoolean(value);
        }
        catch (Exception e)
        {
            MiniHUD.logger.warn("Failed to read config value for {} from the JSON config", this.getName(), e);
        }
    }

    @Override
    public void setValueFromJsonElement(JsonElement element)
    {
        try
        {
            if (element.isJsonPrimitive())
            {
                this.valueBoolean = element.getAsBoolean();
            }
            else
            {
                MiniHUD.logger.warn("Failed to read config value for {} from the JSON config", this.getName());
            }
        }
        catch (Exception e)
        {
            MiniHUD.logger.warn("Failed to read config value for {} from the JSON config", this.getName(), e);
        }
    }

    @Override
    public JsonElement getAsJsonElement()
    {
        return new JsonPrimitive(this.valueBoolean);
    }
}
