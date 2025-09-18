package fi.dy.masa.minihud.util;

import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.data.DebugDataManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.debug.DebugHudEntries;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Deprecated
public class DebugInfoUtils
{
//    private static boolean neighborUpdateEnabled;
//    private static int tickCounter;

    // Could move this, but it works fine.
//    public static void onNeighborUpdate(World world, BlockPos pos)
//    {
//        if (RendererToggle.DEBUG_DATA_MAIN_TOGGLE.getBooleanValue() == false)
//        {
//            return;
//        }
//
//        // This will only work in single player...
//        // We are catching updates from the server world, and adding them to the debug renderer directly
//        //if (neighborUpdateEnabled && world.isClient == false)
//        if (world.isClient() == false)
//        {
//            MinecraftClient mc = MinecraftClient.getInstance();
//            mc.execute(() -> ((NeighborUpdateDebugRenderer) mc.debugRenderer.neighborUpdateDebugRenderer).addNeighborUpdate(world.getTime(), pos.toImmutable()));
//        }
//    }

//    public static void onServerTickEnd(MinecraftServer server)
//    {
//        if (RendererToggle.DEBUG_DATA_MAIN_TOGGLE.getBooleanValue() == false)
//        {
//            return;
//        }
//    }

    public static void toggleDebugRenderer(IConfigBoolean config)
    {
        if (config == RendererToggle.DEBUG_CHUNK_BORDER)
        {
//            boolean enabled = ((IMixinDebugRenderer) MinecraftClient.getInstance().debugRenderer).minihud_getShowChunkBorder();
			boolean enabled = DebugDataManager.getInstance().isDebugAlwaysEnabled(DebugHudEntries.CHUNK_BORDERS);

            if (enabled != RendererToggle.DEBUG_CHUNK_BORDER.getBooleanValue())
            {
                enabled = DebugDataManager.getInstance().toggleDebugAlwaysEnabled(DebugHudEntries.CHUNK_BORDERS);
                debugWarn(enabled ? "debug.chunk_boundaries.on" : "debug.chunk_boundaries.off");
            }
        }
        else if (config == RendererToggle.DEBUG_CHUNK_INFO)
        {
//            MinecraftClient.getInstance().debugChunkInfo = config.getBooleanValue();
			DebugDataManager.getInstance().setDebugAlwaysEnabled(DebugHudEntries.CHUNK_SECTION_PATHS, config.getBooleanValue());
        }
        else if (config == RendererToggle.DEBUG_CHUNK_OCCLUSION)
        {
//            MinecraftClient.getInstance().debugChunkOcclusion = config.getBooleanValue();
			DebugDataManager.getInstance().setDebugAlwaysEnabled(DebugHudEntries.CHUNK_SECTION_VISIBILITY, config.getBooleanValue());
        }
        else if (config == RendererToggle.DEBUG_OCTREEE)
        {
			boolean enabled = DebugDataManager.getInstance().isDebugAlwaysEnabled(DebugHudEntries.CHUNK_SECTION_OCTREE);
//			boolean enabled = ((IMixinDebugRenderer) MinecraftClient.getInstance().debugRenderer).minihud_getShowOctree();

            if (enabled != RendererToggle.DEBUG_OCTREEE.getBooleanValue())
            {
                enabled = DebugDataManager.getInstance().toggleDebugAlwaysEnabled(DebugHudEntries.CHUNK_SECTION_OCTREE);
//				enabled = MinecraftClient.getInstance().debugRenderer.toggleShowOctree();
            }

            if (enabled)
            {
                MiniHUD.LOGGER.warn("Toggled Vanilla 'Octree' Debug Renderer ON.");
            }
            else
            {
                MiniHUD.LOGGER.warn("Toggled Vanilla 'Octree' Debug Renderer OFF.");
            }
        }

        // NeedsServerData
//        if (RendererToggle.DEBUG_DATA_MAIN_TOGGLE.getBooleanValue() == false)
//        {
//            return;
//        }
//        if (config == RendererToggle.DEBUG_NEIGHBOR_UPDATES)
//        {
//            neighborUpdateEnabled = config.getBooleanValue();
//        }
    }

    private static void debugWarn(String key, Object... args)
    {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.empty()
                .append(Text.translatable("debug.prefix").formatted(Formatting.YELLOW, Formatting.BOLD))
                .append(" ")
                .append(Text.translatable(key, args)));
    }

    public static void renderVanillaDebug(MatrixStack matrixStack, Frustum frustum,
                                          VertexConsumerProvider.Immediate vtx,
                                          double cameraX, double cameraY, double cameraZ)
    {
//        DebugRenderer renderer = MinecraftClient.getInstance().debugRenderer;
//
//        // TODO not needed
//        /*
//        if (RendererToggle.DEBUG_CHUNK_DEBUG.getBooleanValue())
//        {
//            renderer.chunkDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
//        }
//         */
//        if (RendererToggle.DEBUG_CHUNK_LOADING.getBooleanValue())
//        {
//            renderer.chunkLoadingDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
//        }
//        if (RendererToggle.DEBUG_COLLISION_BOXES.getBooleanValue())
//        {
//            renderer.collisionDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
//        }
//        if (RendererToggle.DEBUG_HEIGHTMAP.getBooleanValue())
//        {
//            renderer.heightmapDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
//        }
//        if (RendererToggle.DEBUG_LIGHT.getBooleanValue())
//        {
//            renderer.lightDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
//        }
//        if (RendererToggle.DEBUG_SKYLIGHT.getBooleanValue())
//        {
//            renderer.skyLightDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
//        }
//        if (RendererToggle.DEBUG_SOLID_FACES.getBooleanValue())
//        {
//            //RenderSystem.enableDepthTest();
//            renderer.blockOutlineDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
//        }
//        if (RendererToggle.DEBUG_SUPPORTING_BLOCK.getBooleanValue())
//        {
//            renderer.supportingBlockDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
//        }
//        if (RendererToggle.DEBUG_WATER.getBooleanValue())
//        {
//            renderer.waterDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
//        }
//
//        // NeedsServerData
//        if (RendererToggle.DEBUG_DATA_MAIN_TOGGLE.getBooleanValue() == false)
//        {
//            return;
//        }
//
//        if (RendererToggle.DEBUG_NEIGHBOR_UPDATES.getBooleanValue())
//        {
//            renderer.neighborUpdateDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
//        }
//        if (RendererToggle.DEBUG_WORLDGEN.getBooleanValue())
//        {
//            renderer.worldGenAttemptDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
//        }
//        if (RendererToggle.DEBUG_STRUCTURES.getBooleanValue())
//        {
//            renderer.structureDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
//        }
//        if (RendererToggle.DEBUG_VILLAGE_SECTIONS.getBooleanValue())
//        {
//            renderer.villageSectionsDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
//        }
//        if (RendererToggle.DEBUG_BREEZE_JUMP.getBooleanValue())
//        {
//            renderer.breezeDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
//        }
//        if (RendererToggle.DEBUG_RAID_CENTER.getBooleanValue())
//        {
//            renderer.raidCenterDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
//        }
//        if (RendererToggle.DEBUG_GOAL_SELECTOR.getBooleanValue())
//        {
//            renderer.goalSelectorDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
//        }
//        if (RendererToggle.DEBUG_GAME_EVENT.getBooleanValue())
//        {
//            renderer.gameEventDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
//        }
//        if (RendererToggle.DEBUG_PATH_FINDING.getBooleanValue())
//        {
//            renderer.pathfindingDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
//        }
//        if (RendererToggle.DEBUG_VILLAGE.getBooleanValue())
//        {
//            renderer.villageDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
//        }
//        if (RendererToggle.DEBUG_BEEDATA.getBooleanValue())
//        {
//            renderer.beeDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
//        }
//        /*
//        if (RendererToggle.DEBUG_REDSTONE_UPDATE_ORDER.getBooleanValue())
//        {
//            renderer.redstoneUpdateOrderDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
//        }
//         */
    }

    /**
     * Fixes Desync between MiniHUD config and the actual toggles in game.
     * @param toggle
     */
    public static void onToggleVanillaDebugChunkBorder(boolean toggle)
    {
        if (toggle != RendererToggle.DEBUG_CHUNK_BORDER.getBooleanValue())
        {
            RendererToggle.DEBUG_CHUNK_BORDER.setBooleanValue(toggle);
        }
    }

    public static void onToggleVanillaDebugOctree(boolean toggle)
    {
        if (toggle != RendererToggle.DEBUG_OCTREEE.getBooleanValue())
        {
            RendererToggle.DEBUG_OCTREEE.setBooleanValue(toggle);
        }
    }
}
