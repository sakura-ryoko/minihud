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
    // Basic Info
    FPS                     ("infoFPS",                     false, "", "minihud.config.info_toggle.comment.infoFPS", "minihud.config.info_toggle.name.infoFPS"),
    MEMORY_USAGE            ("infoMemoryUsage",             false, "", "minihud.config.info_toggle.comment.infoMemoryUsage", "minihud.config.info_toggle.name.infoMemoryUsage"),
    TIME_REAL               ("infoTimeIRL",                 true,  "", "minihud.config.info_toggle.comment.infoTimeIRL", "minihud.config.info_toggle.name.infoTimeIRL"),
    TIME_WORLD              ("infoTimeWorld",               false, "", "minihud.config.info_toggle.comment.infoTimeWorld", "minihud.config.info_toggle.name.infoTimeWorld"),
    TIME_WORLD_FORMATTED    ("infoWorldTimeFormatted",      false, "", "minihud.config.info_toggle.comment.infoWorldTimeFormatted", "minihud.config.info_toggle.name.infoWorldTimeFormatted"),

    // Player (Camera)
    COORDINATES             ("infoCoordinates",             true,  "", "minihud.config.info_toggle.comment.infoCoordinates", "minihud.config.info_toggle.name.infoCoordinates"),
    COORDINATES_SCALED      ("infoCoordinatesScaled",       false, "", "minihud.config.info_toggle.comment.infoCoordinatesScaled", "minihud.config.info_toggle.name.infoCoordinatesScaled"),
    BLOCK_POS               ("infoBlockPosition",           false, "", "minihud.config.info_toggle.comment.infoBlockPosition", "minihud.config.info_toggle.name.infoBlockPosition"),
    CHUNK_POS               ("infoChunkPosition",           false, "", "minihud.config.info_toggle.comment.infoChunkPosition", "minihud.config.info_toggle.name.infoChunkPosition"),
    BLOCK_IN_CHUNK          ("infoBlockInChunk",            false, "", "minihud.config.info_toggle.comment.infoBlockInChunk", "minihud.config.info_toggle.name.infoBlockInChunk"),
    DIMENSION               ("infoDimensionId",             false, "", "minihud.config.info_toggle.comment.infoDimensionId", "minihud.config.info_toggle.name.infoDimensionId"),
    FACING                  ("infoFacing",                  true,  "", "minihud.config.info_toggle.comment.infoFacing", "minihud.config.info_toggle.name.infoFacing"),
    ROTATION_YAW            ("infoRotationYaw",             false, "", "minihud.config.info_toggle.comment.infoRotationYaw", "minihud.config.info_toggle.name.infoRotationYaw"),
    ROTATION_PITCH          ("infoRotationPitch",           false, "", "minihud.config.info_toggle.comment.infoRotationPitch", "minihud.config.info_toggle.name.infoRotationPitch"),

    // Player
    BLOCK_BREAK_SPEED       ("infoBlockBreakSpeed",         false, "", "minihud.config.info_toggle.comment.infoBlockBreakSpeed", "minihud.config.info_toggle.name.infoBlockBreakSpeed"),
    SPEED                   ("infoSpeed",                   false, "", "minihud.config.info_toggle.comment.infoSpeed", "minihud.config.info_toggle.name.infoSpeed"),
    SPEED_AXIS              ("infoSpeedAxis",               false, "", "minihud.config.info_toggle.comment.infoSpeedAxis", "minihud.config.info_toggle.name.infoSpeedAxis"),
    SPEED_HV                ("infoSpeedHV",                 false, "", "minihud.config.info_toggle.comment.infoSpeedHV", "minihud.config.info_toggle.name.infoSpeedHV"),
    SPRINTING               ("infoSprinting",               false, "", "minihud.config.info_toggle.comment.infoSprinting", "minihud.config.info_toggle.name.infoSprinting"),

    // Server
    SERVER_TPS              ("infoServerTPS",               false, "", "minihud.config.info_toggle.comment.infoServerTPS", "minihud.config.info_toggle.name.infoServerTPS"),
    SERVUX                  ("infoServux",                  false, "", "minihud.config.info_toggle.comment.infoServux", "minihud.config.info_toggle.name.infoServux"),
    PING                    ("infoPing",                    false, "", "minihud.config.info_toggle.comment.infoPing", "minihud.config.info_toggle.name.infoPing"),

    // World
    WEATHER                 ("infoWeather",                 false, "", "minihud.config.info_toggle.comment.infoWeather", "minihud.config.info_toggle.name.infoWeather"),
    TIME_TOTAL_MODULO       ("infoTimeTotalModulo",         false, "", "minihud.config.info_toggle.comment.infoTimeTotalModulo", "minihud.config.info_toggle.name.infoTimeTotalModulo"),
    TIME_DAY_MODULO         ("infoTimeDayModulo",           false, "", "minihud.config.info_toggle.comment.infoTimeDayModulo", "minihud.config.info_toggle.name.infoTimeDayModulo"),
    MOB_CAPS                ("infoMobCaps",                 false, "", "minihud.config.info_toggle.comment.infoMobCaps", "minihud.config.info_toggle.name.infoMobCaps"),
    PARTICLE_COUNT          ("infoParticleCount",           false, "", "minihud.config.info_toggle.comment.infoParticleCount", "minihud.config.info_toggle.name.infoParticleCount"),
    DIFFICULTY              ("infoDifficulty",              false, "", "minihud.config.info_toggle.comment.infoDifficulty", "minihud.config.info_toggle.name.infoDifficulty"),
    ENTITIES                ("infoEntities",                false, "", "minihud.config.info_toggle.comment.infoEntities", "minihud.config.info_toggle.name.infoEntities"),
    ENTITIES_CLIENT_WORLD   ("infoEntitiesClientWorld",     false, "", "minihud.config.info_toggle.comment.infoEntitiesClientWorld", "minihud.config.info_toggle.name.infoEntitiesClientWorld"),
    TILE_ENTITIES           ("infoTileEntities",            false, "", "minihud.config.info_toggle.comment.infoTileEntities", "minihud.config.info_toggle.name.infoTileEntities"),

    // World (Current position)
    LIGHT_LEVEL             ("infoLightLevel",              false, "", "minihud.config.info_toggle.comment.infoLightLevel", "minihud.config.info_toggle.name.infoLightLevel"),
    BIOME                   ("infoBiome",                   false, "", "minihud.config.info_toggle.comment.infoBiome", "minihud.config.info_toggle.name.infoBiome"),
    BIOME_REG_NAME          ("infoBiomeRegistryName",       false, "", "minihud.config.info_toggle.comment.infoBiomeRegistryName", "minihud.config.info_toggle.name.infoBiomeRegistryName"),
    DISTANCE                ("infoDistance",                false, "", "minihud.config.info_toggle.comment.infoDistance", "minihud.config.info_toggle.name.infoDistance"),

    // Chunk
    LOADED_CHUNKS_COUNT     ("infoLoadedChunksCount",       false, "", "minihud.config.info_toggle.comment.infoLoadedChunksCount", "minihud.config.info_toggle.name.infoLoadedChunksCount"),
    CHUNK_SECTIONS          ("infoChunkSections",           false, "", "minihud.config.info_toggle.comment.infoChunkSections", "minihud.config.info_toggle.name.infoChunkSections"),
    CHUNK_SECTIONS_FULL     ("infoChunkSectionsLine",       false, "", "minihud.config.info_toggle.comment.infoChunkSectionsLine", "minihud.config.info_toggle.name.infoChunkSectionsLine"),
    CHUNK_UPDATES           ("infoChunkUpdates",            false, "", "minihud.config.info_toggle.comment.infoChunkUpdates", "minihud.config.info_toggle.name.infoChunkUpdates"),
    REGION_FILE             ("infoRegionFile",              false, "", "minihud.config.info_toggle.comment.infoRegionFile", "minihud.config.info_toggle.name.infoRegionFile"),
    SLIME_CHUNK             ("infoSlimeChunk",              false, "", "minihud.config.info_toggle.comment.infoSlimeChunk", "minihud.config.info_toggle.name.infoSlimeChunk"),

    // Block
    LOOKING_AT_BLOCK        ("infoLookingAtBlock",          false, "", "minihud.config.info_toggle.comment.infoLookingAtBlock", "minihud.config.info_toggle.name.infoLookingAtBlock"),
    LOOKING_AT_BLOCK_CHUNK  ("infoLookingAtBlockInChunk",   false, "", "minihud.config.info_toggle.comment.infoLookingAtBlockInChunk", "minihud.config.info_toggle.name.infoLookingAtBlockInChunk"),
    BLOCK_PROPS             ("infoBlockProperties",         false, "", "minihud.config.info_toggle.comment.infoBlockProperties", "minihud.config.info_toggle.name.infoBlockProperties"),
    BEE_COUNT               ("infoBeeCount",                false, "", "minihud.config.info_toggle.comment.infoBeeCount", "minihud.config.info_toggle.name.infoBeeCount"),
    HONEY_LEVEL             ("infoHoneyLevel",              false, "", "minihud.config.info_toggle.comment.infoHoneyLevel", "minihud.config.info_toggle.name.infoHoneyLevel"),
    FURNACE_XP              ("infoFurnaceXp",               false, "", "minihud.config.info_toggle.comment.infoFurnaceXp", "minihud.config.info_toggle.name.infoFurnaceXp"),

    // Entity
    LOOKING_AT_ENTITY       ("infoLookingAtEntity",         false, "", "minihud.config.info_toggle.comment.infoLookingAtEntity", "minihud.config.info_toggle.name.infoLookingAtEntity"),
    ENTITY_REG_NAME         ("infoEntityRegistryName",      false, "", "minihud.config.info_toggle.comment.infoEntityRegistryName", "minihud.config.info_toggle.name.infoEntityRegistryName"),
    LOOKING_AT_EFFECTS      ("infoLookingAtEffects",        false, "", "minihud.config.info_toggle.comment.infoLookingAtEffects", "minihud.config.info_toggle.name.infoLookingAtEffects"),
    ZOMBIE_CONVERSION       ("infoZombieConversion",        false, "", "minihud.config.info_toggle.comment.infoZombieConversion", "minihud.config.info_toggle.name.infoZombieConversion"),
    HORSE_SPEED             ("infoHorseSpeed",              false, "", "minihud.config.info_toggle.comment.infoHorseSpeed", "minihud.config.info_toggle.name.infoHorseSpeed"),
    HORSE_JUMP              ("infoHorseJump",               false, "", "minihud.config.info_toggle.comment.infoHorseJump", "minihud.config.info_toggle.name.infoHorseJump"),
    PANDA_GENE              ("infoPandaGene",               false, "", "minihud.config.info_toggle.comment.infoPandaGene", "minihud.config.info_toggle.name.infoPandaGene"),
    ;

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
    static private int nextDefaultLinePosition;

    private static int getNextDefaultLinePosition()
    {
        return nextDefaultLinePosition++;
    }

    InfoToggle(String name, boolean defaultValue, String defaultHotkey, String comment, String translatedName)
    {
        this(name, defaultValue, getNextDefaultLinePosition(), defaultHotkey, comment, KeybindSettings.DEFAULT, translatedName);
    }

    InfoToggle(String name, boolean defaultValue, int linePosition, String defaultHotkey, String comment, KeybindSettings settings, String translatedName)
    {
        this.name = name;
        this.valueBoolean = defaultValue;
        this.defaultValueBoolean = defaultValue;
        this.keybind = KeybindMulti.fromStorageString(defaultHotkey, settings);
        this.keybind.setCallback(new KeyCallbackToggleBoolean(this));
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
