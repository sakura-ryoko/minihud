package fi.dy.masa.minihud.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.IConfigNotifiable;
import fi.dy.masa.malilib.config.IHotkeyTogglable;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import fi.dy.masa.malilib.interfaces.IValueChangeCallback;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.minihud.MiniHUD;

import javax.annotation.Nullable;

public enum RendererToggle implements IHotkeyTogglable, IConfigNotifiable<IConfigBoolean>
{
    OVERLAY_BEACON_RANGE                ("overlayBeaconRange",          ""),
    OVERLAY_BIOME_BORDER                ("overlayBiomeBorder",          ""),
    OVERLAY_BLOCK_GRID                  ("overlayBlockGrid",            ""),
    OVERLAY_CONDUIT_RANGE               ("overlayConduitRange",         ""),
    OVERLAY_LIGHT_LEVEL                 ("overlayLightLevel",           ""),
    OVERLAY_RANDOM_TICKS_FIXED          ("overlayRandomTicksFixed",     ""),
    OVERLAY_RANDOM_TICKS_PLAYER         ("overlayRandomTicksPlayer",    ""),
    OVERLAY_REGION_FILE                 ("overlayRegionFile",           ""),
    OVERLAY_SLIME_CHUNKS_OVERLAY        ("overlaySlimeChunks",          "", KeybindSettings.INGAME_BOTH),
    OVERLAY_SPAWNABLE_COLUMN_HEIGHTS    ("overlaySpawnableColumnHeights",""),
    OVERLAY_SPAWN_CHUNK_OVERLAY_REAL    ("overlaySpawnChunkReal",       ""),
    OVERLAY_SPAWN_CHUNK_OVERLAY_PLAYER  ("overlaySpawnChunkPlayer",     ""),
    OVERLAY_STRUCTURE_MAIN_TOGGLE       ("overlayStructureMainToggle",  ""),
    OVERLAY_VILLAGER_INFO               ("overlayVillagerInfo",         ""),
    SHAPE_RENDERER                      ("shapeRenderer",               ""),

    DEBUG_CHUNK_BORDER                  ("debugChunkBorder",            ""),
    DEBUG_CHUNK_INFO                    ("debugChunkInfo",              ""),
    DEBUG_CHUNK_OCCLUSION               ("debugChunkOcclusion",         ""),
    DEBUG_COLLISION_BOXES               ("debugCollisionBoxEnabled",    ""),
    DEBUG_NEIGHBOR_UPDATES              ("debugNeighborsUpdateEnabled", ""),
    //DEBUG_PATH_FINDING                  ("debugPathfindingEnabled",     ""),
    DEBUG_SOLID_FACES                   ("debugSolidFaceEnabled",       ""),
    DEBUG_WATER                         ("debugWaterEnabled",           "");

    public static final ImmutableList<RendererToggle> VALUES = ImmutableList.copyOf(values());
    private static final String translateNameBase = "minihud.config.render_toggle";

    private final String name;
    private final String comment;
    private final String prettyName;
    private final String translatedName;
    private final IKeybind keybind;
    private final boolean defaultValueBoolean;
    private boolean valueBoolean;
    @Nullable private IValueChangeCallback<IConfigBoolean> callback;

    RendererToggle(String name, String defaultHotkey)
    {
        this(name, defaultHotkey, KeybindSettings.DEFAULT, buildTranslateName(name, "comment"), buildTranslateName(name, "prettyName"), buildTranslateName(name, "name"));
    }

    RendererToggle(String name, String defaultHotkey, String comment, String prettyName)
    {
        this(name, defaultHotkey, KeybindSettings.DEFAULT, comment, prettyName, name);
    }

    RendererToggle(String name, String defaultHotkey, String comment, String prettyName, String translatedName)
    {
        this(name, defaultHotkey, KeybindSettings.DEFAULT, comment, prettyName, translatedName);
    }

    RendererToggle(String name, String defaultHotkey, KeybindSettings settings)
    {
        this(name, defaultHotkey, settings, buildTranslateName(name, "comment"), buildTranslateName(name, "prettyName"), buildTranslateName(name, "name"));
    }

    RendererToggle(String name, String defaultHotkey, KeybindSettings settings, String comment, String prettyName)
    {
        this(name, defaultHotkey, settings, comment, prettyName, name);
    }

    RendererToggle(String name, String defaultHotkey, KeybindSettings settings, String comment, String prettyName, String translatedName)
    {
        this.name = name;
        this.defaultValueBoolean = false;
        this.keybind = KeybindMulti.fromStorageString(defaultHotkey, settings);
        this.keybind.setCallback(this::toggleValueWithMessage);
        this.comment = comment;
        this.prettyName = prettyName;
        this.translatedName = translatedName;
    }

    private boolean toggleValueWithMessage(KeyAction action, IKeybind key)
    {
        // Print the message before toggling the value, so that this message
        // doesn't overwrite the possible value change callback message
        InfoUtils.printBooleanConfigToggleMessage(this.getPrettyName(), ! this.valueBoolean);
        this.setBooleanValue(! this.valueBoolean);
        return true;
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
        boolean oldValue = this.valueBoolean;
        this.valueBoolean = value;

        if (oldValue != this.valueBoolean)
        {
            this.onValueChanged();
        }
    }

    @Override
    public void setValueChangeCallback(IValueChangeCallback<IConfigBoolean> callback)
    {
        this.callback = callback;
    }

    @Override
    public void onValueChanged()
    {
        if (this.callback != null)
        {
            this.callback.onValueChanged(this);
        }
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

    private static String buildTranslateName(String name, String type)
    {
        return translateNameBase + "." + type + "." + name;
    }
}
