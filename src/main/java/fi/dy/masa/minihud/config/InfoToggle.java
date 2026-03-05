package fi.dy.masa.minihud.config;

import java.util.List;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import fi.dy.masa.malilib.config.*;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyCallbackToggleBoolean;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import fi.dy.masa.malilib.interfaces.IValueChangeCallback;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.data.HudDataManager;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineFlag;
import fi.dy.masa.minihud.info.InfoLineType;
import fi.dy.masa.minihud.info.InfoLineTypes;

public enum InfoToggle implements IConfigInteger, IEnumBooleanHotkey
{
    // Basic Info
    FPS                     ("infoFPS",                     InfoLineTypes.FPS, false, ""),
//	GPU                     ("infoGPU",                     InfoLineTypes.GPU, false, ""),
    MEMORY_USAGE            ("infoMemoryUsage",             InfoLineTypes.MEMORY, false, ""),
    TIME_REAL               ("infoTimeIRL",                 InfoLineTypes.TIME_IRL, true,  ""),
    TIME_WORLD              ("infoTimeWorld",               InfoLineTypes.TIME_WORLD, false, ""),
    TIME_WORLD_FORMATTED    ("infoWorldTimeFormatted",      InfoLineTypes.TIME_WORLD_FORMATTED, false, ""),

    // Player (Camera)
    COORDINATES             ("infoCoordinates",             InfoLineTypes.COORDINATES, true,  ""),
    COORDINATES_SCALED      ("infoCoordinatesScaled",       InfoLineTypes.COORDINATES_SCALED, false, ""),
    BLOCK_POS               ("infoBlockPosition",           InfoLineTypes.BLOCK_POS, false, ""),
    CHUNK_POS               ("infoChunkPosition",           InfoLineTypes.CHUNK_POS, false, ""),
    BLOCK_IN_CHUNK          ("infoBlockInChunk",            InfoLineTypes.BLOCK_IN_CHUNK, false, ""),
    DIMENSION               ("infoDimensionId",             InfoLineTypes.DIMENSION, false, ""),
    FACING                  ("infoFacing",                  InfoLineTypes.FACING, true,  ""),
    ROTATION_YAW            ("infoRotationYaw",             InfoLineTypes.ROTATION_YAW, false, ""),
    ROTATION_PITCH          ("infoRotationPitch",           InfoLineTypes.ROTATION_PITCH, false, ""),

    // Player
    BLOCK_BREAK_SPEED       ("infoBlockBreakSpeed",         InfoLineTypes.BLOCK_BREAK_SPEED, false, ""),
    PLAYER_EXPERIENCE       ("infoPlayerExperience",        InfoLineTypes.PLAYER_EXP, false, ""),
    SPEED                   ("infoSpeed",                   InfoLineTypes.SPEED, false, ""),
    SPEED_AXIS              ("infoSpeedAxis",               InfoLineTypes.SPEED_AXIS, false, ""),
    SPEED_HV                ("infoSpeedHV",                 InfoLineTypes.SPEED_HV, false, ""),
    SPRINTING               ("infoSprinting",               InfoLineTypes.SPRINTING, false, ""),
    SCULK_WARNING_LEVEL     ("infoSculkWarningLevel",       InfoLineTypes.SCULK_WARNING_LEVEL, false, true, ""),

    // Server
    SERVER_TPS              ("infoServerTPS",               InfoLineTypes.SERVER_TPS, false, ""),
    SERVUX                  ("infoServux",                  InfoLineTypes.SERVUX, false, true, ""),
    PING                    ("infoPing",                    InfoLineTypes.PING, false, ""),

    // World
    WEATHER                 ("infoWeather",                 InfoLineTypes.WEATHER, false, true, ""),
    TIME_TOTAL_MODULO       ("infoTimeTotalModulo",         InfoLineTypes.TIME_TOTAL_MODULO, false, ""),
    TIME_DAY_MODULO         ("infoTimeDayModulo",           InfoLineTypes.TIME_DAY_MODULO, false, ""),
    MOB_CAPS                ("infoMobCaps",                 InfoLineTypes.MOB_CAPS, false, true,""),
    PARTICLE_COUNT          ("infoParticleCount",           InfoLineTypes.PARTICLE_COUNT, false, ""),
    DIFFICULTY              ("infoDifficulty",              InfoLineTypes.DIFFICULTY, false, ""),
    ENTITIES                ("infoEntities",                InfoLineTypes.ENTITIES, false, ""),
    ENTITIES_CLIENT_WORLD   ("infoEntitiesClientWorld",     InfoLineTypes.ENTITIES_CLIENT_WORLD, false, ""),
    TILE_ENTITIES           ("infoTileEntities",            InfoLineTypes.TILE_ENTITIES, false, ""),

    // World (Current position)
    LIGHT_LEVEL             ("infoLightLevel",              InfoLineTypes.LIGHT_LEVEL, false, ""),
    BIOME                   ("infoBiome",                   InfoLineTypes.BIOME, false, ""),
    BIOME_REG_NAME          ("infoBiomeRegistryName",       InfoLineTypes.BIOME_REG_NAME, false, ""),
    DISTANCE                ("infoDistance",                InfoLineTypes.DISTANCE, false, ""),

    // Chunk
    LOADED_CHUNKS_COUNT     ("infoLoadedChunksCount",       InfoLineTypes.LOADED_CHUNKS, false, ""),
    CHUNK_SECTIONS          ("infoChunkSections",           InfoLineTypes.CHUNK_SECTIONS, false, ""),
    CHUNK_SECTIONS_FULL     ("infoChunkSectionsLine",       InfoLineTypes.CHUNK_SECTIONS_FULL, false, ""),
    CHUNK_UPDATES           ("infoChunkUpdates",            InfoLineTypes.CHUNK_UPDATES, false, ""),
    REGION_FILE             ("infoRegionFile",              InfoLineTypes.REGION_FILE, false, ""),
    SLIME_CHUNK             ("infoSlimeChunk",              InfoLineTypes.SLIME_CHUNK, false, ""),

    // Block
    LOOKING_AT_BLOCK        ("infoLookingAtBlock",          InfoLineTypes.LOOKING_AT_BLOCK, false, ""),
    LOOKING_AT_BLOCK_CHUNK  ("infoLookingAtBlockInChunk",   InfoLineTypes.LOOKING_AT_CHUNK, false, ""),
    BLOCK_PROPS             ("infoBlockProperties",         InfoLineTypes.BLOCK_PROPS, false, ""),
    BEE_COUNT               ("infoBeeCount",                InfoLineTypes.BEE_COUNT, false, true, ""),
    COMPARATOR_OUTPUT       ("infoComparatorOutput",        InfoLineTypes.COMPARATOR, false, true, ""),
    HONEY_LEVEL             ("infoHoneyLevel",              InfoLineTypes.HONEY_LEVEL, false, ""),
    FURNACE_XP              ("infoFurnaceXp",               InfoLineTypes.FURNACE_EXP, false, true, ""),

    // Entity
    ENTITY_REG_NAME         ("infoEntityRegistryName",      InfoLineTypes.ENTITY_REG, false, ""),
    LOOKING_AT_ENTITY       ("infoLookingAtEntity",         InfoLineTypes.LOOKING_AT_ENTITY, false, ""),
    LOOKING_AT_EFFECTS      ("infoLookingAtEffects",        InfoLineTypes.LOOKING_AT_EFFECTS, false, ""),
    LOOKING_AT_PLAYER_EXP   ("infoLookingAtPlayerExp",      InfoLineTypes.LOOKING_AT_PLAYER_EXP, false, ""),
    ZOMBIE_CONVERSION       ("infoZombieConversion",        InfoLineTypes.ZOMBIE_CONVERSION, false, ""),
    HORSE_SPEED             ("infoHorseSpeed",              InfoLineTypes.HORSE_SPEED, false, ""),
    HORSE_JUMP              ("infoHorseJump",               InfoLineTypes.HORSE_JUMP, false, ""),
    HORSE_MAX_HEALTH        ("infoHorseMaxHealth",          InfoLineTypes.HORSE_MAX_HEALTH, false, ""),
    PANDA_GENE              ("infoPandaGene",               InfoLineTypes.PANDA_GENE, false, ""),
    DOLPHIN_TREASURE        ("infoDolphinTreasure",         InfoLineTypes.DOLPHIN_TREASURE, false, ""),
    ENTITY_VARIANT          ("infoEntityVariant",           InfoLineTypes.ENTITY_VARIANT, false, ""),
    ENTITY_HOME_POS         ("infoEntityHomePos",           InfoLineTypes.HOME_POS, false, ""),
	ENTITY_COPPER_AGING		("infoEntityCopperAging",       InfoLineTypes.COPPER_AGING, false, ""),
    ;

    public static final ImmutableList<@NotNull InfoToggle> VALUES = ImmutableList.copyOf(values());
    private static final String INFO_KEY = Reference.MOD_ID+".config.info_toggle";

    private final String name;
    private final InfoLineType<?> type;
    private String comment;
    private String prettyName;
    private String translatedName;
    private final IKeybind keybind;
    private final boolean defaultValueBoolean;
    private final int defaultLinePosition;
    private boolean valueBoolean;
    private int linePosition;
    static private int nextDefaultLinePosition;
    private final boolean serverDataRequired;
    private boolean dirty = false;
    private Pair<Boolean, String> lastBooleanHotkey;
    private int lastInteger;

    private static int getNextDefaultLinePosition()
    {
        return nextDefaultLinePosition++;
    }

    InfoToggle(String name, InfoLineType<?> type, boolean defaultValue, String defaultHotkey)
    {
        this(name, type,
             defaultValue, false,
             getNextDefaultLinePosition(),
             defaultHotkey,
             buildTranslateName(name, "comment"),
             KeybindSettings.DEFAULT,
             buildTranslateName(name, "name"),
             buildTranslateName(name, "prettyName"));
    }

    InfoToggle(String name, InfoLineType<?> type, boolean defaultValue, boolean serverDataRequired, String defaultHotkey)
    {
        this(name, type,
             defaultValue, serverDataRequired,
             getNextDefaultLinePosition(),
             defaultHotkey,
             buildTranslateName(name, "comment"),
             KeybindSettings.DEFAULT,
             buildTranslateName(name, "name"),
             buildTranslateName(name, "prettyName"));
    }

    InfoToggle(String name, InfoLineType<?> type, boolean defaultValue, String defaultHotkey, KeybindSettings settings)
    {
        this(name, type,
             defaultValue, false,
             getNextDefaultLinePosition(),
             defaultHotkey,
             buildTranslateName(name, "comment"),
             settings,
             buildTranslateName(name, "name"),
             buildTranslateName(name, "prettyName"));
    }

    InfoToggle(String name, InfoLineType<?> type, boolean defaultValue, boolean serverDataRequired, String defaultHotkey, KeybindSettings settings)
    {
        this(name, type,
             defaultValue, serverDataRequired,
             getNextDefaultLinePosition(),
             defaultHotkey,
             buildTranslateName(name, "comment"),
             settings,
             buildTranslateName(name, "name"),
             buildTranslateName(name, "prettyName"));
    }

    InfoToggle(String name, InfoLineType<?> type, boolean defaultValue, int linePosition, String defaultHotkey)
    {
        this(name, type,
             defaultValue, false,
             linePosition,
             defaultHotkey,
             buildTranslateName(name, "comment"),
             KeybindSettings.DEFAULT,
             buildTranslateName(name, "name"),
             buildTranslateName(name, "prettyName"));
    }

    InfoToggle(String name, InfoLineType<?> type, boolean defaultValue, boolean serverDataRequired, int linePosition, String defaultHotkey)
    {
        this(name, type,
             defaultValue, serverDataRequired,
             linePosition,
             defaultHotkey,
             buildTranslateName(name, "comment"),
             KeybindSettings.DEFAULT,
             buildTranslateName(name, "name"),
             buildTranslateName(name, "prettyName"));
    }

    InfoToggle(String name, InfoLineType<?> type, boolean defaultValue, int linePosition, String defaultHotkey, KeybindSettings settings)
    {
        this(name, type,
             defaultValue, false,
             linePosition,
             defaultHotkey,
             buildTranslateName(name, "comment"),
             settings,
             buildTranslateName(name, "name"),
             buildTranslateName(name, "prettyName"));
    }

    InfoToggle(String name, InfoLineType<?> type, boolean defaultValue, boolean serverDataRequired, int linePosition, String defaultHotkey, KeybindSettings settings)
    {
        this(name, type,
             defaultValue, serverDataRequired,
             linePosition,
             defaultHotkey,
             buildTranslateName(name, "comment"),
             settings,
             buildTranslateName(name, "name"),
             buildTranslateName(name, "prettyName"));
    }

    InfoToggle(String name, InfoLineType<?> type, boolean defaultValue, String defaultHotkey, String comment)
    {
        this(name, type, defaultValue, false, getNextDefaultLinePosition(), defaultHotkey, comment, KeybindSettings.DEFAULT, buildTranslateName(name, "name"), name);
    }

    InfoToggle(String name, InfoLineType<?> type, boolean defaultValue, boolean serverDataRequired, String defaultHotkey, String comment)
    {
        this(name, type, defaultValue, serverDataRequired, getNextDefaultLinePosition(), defaultHotkey, comment, KeybindSettings.DEFAULT, buildTranslateName(name, "name"), name);
    }

    InfoToggle(String name, InfoLineType<?> type, boolean defaultValue, String defaultHotkey, String comment, String translatedName)
    {
        this(name, type, defaultValue, false, getNextDefaultLinePosition(), defaultHotkey, comment, KeybindSettings.DEFAULT, translatedName, name);
    }

    InfoToggle(String name, InfoLineType<?> type, boolean defaultValue, boolean serverDataRequired, String defaultHotkey, String comment, String translatedName)
    {
        this(name, type, defaultValue, serverDataRequired, getNextDefaultLinePosition(), defaultHotkey, comment, KeybindSettings.DEFAULT, translatedName, name);
    }

    InfoToggle(String name, InfoLineType<?> type, boolean defaultValue, boolean serverDataRequired, int linePosition, String defaultHotkey, String comment, KeybindSettings settings, String translatedName, String prettyName)
    {
        this.name = name;
        this.type = type;
        this.valueBoolean = defaultValue;
        this.defaultValueBoolean = defaultValue;
        this.keybind = KeybindMulti.fromStorageString(defaultHotkey, settings);
        this.keybind.setCallback(new KeyCallbackToggleBoolean(this));
        this.defaultLinePosition = linePosition;
        this.linePosition = linePosition;
        this.comment = comment;
        this.prettyName = prettyName;
        this.translatedName = translatedName;
        this.serverDataRequired = serverDataRequired;
        this.updateLastBooleanHotkeyValue();
        this.updateLastIntegerValue();
    }

    @Override
    public ConfigType getType()
    {
        return ConfigType.HOTKEY;
    }

    public @Nullable InfoLineType<?> getInfoType()
    {
        return this.type;
    }

    public @Nullable List<InfoLineFlag> getInfoFlags()
    {
        if (this.type != null)
        {
            return this.type.getFlags();
        }

        return null;
    }

    public boolean hasFlag(InfoLineFlag flag)
    {
        return this.type != null && this.type.getFlags().contains(flag);
    }

    public @Nullable InfoLine initParser()
    {
        if (this.type != null)
        {
            return this.type.init(this);
        }

        return null;
    }

    @Override
    public String getName()
    {
        if (this.serverDataRequired)
        {
            return GuiBase.TXT_GOLD + this.name + GuiBase.TXT_RST;
        }

        return this.name;
    }

    @Override
    public String getPrettyName()
    {
        return StringUtils.getTranslatedOrFallback(this.prettyName, this.prettyName.isEmpty() ? StringUtils.splitCamelCase(this.name) : this.prettyName);
    }

    @Override
    public String getConfigGuiDisplayName()
    {
        String name = StringUtils.getTranslatedOrFallback(this.translatedName, this.name);

        if (this.serverDataRequired)
        {
            return GuiBase.TXT_GOLD + name + GuiBase.TXT_RST;
        }

        return name;
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
        String comment = StringUtils.getTranslatedOrFallback(this.comment, this.comment);

        if (comment != null && this.serverDataRequired)
        {
            return comment + "\n" + StringUtils.translate(Reference.MOD_ID + ".label.config_comment.server_side_data");
        }

        return comment;
    }

    @Override
    public String getTranslatedName()
    {
        String name = StringUtils.getTranslatedOrFallback(this.translatedName, this.name);

        if (this.serverDataRequired)
        {
            return GuiBase.TXT_GOLD + name + GuiBase.TXT_RST;
        }

        return name;
    }

    @Override
    public void setPrettyName(String s)
    {
        this.prettyName = s;
    }

    @Override
    public void setTranslatedName(String s)
    {
        this.translatedName = s;
    }

    @Override
    public void setComment(String s)
    {
        this.comment = s;
    }

    @Override
    public boolean isDirty()
    {
        return this.dirty;
    }

    @Override
    public void markDirty()
    {
        this.getKeybind().markDirty();
        this.dirty = true;
    }

    @Override
    public void markClean()
    {
        this.getKeybind().markClean();
        this.dirty = false;
    }

    @Override
    public void checkIfClean()
    {
        if (this.isDirty())
        {
            this.markClean();
            this.onValueChanged();
        }
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
        this.updateLastBooleanHotkeyValue();
        this.valueBoolean = value;
    }

    @Override
    public void toggleBooleanValue()
    {
        this.updateLastBooleanHotkeyValue();
        this.valueBoolean = !this.valueBoolean;
        this.markClean();
        this.onValueChanged();

        if (this == InfoToggle.SERVER_TPS || this == InfoToggle.MOB_CAPS)
        {
            HudDataManager.getInstance().refreshDataLoggers();
        }
    }

    @Override
    public boolean getLastBooleanValue()
    {
        return this.lastBooleanHotkey.getLeft();
    }

    @Override
    public void updateLastBooleanValue()
    {
        this.updateLastBooleanHotkeyValue();
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
        this.updateLastIntegerValue();
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
    public int getLastIntegerValue()
    {
        return this.lastInteger;
    }

    @Override
    public void updateLastIntegerValue()
    {
        this.lastInteger = this.linePosition;
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
        return !String.valueOf(this.defaultValueBoolean).equals(newValue);
    }

    @Override
    public void resetToDefault()
    {
        this.updateLastBooleanHotkeyValue();
        this.updateLastIntegerValue();
        this.valueBoolean = this.defaultValueBoolean;
    }

    @Override
    public Pair<Boolean, String> getBooleanHotkeyValue()
    {
        return Pair.of(this.valueBoolean, this.getKeybind().getStringValue());
    }

    @Override
    public Pair<Boolean, String> getDefaultBooleanHotkeyValue()
    {
        return Pair.of(this.defaultValueBoolean, this.getKeybind().getDefaultStringValue());
    }

    @Override
    public void setBooleanHotkeyValue(Pair<Boolean, String> value)
    {
        this.updateLastBooleanHotkeyValue();
        this.setBooleanValue(value.getLeft());
        this.getKeybind().setValueFromString(value.getRight());
    }

    @Override
    public Pair<Boolean, String> getLastBooleanHotkeyValue()
    {
        return this.lastBooleanHotkey;
    }

    @Override
    public void updateLastBooleanHotkeyValue()
    {
        this.lastBooleanHotkey = this.getBooleanHotkeyValue();
    }

    @Override
    public void onValueChanged()
    {
        // NO-OP
    }

    @Override
    public void setValueChangeCallback(IValueChangeCallback<IConfigBoolean> iValueChangeCallback)
    {
        // NO-OP
    }

    @Override
    public void setValueFromString(String value)
    {
        this.updateLastBooleanHotkeyValue();
        boolean oldValue = this.valueBoolean;

        try
        {
            this.valueBoolean = Boolean.parseBoolean(value);

            if (oldValue != this.valueBoolean)
            {
                this.markClean();
                this.onValueChanged();
            }
        }
        catch (Exception e)
        {
            MiniHUD.LOGGER.warn("Failed to read config value for {} from the JSON config", this.getName(), e);
        }
    }

    @Override
    public void setValueFromJsonElement(JsonElement element)
    {
        final boolean oldBool = this.valueBoolean;
        final String oldKeybind = this.keybind.getStringValue();

        try
        {
            if (element.isJsonPrimitive())
            {
                boolean temp = element.getAsBoolean();
                this.valueBoolean = temp;       // This seems redundant, but this makes it safer from corruption
            }
            else
            {
                MiniHUD.LOGGER.warn("Failed to read config value for {} from the JSON config", this.getName());
            }

            if (oldBool != this.valueBoolean ||
                oldKeybind != null && !oldKeybind.equals(this.keybind.getStringValue()) ||
                this.isDirty())
            {
                this.markClean();

                if (!this.getLastBooleanHotkeyValue().equals(this.getBooleanHotkeyValue()))
                {
                    this.onValueChanged();
                }
            }
        }
        catch (Exception e)
        {
            MiniHUD.LOGGER.warn("Failed to read config value for {} from the JSON config", this.getName(), e);
        }
    }

    @Override
    public JsonElement getAsJsonElement()
    {
        return new JsonPrimitive(this.valueBoolean);
    }
}
