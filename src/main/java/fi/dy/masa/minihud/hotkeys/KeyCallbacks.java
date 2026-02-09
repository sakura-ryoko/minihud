package fi.dy.masa.minihud.hotkeys;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.*;
import fi.dy.masa.malilib.render.InventoryOverlayScreen;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererCallbacks;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.data.EntitiesDataManager;
import fi.dy.masa.minihud.data.HudDataManager;
import fi.dy.masa.minihud.gui.GuiConfigs;
import fi.dy.masa.minihud.gui.GuiConfigs.ConfigGuiTab;
import fi.dy.masa.minihud.gui.GuiShapeEditor;
import fi.dy.masa.minihud.gui.GuiShapeManager;
import fi.dy.masa.minihud.renderer.*;
import fi.dy.masa.minihud.renderer.shapes.ShapeBase;
import fi.dy.masa.minihud.renderer.shapes.ShapeManager;
import fi.dy.masa.minihud.util.DataStorage;
import fi.dy.masa.minihud.util.DebugInfoUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public class KeyCallbacks
{
    public static void init()
    {
        Callbacks callback = new Callbacks();

        Configs.Generic.SET_DISTANCE_REFERENCE_POINT.getKeybind().setCallback(callback);
        Configs.Generic.MOVE_SHAPE_TO_PLAYER.getKeybind().setCallback(callback);
        Configs.Generic.OPEN_CONFIG_GUI.getKeybind().setCallback(callback);
        Configs.Generic.SHAPE_EDITOR.getKeybind().setCallback(callback);
        Configs.Generic.INVENTORY_PREVIEW_TOGGLE_SCREEN.getKeybind().setCallback(callback);

        Configs.Generic.ENTITY_DATA_SYNC.setValueChangeCallback((config) -> EntitiesDataManager.getInstance().onEntityDataSyncToggled(config));
        Configs.Generic.HUD_DATA_SYNC.setValueChangeCallback((config) -> HudDataManager.getInstance().onHudDataSyncToggled(config));

        Configs.Colors.BEACON_RANGE_LVL1_OVERLAY_COLOR.setValueChangeCallback((config) -> updateBeaconOverlay());
        Configs.Colors.BEACON_RANGE_LVL2_OVERLAY_COLOR.setValueChangeCallback((config) -> updateBeaconOverlay());
        Configs.Colors.BEACON_RANGE_LVL3_OVERLAY_COLOR.setValueChangeCallback((config) -> updateBeaconOverlay());
        Configs.Colors.BEACON_RANGE_LVL4_OVERLAY_COLOR.setValueChangeCallback((config) -> updateBeaconOverlay());
        Configs.Colors.CONDUIT_RANGE_OVERLAY_COLOR.setValueChangeCallback(cfg -> OverlayRendererBeaconRange.INSTANCE.setNeedsUpdate());
        Configs.Colors.LIGHTNING_ROD_RANGE_OVERLAY_COLOR.setValueChangeCallback((config) -> OverlayRendererLightningRodRange.INSTANCE.setNeedsUpdate());
        Configs.Colors.LIGHTNING_ROD_DAMAGE_ZONE_COLOR.setValueChangeCallback((config) -> OverlayRendererLightningRodRange.INSTANCE.setNeedsUpdate());

        Configs.Generic.LIGHT_LEVEL_RANGE.setValueChangeCallback((config) -> OverlayRendererLightLevel.INSTANCE.setNeedsUpdate());
        Configs.Generic.LIGHT_LEVEL_RENDER_THROUGH.setValueChangeCallback((config) -> OverlayRendererLightLevel.INSTANCE.setRenderThrough(config.getBooleanValue()));
        Configs.Generic.STRUCTURES_RENDER_THROUGH.setValueChangeCallback((config) -> OverlayRendererStructures.INSTANCE.setRenderThrough(config.getBooleanValue()));

        Configs.Generic.SPAWN_PLAYER_OUTER_OVERLAY_ENABLED.setValueChangeCallback((config) -> OverlayRendererSpawnChunks.INSTANCE_PLAYER.setNeedsUpdate());
        Configs.Generic.SPAWN_PLAYER_REDSTONE_OVERLAY_ENABLED.setValueChangeCallback((config) -> OverlayRendererSpawnChunks.INSTANCE_PLAYER.setNeedsUpdate());
//        Configs.Generic.SPAWN_REAL_OUTER_OVERLAY_ENABLED.setValueChangeCallback((config) -> OverlayRendererSpawnChunks.INSTANCE_REAL.setNeedsUpdate());
//        Configs.Generic.SPAWN_REAL_REDSTONE_OVERLAY_ENABLED.setValueChangeCallback((config) -> OverlayRendererSpawnChunks.INSTANCE_REAL.setNeedsUpdate());

        Configs.Colors.SPAWN_PLAYER_ENTITY_OVERLAY_COLOR.setValueChangeCallback((config) -> OverlayRendererSpawnChunks.INSTANCE_PLAYER.setNeedsUpdate());
        Configs.Colors.SPAWN_PLAYER_REDSTONE_OVERLAY_COLOR.setValueChangeCallback((config) -> OverlayRendererSpawnChunks.INSTANCE_PLAYER.setNeedsUpdate());
        Configs.Colors.SPAWN_PLAYER_LAZY_OVERLAY_COLOR.setValueChangeCallback((config) -> OverlayRendererSpawnChunks.INSTANCE_PLAYER.setNeedsUpdate());
        Configs.Colors.SPAWN_PLAYER_OUTER_OVERLAY_COLOR.setValueChangeCallback((config) -> OverlayRendererSpawnChunks.INSTANCE_PLAYER.setNeedsUpdate());

        Configs.Colors.SPAWN_REAL_ENTITY_OVERLAY_COLOR.setValueChangeCallback((config) -> OverlayRendererSpawnChunks.INSTANCE_REAL.setNeedsUpdate());
//        Configs.Colors.SPAWN_REAL_REDSTONE_OVERLAY_COLOR.setValueChangeCallback((config) -> OverlayRendererSpawnChunks.INSTANCE_REAL.setNeedsUpdate());
//        Configs.Colors.SPAWN_REAL_LAZY_OVERLAY_COLOR.setValueChangeCallback((config) -> OverlayRendererSpawnChunks.INSTANCE_REAL.setNeedsUpdate());
//        Configs.Colors.SPAWN_REAL_OUTER_OVERLAY_COLOR.setValueChangeCallback((config) -> OverlayRendererSpawnChunks.INSTANCE_REAL.setNeedsUpdate());

        RendererToggle.OVERLAY_SLIME_CHUNKS_OVERLAY.getKeybind().setCallback(new KeyCallbackAdjustable(RendererToggle.OVERLAY_SLIME_CHUNKS_OVERLAY, new KeyCallbackToggleBooleanConfigWithMessage(RendererToggle.OVERLAY_SLIME_CHUNKS_OVERLAY)));

        RendererToggle.OVERLAY_BEACON_RANGE.setValueChangeCallback(RendererCallbacks::onBeaconRangeToggled);
        RendererToggle.OVERLAY_BIOME_BORDER.setValueChangeCallback(RendererCallbacks::onBiomeBorderToggled);
        RendererToggle.OVERLAY_CONDUIT_RANGE.setValueChangeCallback(RendererCallbacks::onConduitRangeToggled);
        RendererToggle.OVERLAY_LIGHTNING_ROD_RANGE.setValueChangeCallback(RendererCallbacks::onLightningRodRangeToggled);
        RendererToggle.OVERLAY_LIGHT_LEVEL.setValueChangeCallback(RendererCallbacks::onLightLevelToggled);
        RendererToggle.OVERLAY_RANDOM_TICKS_FIXED.setValueChangeCallback(RendererCallbacks::onRandomTicksFixedToggled);
        RendererToggle.OVERLAY_RANDOM_TICKS_PLAYER.setValueChangeCallback(RendererCallbacks::onRandomTicksPlayerToggled);
        RendererToggle.OVERLAY_REGION_FILE.setValueChangeCallback(RendererCallbacks::onRegionFileToggled);
        RendererToggle.OVERLAY_SLIME_CHUNKS_OVERLAY.setValueChangeCallback(RendererCallbacks::onSlimeChunksToggled);
        RendererToggle.OVERLAY_SPAWN_CHUNK_OVERLAY_PLAYER.setValueChangeCallback(RendererCallbacks::onSpawnChunksPlayerToggled);
        RendererToggle.OVERLAY_SPAWN_CHUNK_OVERLAY_REAL.setValueChangeCallback(RendererCallbacks::onSpawnChunksRealToggled);
        RendererToggle.OVERLAY_STRUCTURE_MAIN_TOGGLE.setValueChangeCallback(RendererCallbacks::onStructuresToggled);

        RendererToggle.SHAPE_RENDERER.setValueChangeCallback(RendererCallbacks::onShapeRendererToggled);

        RendererToggle.DEBUG_CHUNK_BORDER.setValueChangeCallback(DebugInfoUtils::toggleDebugRenderer);
	    RendererToggle.DEBUG_ENTITY_HITBOXES.setValueChangeCallback(DebugInfoUtils::toggleDebugRenderer);
	    RendererToggle.DEBUG_BLOCK_OUTLINE.setValueChangeCallback(DebugInfoUtils::toggleDebugRenderer);
	    RendererToggle.DEBUG_WATER.setValueChangeCallback(DebugInfoUtils::toggleDebugRenderer);
	    RendererToggle.DEBUG_HEIGHTMAP.setValueChangeCallback(DebugInfoUtils::toggleDebugRenderer);
	    RendererToggle.DEBUG_COLLISION_BOXES.setValueChangeCallback(DebugInfoUtils::toggleDebugRenderer);
	    RendererToggle.DEBUG_SUPPORTING_BLOCK.setValueChangeCallback(DebugInfoUtils::toggleDebugRenderer);
//	    RendererToggle.DEBUG_LIGHT.setValueChangeCallback(DebugInfoUtils::toggleDebugDataConfig);
	    RendererToggle.DEBUG_BLOCK_LIGHT.setValueChangeCallback(DebugInfoUtils::toggleDebugRenderer);
	    RendererToggle.DEBUG_SKY_LIGHT.setValueChangeCallback(DebugInfoUtils::toggleDebugRenderer);
	    RendererToggle.DEBUG_SKYLIGHT_SECTIONS.setValueChangeCallback(DebugInfoUtils::toggleDebugRenderer);

	    RendererToggle.DEBUG_CHUNK_LOADING.setValueChangeCallback(DebugInfoUtils::toggleDebugRenderer);
	    RendererToggle.DEBUG_CHUNK_SECTION_OCTREEE.setValueChangeCallback(DebugInfoUtils::toggleDebugRenderer);
        RendererToggle.DEBUG_CHUNK_SECTION_PATHS.setValueChangeCallback(DebugInfoUtils::toggleDebugRenderer);
        RendererToggle.DEBUG_CHUNK_SECTION_VISIBILITY.setValueChangeCallback(DebugInfoUtils::toggleDebugRenderer);

	    RendererToggle.DEBUG_DATA_MAIN_TOGGLE.setValueChangeCallback(RendererCallbacks::onDebugServiceToggled);
	    RendererToggle.DEBUG_PATH_FINDING.setValueChangeCallback(DebugInfoUtils::toggleDebugDataConfig);
	    RendererToggle.DEBUG_NEIGHBOR_UPDATES.setValueChangeCallback(DebugInfoUtils::toggleDebugDataConfig);
	    RendererToggle.DEBUG_REDSTONE_UPDATE_ORDER.setValueChangeCallback(DebugInfoUtils::toggleDebugDataConfig);
	    RendererToggle.DEBUG_STRUCTURES.setValueChangeCallback(DebugInfoUtils::toggleDebugDataConfig);
	    RendererToggle.DEBUG_VILLAGE_SECTIONS.setValueChangeCallback(DebugInfoUtils::toggleDebugDataConfig);
	    RendererToggle.DEBUG_BRAIN.setValueChangeCallback(DebugInfoUtils::toggleDebugDataConfig);
	    RendererToggle.DEBUG_POI.setValueChangeCallback(DebugInfoUtils::toggleDebugDataConfig);
	    RendererToggle.DEBUG_BEEDATA.setValueChangeCallback(DebugInfoUtils::toggleDebugDataConfig);
	    RendererToggle.DEBUG_RAID_CENTER.setValueChangeCallback(DebugInfoUtils::toggleDebugDataConfig);
	    RendererToggle.DEBUG_GOAL_SELECTOR.setValueChangeCallback(DebugInfoUtils::toggleDebugDataConfig);
	    RendererToggle.DEBUG_GAME_EVENT.setValueChangeCallback(DebugInfoUtils::toggleDebugDataConfig);
	    RendererToggle.DEBUG_BREEZE_JUMP.setValueChangeCallback(DebugInfoUtils::toggleDebugDataConfig);
	    RendererToggle.DEBUG_ENTITY_BLOCK_INTERSECTION.setValueChangeCallback(DebugInfoUtils::toggleDebugDataConfig);
    }

    private static void updateBeaconOverlay()
    {
        OverlayRendererBeaconRange.INSTANCE.setNeedsUpdate();
    }

    public static class Callbacks implements IHotkeyCallback
    {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            Minecraft mc = Minecraft.getInstance();

            if (mc.player == null)
            {
                return false;
            }

            if (key == Configs.Generic.OPEN_CONFIG_GUI.getKeybind())
            {
                GuiBase.openGui(new GuiConfigs());
            }
            else if (key == Configs.Generic.MOVE_SHAPE_TO_PLAYER.getKeybind())
            {
                Entity entity = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
                ShapeBase shape = ShapeManager.INSTANCE.getSelectedShape();

                if (shape != null)
                {
                    shape.moveToPosition(entity.position());
                }
            }
            else if (key == Configs.Generic.SET_DISTANCE_REFERENCE_POINT.getKeybind())
            {
                Entity entity = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
                DataStorage.getInstance().setDistanceReferencePoint(entity.position());
            }
            else if (key == Configs.Generic.SHAPE_EDITOR.getKeybind())
            {
                ShapeBase shape = ShapeManager.INSTANCE.getSelectedShape();

                if (shape != null)
                {
                    GuiBase.openGui(new GuiShapeEditor(shape));
                }
                else
                {
                    GuiConfigs.tab = ConfigGuiTab.SHAPES;
                    GuiBase.openGui(new GuiShapeManager());
                }
            }
            else if (key == Configs.Generic.INVENTORY_PREVIEW_TOGGLE_SCREEN.getKeybind())
            {
                if (mc.screen instanceof InventoryOverlayScreen)
                {
                    mc.setScreen(null);
                }
                else if (Configs.Generic.INVENTORY_PREVIEW_ENABLED.getBooleanValue() &&
                        Configs.Generic.INVENTORY_PREVIEW.getKeybind().isKeybindHeld())
                {
                    //RayTraceUtils.InventoryPreviewData inventory = RayTraceUtils.getTargetInventory(mc);
                    /*
                    InventoryOverlay.Context inventory = RayTraceUtils.getTargetInventory(mc, false);

                    if (inventory != null)
                    {
                        mc.setScreen(new InventoryOverlayScreen(inventory));
                    }
                     */

                    InventoryOverlayHandler.getInstance().refreshInventoryOverlay(mc, Configs.Generic.SHULKER_DISPLAY_BACKGROUND_COLOR.getBooleanValue());
                }
                else
                {
                    return false;
                }
            }

            return true;
        }
    }
}
