package fi.dy.masa.minihud.config;

import java.util.List;
import com.google.common.collect.ImmutableList;

import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import fi.dy.masa.minihud.Reference;

public class Hotkeys
{
	private static final String HOTKEY_KEY = Reference.MOD_ID+".config.hotkey";

	public static final ConfigHotkey        INVENTORY_PREVIEW                   = new ConfigHotkey("inventoryPreview",              "LEFT_ALT", KeybindSettings.PRESS_ALLOWEXTRA).apply(HOTKEY_KEY);
	public static final ConfigHotkey        INVENTORY_PREVIEW_TOGGLE_SCREEN     = new ConfigHotkey("inventoryPreviewToggleScreen",  "BUTTON_2", KeybindSettings.create(KeybindSettings.Context.ANY, KeyAction.PRESS, true, true, false, true)).apply(HOTKEY_KEY);
	public static final ConfigHotkey        MOVE_SHAPE_TO_PLAYER                = new ConfigHotkey("moveShapeToPlayer",             "").apply(HOTKEY_KEY);
	public static final ConfigHotkey        OPEN_CONFIG_GUI                     = new ConfigHotkey("openConfigGui",                 "H,C").apply(HOTKEY_KEY);
	public static final ConfigHotkey        REQUIRED_KEY                        = new ConfigHotkey("requiredKey",                   "", KeybindSettings.MODIFIER_INGAME_EMPTY).apply(HOTKEY_KEY);
	public static final ConfigHotkey        SET_DISTANCE_REFERENCE_POINT        = new ConfigHotkey("setDistanceReferencePoint",     "").apply(HOTKEY_KEY);
	public static final ConfigHotkey        SHAPE_EDITOR                        = new ConfigHotkey("shapeEditor",                   "").apply(HOTKEY_KEY);

	public static final List<ConfigHotkey> HOTKEY_LIST = ImmutableList.of(
			OPEN_CONFIG_GUI,
			SHAPE_EDITOR,
			INVENTORY_PREVIEW,
			INVENTORY_PREVIEW_TOGGLE_SCREEN,
			MOVE_SHAPE_TO_PLAYER,
			REQUIRED_KEY,
			SET_DISTANCE_REFERENCE_POINT
	);
}
