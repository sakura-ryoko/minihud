package fi.dy.masa.minihud.config;

import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.minihud.data.DebugDataManager;
import fi.dy.masa.minihud.data.HudDataManager;
import fi.dy.masa.minihud.renderer.*;
import fi.dy.masa.minihud.renderer.shapes.ShapeManager;
import fi.dy.masa.minihud.util.DataStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class RendererCallbacks
{
    public static void onBeaconRangeToggled(IConfigBoolean config)
    {
        if (config.getBooleanValue())
        {
            OverlayRendererBeaconRange.INSTANCE.reset();
            OverlayRendererBeaconRange.INSTANCE.setNeedsUpdate();
        }
    }

    public static void onBiomeBorderToggled(IConfigBoolean config)
    {
        if (config.getBooleanValue())
        {
            OverlayRendererBiomeBorders.INSTANCE.reset();
            OverlayRendererBiomeBorders.INSTANCE.setNeedsUpdate();
        }
    }

    public static void onConduitRangeToggled(IConfigBoolean config)
    {
        if (config.getBooleanValue())
        {
            OverlayRendererBeaconRange.INSTANCE.reset();
            OverlayRendererConduitRange.INSTANCE.setNeedsUpdate();
        }
    }

    public static void onLightningRodRangeToggled(IConfigBoolean config)
    {
        OverlayRendererLightningRodRange.INSTANCE.reset();
        OverlayRendererLightningRodRange.INSTANCE.forceRescan();
    }

    public static void onLightLevelToggled(IConfigBoolean config)
    {
        if (config.getBooleanValue())
        {
            OverlayRendererLightLevel.INSTANCE.reset();
            OverlayRendererLightLevel.INSTANCE.setNeedsUpdate();
        }
    }

    public static void onRandomTicksFixedToggled(IConfigBoolean config)
    {
        Entity entity = EntityUtils.getCameraEntity();

        if (config.getBooleanValue() && entity != null)
        {
            Vec3 pos = entity.position();
            OverlayRendererRandomTickableChunks.INSTANCE_FIXED.setNewPos(pos);
            String green = GuiBase.TXT_GREEN;
            String rst = GuiBase.TXT_RST;
            String strStatus = green + StringUtils.translate("malilib.message.value.on") + rst;
            String strPos = String.format("x: %.2f, y: %.2f, z: %.2f", pos.x, pos.y, pos.z);
            String message = StringUtils.translate("minihud.message.toggled_using_position", config.getPrettyName(), strStatus, strPos);

            InfoUtils.printActionbarMessage(message);
        }
    }

    public static void onRandomTicksPlayerToggled(IConfigBoolean config)
    {
        if (config.getBooleanValue())
        {
            OverlayRendererRandomTickableChunks.INSTANCE_PLAYER.setNeedsUpdate();
        }
    }

    public static void onRegionFileToggled(IConfigBoolean config)
    {
        if (config.getBooleanValue())
        {
            OverlayRendererRegion.INSTANCE.setNeedsUpdate();
        }
    }

    public static void onSlimeChunksToggled(IConfigBoolean config)
    {
        if (config.getBooleanValue())
        {
            OverlayRendererSlimeChunks.INSTANCE.setNeedsUpdate();
            OverlayRendererSlimeChunks.INSTANCE.onEnabled();
        }
    }

    public static void onSpawnChunksPlayerToggled(IConfigBoolean config)
    {
        if (config.getBooleanValue())
        {
            String green = GuiBase.TXT_GREEN;
            String rst = GuiBase.TXT_RST;
            String message;

            String strStatus = green + StringUtils.translate("malilib.message.value.on") + rst;
            String strDist = String.format("%d", DataStorage.getInstance().getSimulationDistance());
            message = StringUtils.translate("minihud.message.toggled_using_player_spawn", config.getPrettyName(), strStatus, strDist);

            InfoUtils.printActionbarMessage(message);
            OverlayRendererSpawnChunks.INSTANCE_PLAYER.setNeedsUpdate();
        }
    }

    public static void onSpawnChunksRealToggled(IConfigBoolean config)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc != null && mc.player != null)
        {
            if (config.getBooleanValue())
            {
                GlobalPos spawn = HudDataManager.getInstance().getWorldSpawn();
                int radius = HudDataManager.getInstance().getSpawnChunkRadius();
                String green = GuiBase.TXT_GREEN;
//                String red = GuiBase.TXT_RED;
                String rst = GuiBase.TXT_RST;
                String message;

//                if (radius < 0)
//                {
//                    HudDataManager.getInstance().setSpawnChunkRadius(2, true);   // 1.20.5 Vanilla Default
//                    radius = 2;
//                }
                if (radius > 0)
                {
                    String strStatus = green + StringUtils.translate("malilib.message.value.on") + rst;
                    String strPos = String.format("x: %d, y: %d, z: %d [R: %d]", spawn.pos().getX(), spawn.pos().getY(), spawn.pos().getZ(), radius);
                    message = StringUtils.translate("minihud.message.toggled_using_world_spawn", config.getPrettyName(), strStatus, strPos);

                    if (mc.hasSingleplayerServer() == false && HudDataManager.getInstance().hasServuxServer())
                    {
                        HudDataManager.getInstance().requestSpawnMetadata();
                    }
                    else
                    {
                        OverlayRendererSpawnChunks.INSTANCE_REAL.setNeedsUpdate();
                    }
                }
                else
                {
                    OverlayRendererSpawnChunks.INSTANCE_REAL.setNeedsUpdate();

                    String strStatus = green + StringUtils.translate("malilib.message.value.on") + rst;
                    String strPos = String.format("[%s] x: %d, y: %d, z: %d",
                                                  spawn.dimension().identifier().toString(),
                                                  spawn.pos().getX(), spawn.pos().getY(), spawn.pos().getZ());
                    message = StringUtils.translate("minihud.message.toggled_using_world_spawn", config.getPrettyName(), strStatus, strPos);

//                    RendererToggle.OVERLAY_SPAWN_CHUNK_OVERLAY_REAL.setBooleanValue(false);
                }

                InfoUtils.printActionbarMessage(message);
            }
        }
    }

    public static void onStructuresToggled(IConfigBoolean config)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc != null && mc.player != null)
        {
            if (mc.hasSingleplayerServer() == false && DataStorage.getInstance().hasIntegratedServer() == false)
            {
                if (config.getBooleanValue())
                {
                    DataStorage.getInstance().registerStructureChannel();
                }
                else
                {
                    DataStorage.getInstance().unregisterStructureChannel();
                }
            }
            else
            {
                DataStorage.getInstance().setStructuresNeedUpdating();
            }
        }
    }

    public static void onShapeRendererToggled(IConfigBoolean config)
    {
        if (config.getBooleanValue())
        {
            ShapeManager.INSTANCE.setAllNeedsUpdate();
        }
    }

    public static void onDebugServiceToggled(IConfigBoolean config)
    {
        Minecraft mc = Minecraft.getInstance();
	    DebugDataManager.getInstance().onConfigSync();

        if (mc != null && mc.player != null)
        {
            if (!mc.hasSingleplayerServer() && !DataStorage.getInstance().hasIntegratedServer())
            {
                if (config.getBooleanValue())
                {
					DebugDataManager.getInstance().toggleDebugRendering(true);
                    DebugDataManager.getInstance().registerDebugService();
                    DebugDataManager.getInstance().requestMetadata();
                }
                else
                {
	                DebugDataManager.getInstance().toggleDebugRendering(false);
                    DebugDataManager.getInstance().unregisterDebugService();
                }
            }
        }
    }
}
