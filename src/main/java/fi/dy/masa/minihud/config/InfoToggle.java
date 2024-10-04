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
import fi.dy.masa.minihud.Reference;

public enum InfoToggle implements IConfigInteger, IHotkeyTogglable
{
    // Basic Info
    FPS                     ("infoFPS",                     false, ""),
    MEMORY_USAGE            ("infoMemoryUsage",             false, ""),
    TIME_REAL               ("infoTimeIRL",                 true,  ""),
    TIME_WORLD              ("infoTimeWorld",               false, ""),
    TIME_WORLD_FORMATTED    ("infoWorldTimeFormatted",      false, ""),

    // Player (Camera)
    COORDINATES             ("infoCoordinates",             true,  ""),
    COORDINATES_SCALED      ("infoCoordinatesScaled",       false, ""),
    BLOCK_POS               ("infoBlockPosition",           false, ""),
    CHUNK_POS               ("infoChunkPosition",           false, ""),
    BLOCK_IN_CHUNK          ("infoBlockInChunk",            false, ""),
    DIMENSION               ("infoDimensionId",             false, ""),
    FACING                  ("infoFacing",                  true,  ""),
    ROTATION_YAW            ("infoRotationYaw",             false, ""),
    ROTATION_PITCH          ("infoRotationPitch",           false, ""),

    // Player
    BLOCK_BREAK_SPEED       ("infoBlockBreakSpeed",         false, ""),
    PLAYER_EXPERIENCE       ("infoPlayerExperience",        false, ""),
    SPEED                   ("infoSpeed",                   false, ""),
    SPEED_AXIS              ("infoSpeedAxis",               false, ""),
    SPEED_HV                ("infoSpeedHV",                 false, ""),
    SPRINTING               ("infoSprinting",               false, ""),

    // Server
    SERVER_TPS              ("infoServerTPS",               false, ""),
    SERVUX                  ("infoServux",                  false, ""),
    PING                    ("infoPing",                    false, ""),

    // World
    WEATHER                 ("infoWeather",                 false, ""),
    TIME_TOTAL_MODULO       ("infoTimeTotalModulo",         false, ""),
    TIME_DAY_MODULO         ("infoTimeDayModulo",           false, ""),
    MOB_CAPS                ("infoMobCaps",                 false, ""),
    PARTICLE_COUNT          ("infoParticleCount",           false, ""),
    DIFFICULTY              ("infoDifficulty",              false, ""),
    ENTITIES                ("infoEntities",                false, ""),
    ENTITIES_CLIENT_WORLD   ("infoEntitiesClientWorld",     false, ""),
    TILE_ENTITIES           ("infoTileEntities",            false, ""),

    // World (Current position)
    LIGHT_LEVEL             ("infoLightLevel",              false, ""),
    BIOME                   ("infoBiome",                   false, ""),
    BIOME_REG_NAME          ("infoBiomeRegistryName",       false, ""),
    DISTANCE                ("infoDistance",                false, ""),

    // Chunk
    LOADED_CHUNKS_COUNT     ("infoLoadedChunksCount",       false, ""),
    CHUNK_SECTIONS          ("infoChunkSections",           false, ""),
    CHUNK_SECTIONS_FULL     ("infoChunkSectionsLine",       false, ""),
    CHUNK_UPDATES           ("infoChunkUpdates",            false, ""),
    REGION_FILE             ("infoRegionFile",              false, ""),
    SLIME_CHUNK             ("infoSlimeChunk",              false, ""),

    // Block
    LOOKING_AT_BLOCK        ("infoLookingAtBlock",          false, ""),
    LOOKING_AT_BLOCK_CHUNK  ("infoLookingAtBlockInChunk",   false, ""),
    BLOCK_PROPS             ("infoBlockProperties",         false, ""),
    BEE_COUNT               ("infoBeeCount",                false, ""),
    HONEY_LEVEL             ("infoHoneyLevel",              false, ""),
    FURNACE_XP              ("infoFurnaceXp",               false, ""),

    // Entity
    ENTITY_REG_NAME         ("infoEntityRegistryName",      false, ""),
    LOOKING_AT_ENTITY       ("infoLookingAtEntity",         false, ""),
    LOOKING_AT_EFFECTS      ("infoLookingAtEffects",        false, ""),
    LOOKING_AT_PLAYER_EXP   ("infoLookingAtPlayerExp",      false, ""),
    ZOMBIE_CONVERSION       ("infoZombieConversion",        false, ""),
    HORSE_SPEED             ("infoHorseSpeed",              false, ""),
    HORSE_JUMP              ("infoHorseJump",               false, ""),
    PANDA_GENE              ("infoPandaGene",               false, ""),
    ENTITY_VARIANT          ("infoEntityVariant",           false, ""),
    ;

    public static final ImmutableList<InfoToggle> VALUES = ImmutableList.copyOf(values());
    private static final String INFO_KEY = Reference.MOD_ID+".config.info_toggle";

    private final String name;
    private final String comment;
    private final String prettyName;
    private final String translatedName;
    private final IKeybind keybind;
    private final boolean defaultValueBoolean;
    private final int defaultLinePosition;
    private boolean valueBoolean;
    private int linePosition;
    static private int nextDefaultLinePosition;

    private static int getNextDefaultLinePosition()
    {
        return nextDefaultLinePosition++;
    }

    InfoToggle(String name, boolean defaultValue, String defaultHotkey)
    {
        this(name, defaultValue,
                getNextDefaultLinePosition(),
                defaultHotkey,
                buildTranslateName(name, "comment"),
                KeybindSettings.DEFAULT,
                buildTranslateName(name, "name"),
                buildTranslateName(name, "prettyName"));
    }

    InfoToggle(String name, boolean defaultValue, String defaultHotkey, KeybindSettings settings)
    {
        this(name, defaultValue,
                getNextDefaultLinePosition(),
                defaultHotkey,
                buildTranslateName(name, "comment"),
                settings,
                buildTranslateName(name, "name"),
                buildTranslateName(name, "prettyName"));
    }

    InfoToggle(String name, boolean defaultValue, int linePosition, String defaultHotkey)
    {
        this(name, defaultValue,
                linePosition,
                defaultHotkey,
                buildTranslateName(name, "comment"),
                KeybindSettings.DEFAULT,
                buildTranslateName(name, "name"),
                buildTranslateName(name, "prettyName"));
    }

    InfoToggle(String name, boolean defaultValue, int linePosition, String defaultHotkey, KeybindSettings settings)
    {
        this(name, defaultValue,
                linePosition,
                defaultHotkey,
                buildTranslateName(name, "comment"),
                settings,
                buildTranslateName(name, "name"),
                buildTranslateName(name, "prettyName"));
    }

    InfoToggle(String name, boolean defaultValue, String defaultHotkey, String comment)
    {
        this(name, defaultValue, getNextDefaultLinePosition(), defaultHotkey, comment, KeybindSettings.DEFAULT, buildTranslateName(name, "name"), name);
    }

    InfoToggle(String name, boolean defaultValue, String defaultHotkey, String comment, String translatedName)
    {
        this(name, defaultValue, getNextDefaultLinePosition(), defaultHotkey, comment, KeybindSettings.DEFAULT, translatedName, name);
    }

    InfoToggle(String name, boolean defaultValue, int linePosition, String defaultHotkey, String comment, KeybindSettings settings, String translatedName, String prettyName)
    {
        this.name = name;
        this.valueBoolean = defaultValue;
        this.defaultValueBoolean = defaultValue;
        this.keybind = KeybindMulti.fromStorageString(defaultHotkey, settings);
        this.keybind.setCallback(new KeyCallbackToggleBoolean(this));
        this.defaultLinePosition = linePosition;
        this.comment = comment;
        this.prettyName = prettyName;
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
        return StringUtils.getTranslatedOrFallback(this.prettyName, this.prettyName.isEmpty() ? this.name : this.prettyName);
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
        return StringUtils.getTranslatedOrFallback(this.comment, this.comment.isEmpty() ? this.name : this.comment);
    }

    @Override
    public String getTranslatedName()
    {
        return this.translatedName.isEmpty() ? this.name : this.translatedName;
    }

    private static String buildTranslateName(String name, String type)
    {
        return INFO_KEY + "." + type + "." + name;
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
