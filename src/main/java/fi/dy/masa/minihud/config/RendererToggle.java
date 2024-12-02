package fi.dy.masa.minihud.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.IConfigNotifiable;
import fi.dy.masa.malilib.config.IHotkeyTogglable;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import fi.dy.masa.malilib.interfaces.IValueChangeCallback;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.Reference;

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
    OVERLAY_STRUCTURE_MAIN_TOGGLE       ("overlayStructureMainToggle",  true, ""),
    OVERLAY_VILLAGER_INFO               ("overlayVillagerInfo",         true, ""),
    SHAPE_RENDERER                      ("shapeRenderer",               ""),

    DEBUG_DATA_MAIN_TOGGLE              ("debugDataMainToggle",         true, ""),
    DEBUG_BEEDATA                       ("debugBeeDataEnabled",         true, ""),
    DEBUG_BRAIN                         ("debugBrainEnabled",           true, ""),
    DEBUG_BREEZE_JUMP                   ("debugBreezeJumpEnabled",      true, ""),
    DEBUG_CHUNK_BORDER                  ("debugChunkBorder",            ""),
    DEBUG_CHUNK_DEBUG                   ("debugChunkDebug",             ""),
    DEBUG_CHUNK_DEBUG                   ("debugChunkDebug",             ""),
    DEBUG_CHUNK_INFO                    ("debugChunkInfo",              ""),
    DEBUG_CHUNK_LOADING                 ("debugChunkLoading",           true, ""),
    DEBUG_CHUNK_OCCLUSION               ("debugChunkOcclusion",         ""),
    DEBUG_COLLISION_BOXES               ("debugCollisionBoxEnabled",    ""),
    DEBUG_HEIGHTMAP                     ("debugHeightmapEnabled",       ""),
    DEBUG_LIGHT                         ("debugLightEnabled",           ""),
    DEBUG_NEIGHBOR_UPDATES              ("debugNeighborsUpdateEnabled", true, ""),
    // todo
    //DEBUG_GAME_TEST                     ("debugGameTestEnabled",        true, ""),
    DEBUG_GAME_EVENT                    ("debugGameEventsEnabled",      true,""),
    DEBUG_GOAL_SELECTOR                 ("debugGoalSelectorEnabled",    true, ""),
    DEBUG_OCTREEE                       ("debugOctreeEnabled",          ""),
    DEBUG_PATH_FINDING                  ("debugPathfindingEnabled",     true, ""),
    DEBUG_RAID_CENTER                   ("debugRaidCenterEnabled",      true, ""),
    // todo
    //DEBUG_REDSTONE_UPDATE_ORDER         ("debugRedstoneUpdateOrder",    true, ""),
    DEBUG_SKYLIGHT                      ("debugSkylightEnabled",        ""),
    DEBUG_SOLID_FACES                   ("debugSolidFaceEnabled",       ""),
    DEBUG_STRUCTURES                    ("debugStructuresEnabled",      true, ""),
    DEBUG_SUPPORTING_BLOCK              ("debugSupportingBlock",        ""),
    DEBUG_WATER                         ("debugWaterEnabled",           ""),
    DEBUG_VILLAGE                       ("debugVillageEnabled",         true, ""),
    DEBUG_VILLAGE_SECTIONS              ("debugVillageSectionsEnabled", true, ""),
    DEBUG_WORLDGEN                      ("debugWorldGenEnabled",        true, "");

    public static final ImmutableList<RendererToggle> VALUES = ImmutableList.copyOf(values());
    private static final String RENDER_KEY = Reference.MOD_ID+".config.render_toggle";

    private final String name;
    private String comment;
    private String prettyName;
    private String translatedName;
    private final IKeybind keybind;
    private final boolean defaultValueBoolean;
    private boolean valueBoolean;
    private final boolean serverDataRequired;
    @Nullable private IValueChangeCallback<IConfigBoolean> callback;

    RendererToggle(String name, String defaultHotkey)
    {
        this(name, false,
             defaultHotkey, KeybindSettings.DEFAULT,
             buildTranslateName(name, "comment"),
             buildTranslateName(name, "prettyName"),
             buildTranslateName(name, "name"));
    }

    RendererToggle(String name, boolean serverDataRequired, String defaultHotkey)
    {
        this(name, serverDataRequired,
             defaultHotkey, KeybindSettings.DEFAULT,
             buildTranslateName(name, "comment"),
             buildTranslateName(name, "prettyName"),
             buildTranslateName(name, "name"));
    }

    RendererToggle(String name, String defaultHotkey, String comment)
    {
        this(name, false,
             defaultHotkey, KeybindSettings.DEFAULT,
             comment,
             buildTranslateName(name, "prettyName"),
             buildTranslateName(name, "name"));
    }

    RendererToggle(String name, boolean serverDataRequired, String defaultHotkey, String comment)
    {
        this(name, serverDataRequired,
             defaultHotkey, KeybindSettings.DEFAULT,
             comment,
             buildTranslateName(name, "prettyName"),
             buildTranslateName(name, "name"));
    }

    RendererToggle(String name, String defaultHotkey, String comment, String prettyName)
    {
        this(name, false,
             defaultHotkey, KeybindSettings.DEFAULT,
             comment,
             prettyName,
             buildTranslateName(name, "name"));
    }

    RendererToggle(String name, boolean serverDataRequired, String defaultHotkey, String comment, String prettyName)
    {
        this(name, serverDataRequired,
             defaultHotkey, KeybindSettings.DEFAULT,
             comment,
             prettyName,
             buildTranslateName(name, "name"));
    }

    RendererToggle(String name, String defaultHotkey, String comment, String prettyName, String translatedName)
    {
        this(name, false,
             defaultHotkey, KeybindSettings.DEFAULT,
             comment,
             prettyName,
             translatedName);
    }

    RendererToggle(String name, boolean serverDataRequired, String defaultHotkey, String comment, String prettyName, String translatedName)
    {
        this(name, serverDataRequired,
             defaultHotkey, KeybindSettings.DEFAULT,
             comment,
             prettyName,
             translatedName);
    }

    RendererToggle(String name, String defaultHotkey, KeybindSettings settings)
    {
        this(name, false,
             defaultHotkey, settings,
             buildTranslateName(name, "comment"),
             buildTranslateName(name, "prettyName"),
             buildTranslateName(name, "name"));
    }

    RendererToggle(String name, boolean serverDataRequired, String defaultHotkey, KeybindSettings settings)
    {
        this(name, serverDataRequired,
             defaultHotkey, settings,
             buildTranslateName(name, "comment"),
             buildTranslateName(name, "prettyName"),
             buildTranslateName(name, "name"));
    }

    RendererToggle(String name, String defaultHotkey, KeybindSettings settings, String comment)
    {
        this(name, false,
             defaultHotkey, settings, comment,
             buildTranslateName(name, "prettyName"),
             buildTranslateName(name, "name"));
    }

    RendererToggle(String name, boolean serverDataRequired, String defaultHotkey, KeybindSettings settings, String comment)
    {
        this(name, serverDataRequired,
             defaultHotkey, settings, comment,
             buildTranslateName(name, "prettyName"),
             buildTranslateName(name, "name"));
    }

    RendererToggle(String name, String defaultHotkey, KeybindSettings settings, String comment, String prettyName)
    {
        this(name, false,
             defaultHotkey, settings,
             comment,
             prettyName,
             buildTranslateName(name, "name"));
    }

    RendererToggle(String name, boolean serverDataRequired, String defaultHotkey, KeybindSettings settings, String comment, String prettyName)
    {
        this(name, serverDataRequired,
             defaultHotkey, settings,
             comment,
             prettyName,
             buildTranslateName(name, "name"));
    }

    RendererToggle(String name, boolean serverDataRequired, String defaultHotkey, KeybindSettings settings, String comment, String prettyName, String translatedName)
    {
        this.name = name;
        this.defaultValueBoolean = false;
        this.keybind = KeybindMulti.fromStorageString(defaultHotkey, settings);
        this.keybind.setCallback(this::toggleValueWithMessage);
        this.comment = comment;
        this.prettyName = prettyName;
        this.translatedName = translatedName;
        this.serverDataRequired = serverDataRequired;
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
        if (this.serverDataRequired)
        {
            return GuiBase.TXT_GOLD + this.name + GuiBase.TXT_RST;
        }

        return this.name;
    }

    @Override
    public String getPrettyName()
    {
        return StringUtils.getTranslatedOrFallback(this.prettyName, this.prettyName);
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


    public boolean needsServerData()
    {
        return this.serverDataRequired;
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

    private static String buildTranslateName(String name, String type)
    {
        return RENDER_KEY + "." + type + "." + name;
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
