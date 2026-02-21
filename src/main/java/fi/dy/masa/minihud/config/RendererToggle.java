package fi.dy.masa.minihud.config;

import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import fi.dy.masa.malilib.config.*;
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

public enum RendererToggle implements IEnumBooleanHotkey
{
    OVERLAY_BEACON_RANGE                ("overlayBeaconRange",          ""),
    OVERLAY_BIOME_BORDER                ("overlayBiomeBorder",          ""),
    OVERLAY_BLOCK_GRID                  ("overlayBlockGrid",            ""),
    OVERLAY_CONDUIT_RANGE               ("overlayConduitRange",         ""),
    OVERLAY_LIGHTNING_ROD_RANGE         ("overlayLightningRodRange",    ""),
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

	// Does not need server side data
	DEBUG_CHUNK_BORDER                  ("debugChunkBorder",            ""),
	DEBUG_ENTITY_HITBOXES               ("debugEntityHitboxes",         ""),
	DEBUG_BLOCK_OUTLINE                 ("debugBlockOutline" ,          ""),
	DEBUG_WATER                         ("debugWaterEnabled",           ""),
	DEBUG_LAVA                          ("debugLavaEnabled",            ""),
	DEBUG_HEIGHTMAP                     ("debugHeightmapEnabled",       ""),
	DEBUG_COLLISION_BOXES               ("debugCollisionBoxEnabled",    ""),
	DEBUG_SUPPORTING_BLOCK              ("debugSupportingBlock",        ""),
//	DEBUG_LIGHT                         ("debugLightEnabled",           ""),
	DEBUG_BLOCK_LIGHT                   ("debugBlockLightEnabled",      ""),
	DEBUG_SKY_LIGHT                     ("debugSkyLightEnabled",        ""),
	DEBUG_SKYLIGHT_SECTIONS             ("debugSkylightSectionsEnabled",""),

	// Not in Debug Renderer (?)
	DEBUG_CHUNK_LOADING                 ("debugChunkLoading",           ""),
	DEBUG_CHUNK_SECTION_OCTREEE         ("debugChunkSectionOctree",     ""),
	DEBUG_CHUNK_SECTION_PATHS           ("debugChunkSectionPaths",      ""),
	DEBUG_CHUNK_SECTION_VISIBILITY      ("debugChunkSectionVisibility", ""),

	// Needs server side data
	DEBUG_DATA_MAIN_TOGGLE              ("debugDataMainToggle",                 true, ""),
	DEBUG_PATH_FINDING                  ("debugPathfindingEnabled",             true, ""),
	DEBUG_NEIGHBOR_UPDATES              ("debugNeighborsUpdateEnabled",         true, ""),
	DEBUG_REDSTONE_UPDATE_ORDER         ("debugRedstoneUpdateOrder",            true, ""),
	DEBUG_STRUCTURES                    ("debugStructuresEnabled",              true, ""),
	DEBUG_VILLAGE_SECTIONS              ("debugVillageSectionsEnabled",         true, ""),
	DEBUG_BRAIN                         ("debugBrainEnabled",                   true, ""),
	DEBUG_POI                           ("debugPoiEnabled",                     true, ""),
    DEBUG_BEEDATA                       ("debugBeeDataEnabled",                 true, ""),
	DEBUG_RAID_CENTER                   ("debugRaidCenterEnabled",              true, ""),
	DEBUG_GOAL_SELECTOR                 ("debugGoalSelectorEnabled",            true, ""),
    DEBUG_GAME_EVENT                    ("debugGameEventsEnabled",              true, ""),
	DEBUG_BREEZE_JUMP                   ("debugBreezeJumpEnabled",              true, ""),
	DEBUG_ENTITY_BLOCK_INTERSECTION     ("debugEntityBlockIntersectionEnabled", true, ""),
    ;

    public static final ImmutableList<@NotNull RendererToggle> VALUES = ImmutableList.copyOf(values());
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
    private boolean dirty;
    private Pair<Boolean, String> lastBooleanHotkey;

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
        this.updateLastBooleanHotkeyValue();
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
        boolean oldValue = this.valueBoolean;
        this.valueBoolean = value;

        if (oldValue != this.valueBoolean)
        {
            this.onValueChanged();
        }
    }

    @Override
    public void toggleBooleanValue()
    {
        this.updateLastBooleanHotkeyValue();
        this.valueBoolean = !this.valueBoolean;
        this.markClean();
        this.onValueChanged();
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

    public void setBooleanValueNoCallback(boolean value)
	{
		this.valueBoolean = value;
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
        return !String.valueOf(this.defaultValueBoolean).equals(newValue);
    }

    @Override
    public void resetToDefault()
    {
        this.updateLastBooleanHotkeyValue();
        boolean oldValue = this.valueBoolean;
        this.valueBoolean = this.defaultValueBoolean;

        if (oldValue != this.valueBoolean)
        {
            this.onValueChanged();
        }
    }

    private static String buildTranslateName(String name, String type)
    {
        return RENDER_KEY + "." + type + "." + name;
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
