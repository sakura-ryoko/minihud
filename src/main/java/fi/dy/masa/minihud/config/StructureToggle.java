package fi.dy.masa.minihud.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.minihud.util.DataStorage;

public enum StructureToggle
{
    OVERLAY_STRUCTURE_ANCIENT_CITY      ("Ancient City",    "", "#41D10AE6", "#41D506C9", "minihud.config.structure_toggle.comment.ancient_city", "Ancient City", "ancient_city"),
    OVERLAY_STRUCTURE_BASTION_REMNANT   ("Bastion Remnant", "", "#302171F5", "#302171F5", "minihud.config.structure_toggle.comment.bastion_remnant", "Bastion Remnant", "bastion_remnant"),
    OVERLAY_STRUCTURE_BURIED_TREASURE   ("Buried Treasure", "", "#302298E6", "#302298E6", "minihud.config.structure_toggle.comment.buried_treasure", "Buried Treasure", "buried_treasure"),
    OVERLAY_STRUCTURE_DESERT_PYRAMID    ("Desert Pyramid",  "", "#30FFFF00", "#30FFFF00", "minihud.config.structure_toggle.comment.desert_pyramid", "Desert Pyramid", "desert_pyramid"),
    OVERLAY_STRUCTURE_END_CITY          ("End City",        "", "#30EB07EB", "#30EB07EB", "minihud.config.structure_toggle.comment.end_city", "End City", "end_city"),
    OVERLAY_STRUCTURE_IGLOO             ("Igloo",           "", "#300FAFE4", "#300FAFE4", "minihud.config.structure_toggle.comment.igloo", "Igloo structures", "igloo"),
    OVERLAY_STRUCTURE_JUNGLE_TEMPLE     ("Jungle Temple",   "", "#3099FF00", "#3099FF00", "minihud.config.structure_toggle.comment.jungle_pyramid", "Jungle Temple", "jungle_pyramid"),
    OVERLAY_STRUCTURE_MANSION           ("Mansion",         "", "#30FF6500", "#30FF6500", "minihud.config.structure_toggle.comment.mansion", "Mansion structures", "mansion"),
    OVERLAY_STRUCTURE_MINESHAFT         ("Mineshaft",       "", "#30F8D650", "#30F8D650", "minihud.config.structure_toggle.comment.mineshaft", "Mineshaft structures", "mineshaft"),
    OVERLAY_STRUCTURE_NETHER_FORTRESS   ("Nether Fortress", "", "#30FC381D", "#30FC381D", "minihud.config.structure_toggle.comment.fortress", "Nether Fortress", "fortress"),
    OVERLAY_STRUCTURE_NETHER_FOSSIL     ("Nether Fossil",   "", "#30868E99", "#30868E99", "minihud.config.structure_toggle.comment.nether_fossil", "Nether Fossil", "nether_fossil"),
    OVERLAY_STRUCTURE_OCEAN_MONUMENT    ("Ocean Monument",  "", "#3029E6EF", "#3029E6EF", "minihud.config.structure_toggle.comment.monument", "Ocean Monument", "monument"),
    OVERLAY_STRUCTURE_OCEAN_RUIN        ("Ocean Ruin",      "", "#300FAD83", "#300FAD83", "minihud.config.structure_toggle.comment.ocean_ruin", "Ocean Ruin", "ocean_ruin"),
    OVERLAY_STRUCTURE_PILLAGER_OUTPOST  ("Pillager Outpost","", "#300FAD83", "#300FAD83", "minihud.config.structure_toggle.comment.pillager_outpost", "Pillager Outpost", "pillager_outpost"),
    OVERLAY_STRUCTURE_RUINED_PORTAL     ("Ruined Portal",   "", "#309F03D3", "#309F03D3", "minihud.config.structure_toggle.comment.ruined_portal", "Ruined Portal", "ruined_portal"),
    OVERLAY_STRUCTURE_SHIPWRECK         ("Shipwreck",       "", "#30EB1995", "#30EB1995", "minihud.config.structure_toggle.comment.shipwreck", "Shipwreck", "shipwreck"),
    OVERLAY_STRUCTURE_STRONGHOLD        ("Stronghold",      "", "#30009999", "#30009999", "minihud.config.structure_toggle.comment.stronghold", "Stronghold", "stronghold"),
    OVERLAY_STRUCTURE_TRIAL_CHAMBERS    ("Trial Chambers",  "", "#3599664E", "#35CC8868", "minihud.config.structure_toggle.comment.trial_chambers", "Trial Chambers", "trial_chambers"),
    OVERLAY_STRUCTURE_VILLAGE           ("Village",         "", "#3054CB4E", "#3054CB4E", "minihud.config.structure_toggle.comment.village", "Village", "village"),
    OVERLAY_STRUCTURE_WITCH_HUT         ("Witch Hut",       "", "#30BE1DFC", "#300099FF", "minihud.config.structure_toggle.comment.swamp_hut", "Witch Hut", "swamp_hut"),
    OVERLAY_STRUCTURE_TRAIL_RUINS       ("Trail Ruins",     "", "#307F5AFF", "#307F5AFF", "minihud.config.structure_toggle.comment.trail_ruins", "Trail Ruins", "trail_ruins"),
    OVERLAY_STRUCTURE_UNKNOWN           ("Unknown",         "", "#50FFFFFF", "#50FFFFFF", "minihud.config.structure_toggle.comment.unknown", "Unknown Structure", "unknown");

    public static final ImmutableList<StructureToggle> VALUES = ImmutableList.copyOf(values());
    public static final ImmutableList<IConfigBoolean> TOGGLE_CONFIGS = ImmutableList.copyOf(VALUES.stream().map(StructureToggle::getToggleOption).toList());
    public static final ImmutableList<IHotkey> HOTKEY_CONFIGS = ImmutableList.copyOf(VALUES.stream().map(StructureToggle::getHotkey).toList());
    public static final ImmutableList<ConfigColor> COLOR_CONFIGS = getColorConfigs();

    private final ConfigBoolean toggleOption;
    private final ConfigColor colorMain;
    private final ConfigColor colorComponents;
    private final IHotkey hotkey;
    private final String translateNameBase = "minihud.config.structure_toggle";

    StructureToggle(String name, String defaultHotkey, String colorMain, String colorComponents, String comment, String prettyName)
    {
        this.toggleOption    = new ConfigBoolean(name, false, comment, prettyName);
        this.colorMain       = new ConfigColor(name +  " Main", colorMain, prettyName + " full box");
        this.colorComponents = new ConfigColor(name + " Components", colorComponents, prettyName + " components");
        this.hotkey          = new ConfigHotkey("Toggle " + name, defaultHotkey, comment);

        this.hotkey.getKeybind().setCallback((action, key) -> { this.toggleOption.toggleBooleanValue(); return true; });
        this.toggleOption.setValueChangeCallback((config) -> DataStorage.getInstance().setStructuresNeedUpdating());

    }

    StructureToggle(String name, String defaultHotkey, String colorMain, String colorComponents, String comment, String prettyName, String translateSubName)
    {
        this.toggleOption    = new ConfigBoolean(name, false, comment, prettyName).translatedName(this.translateNameBase + ".name." + translateSubName);
        this.colorMain       = new ConfigColor(name +  " Main", colorMain, this.translateNameBase + ".full_box.comment." + translateSubName, prettyName + " full box").translatedName(this.translateNameBase + ".full_box.name." + translateSubName);
        this.colorComponents = new ConfigColor(name + " Components", colorComponents, this.translateNameBase + ".components.comment." + translateSubName,prettyName + " components").translatedName(this.translateNameBase + ".components.name." + translateSubName);
        this.hotkey          = new ConfigHotkey("Toggle " + name, defaultHotkey, comment);

        this.hotkey.getKeybind().setCallback((action, key) -> { this.toggleOption.toggleBooleanValue(); return true; });
        this.toggleOption.setValueChangeCallback((config) -> DataStorage.getInstance().setStructuresNeedUpdating());
    }

    public IConfigBoolean getToggleOption()
    {
        return this.toggleOption;
    }

    public ConfigColor getColorMain()
    {
        return this.colorMain;
    }

    public ConfigColor getColorComponents()
    {
        return this.colorComponents;
    }

    public IHotkey getHotkey()
    {
        return this.hotkey;
    }

    private static ImmutableList<ConfigColor> getColorConfigs()
    {
        ImmutableList.Builder<ConfigColor> builder = ImmutableList.builder();

        for (StructureToggle toggle : VALUES)
        {
            builder.add(toggle.getColorMain());
            builder.add(toggle.getColorComponents());
        }

        return builder.build();
    }
}
