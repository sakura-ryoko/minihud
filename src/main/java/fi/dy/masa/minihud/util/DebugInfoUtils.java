package fi.dy.masa.minihud.util;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import com.google.common.collect.MapMaker;
import com.mojang.blaze3d.systems.RenderSystem;
import io.netty.buffer.Unpooled;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.render.debug.NeighborUpdateDebugRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.custom.DebugBrainCustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.mixin.debug.IMixinDebugRenderer;

public class DebugInfoUtils
{
    private static boolean neighborUpdateEnabled;
    private static boolean pathfindingEnabled = false;
    private static int tickCounter;
    private static final Map<Entity, Path> OLD_PATHS = new MapMaker().weakKeys().weakValues().makeMap();

    public static void registerPayloads()
    {
        PayloadTypeRegistry.playC2S().register(DebugBrainCustomPayload.ID, DebugBrainCustomPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DebugBrainCustomPayload.ID, DebugBrainCustomPayload.CODEC);
    }

    public static void sendPacketDebugPath(MinecraftServer server, int entityId, Path path, float maxDistance)
    {
        // FIXME --> This causes a custom_payload crash (Unregistered Vanilla channel)
        //DebugPathCustomPayload packet = new DebugPathCustomPayload(entityId, path, maxDistance);
        //server.getPlayerManager().sendToAll(new CustomPayloadS2CPacket(packet));
    }

    private static Path copyPath(Path path)
    {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        //path.toBuf(buf); // This won't work because the DebugNodeInfo is not set

        buf.writeBoolean(path.reachesTarget());
        buf.writeInt(path.getCurrentNodeIndex());
        buf.writeBlockPos(path.getTarget());

        int size = path.getLength();
        buf.writeVarInt(path.getLength());

        for (int i = 0; i < size; ++i)
        {
            path.getNode(i).write(buf);
        }

        buf.writeVarInt(0); // number of nodes in DebugNodeInfo
        buf.writeVarInt(0); // number of entries in openSet
        buf.writeVarInt(0); // number of entries in closedSet

        return Path.fromBuf(buf);
    }

    public static void onNeighborUpdate(World world, BlockPos pos)
    {
        // This will only work in single player...
        // We are catching updates from the server world, and adding them to the debug renderer directly
        if (neighborUpdateEnabled && world.isClient == false)
        {
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.execute(() -> ((NeighborUpdateDebugRenderer) mc.debugRenderer.neighborUpdateDebugRenderer).addNeighborUpdate(world.getTime(), pos.toImmutable()));
        }
    }

    public static void onServerTickEnd(MinecraftServer server)
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        // Send the custom packet with the Path data, if that debug renderer is enabled
        if (pathfindingEnabled && mc.world != null && ++tickCounter >= 10)
        {
            tickCounter = 0;
            ServerWorld world = server.getWorld(mc.world.getRegistryKey());

            if (world != null)
            {
                TypeFilter<Entity, MobEntity> filter = TypeFilter.instanceOf(MobEntity.class);
                Predicate<MobEntity> predicate = LivingEntity::isAlive;

                for (MobEntity entity : world.getEntitiesByType(filter, predicate))
                {
                    EntityNavigation navigator = entity.getNavigation();

                    if (navigator != null && isAnyPlayerWithinRange(world, entity, 64))
                    {
                        final Path path = navigator.getCurrentPath();

                        if (path == null)
                        {
                            continue;
                        }

                        Path old = OLD_PATHS.get(entity);
                        boolean isSamepath = old != null && old.equalsPath(path);

                        if (old == null || isSamepath == false || old.getCurrentNodeIndex() != path.getCurrentNodeIndex())
                        {
                            final int id = entity.getId();
                            // FIXME
                            //final float maxDistance = Configs.Generic.DEBUG_RENDERER_PATH_MAX_DIST.getBooleanValue() ? ((IMixinEntityNavigation) navigator).getMaxDistanceToWaypoint() : 0F;

                            //DebugInfoUtils.sendPacketDebugPath(server, id, path, maxDistance);

                            if (isSamepath == false)
                            {
                                OLD_PATHS.put(entity, copyPath(path));
                            }
                            else
                            {
                                old.setCurrentNodeIndex(path.getCurrentNodeIndex());
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isAnyPlayerWithinRange(ServerWorld world, Entity entity, double range)
    {
        List<ServerPlayerEntity> players = world.getPlayers();
        double squaredRange = range * range;

        for (PlayerEntity player : players)
        {
            double distSq = player.squaredDistanceTo(entity.getX(), entity.getY(), entity.getZ());

            if (range < 0.0 || distSq < squaredRange)
            {
                return true;
            }
        }

        return false;
    }

    public static void toggleDebugRenderer(IConfigBoolean config)
    {
        if (config == RendererToggle.DEBUG_NEIGHBOR_UPDATES)
        {
            neighborUpdateEnabled = config.getBooleanValue();
        }
        // FIXME --> Pathfinding renderer causes a custom_payload crash (Unregistered Vanilla Payload)
        /*
        else if (config == RendererToggle.DEBUG_PATH_FINDING)
        {
            pathfindingEnabled = config.getBooleanValue();
        }
         */
        else if (config == RendererToggle.DEBUG_CHUNK_BORDER)
        {
            boolean enabled = ((IMixinDebugRenderer) MinecraftClient.getInstance().debugRenderer).minihud_getShowChunkBorder();

            if (enabled != RendererToggle.DEBUG_CHUNK_BORDER.getBooleanValue())
            {
                enabled = MinecraftClient.getInstance().debugRenderer.toggleShowChunkBorder();
            }

            debugWarn(enabled ? "debug.chunk_boundaries.on" : "debug.chunk_boundaries.off");
        }
        else if (config == RendererToggle.DEBUG_CHUNK_INFO || config == RendererToggle.DEBUG_CHUNK_LOADING)
        {
            MinecraftClient.getInstance().debugChunkInfo = config.getBooleanValue();
        }
        else if (config == RendererToggle.DEBUG_CHUNK_OCCLUSION)
        {
            MinecraftClient.getInstance().debugChunkOcclusion = config.getBooleanValue();
        }
        else if (config == RendererToggle.DEBUG_OCTREEE)
        {
            boolean enabled = ((IMixinDebugRenderer) MinecraftClient.getInstance().debugRenderer).minihud_getShowOctree();

            if (enabled != RendererToggle.DEBUG_OCTREEE.getBooleanValue())
            {
                enabled = MinecraftClient.getInstance().debugRenderer.toggleShowOctree();
            }

            if (enabled)
            {
                MiniHUD.logger.warn("Toggled Vanilla 'Octree' Debug Renderer ON.");
            }
            else
            {
                MiniHUD.logger.warn("Toggled Vanilla 'Octree' Debug Renderer OFF.");
            }
        }
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
        DebugRenderer renderer = MinecraftClient.getInstance().debugRenderer;

        if (RendererToggle.DEBUG_COLLISION_BOXES.getBooleanValue())
        {
            renderer.collisionDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
            vtx.draw();
        }
        if (RendererToggle.DEBUG_NEIGHBOR_UPDATES.getBooleanValue())
        {
            renderer.neighborUpdateDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
            vtx.draw();
        }
        if (RendererToggle.DEBUG_SOLID_FACES.getBooleanValue())
        {
            RenderSystem.enableDepthTest();
            renderer.blockOutlineDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
            vtx.draw();
        }
        if (RendererToggle.DEBUG_WATER.getBooleanValue())
        {
            renderer.waterDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
            vtx.draw();
        }
        if (RendererToggle.DEBUG_CHUNK_LOADING.getBooleanValue())
        {
            renderer.chunkLoadingDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
            vtx.draw();
        }
        if (RendererToggle.DEBUG_CHUNK_DEBUG.getBooleanValue())
        {
            renderer.chunkDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
            vtx.draw();
        }
        if (RendererToggle.DEBUG_SUPPORTING_BLOCK.getBooleanValue())
        {
            renderer.supportingBlockDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
            vtx.draw();
        }
        if (RendererToggle.DEBUG_HEIGHTMAP.getBooleanValue())
        {
            renderer.heightmapDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
            vtx.draw();
        }
        if (RendererToggle.DEBUG_WORLDGEN.getBooleanValue())
        {
            renderer.worldGenAttemptDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
            vtx.draw();
        }
        if (RendererToggle.DEBUG_STRUCTURES.getBooleanValue())
        {
            renderer.structureDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
            vtx.draw();
        }
        if (RendererToggle.DEBUG_VILLAGE_SECTIONS.getBooleanValue())
        {
            renderer.villageSectionsDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
            vtx.draw();
        }
        if (RendererToggle.DEBUG_BREEZE_JUMP.getBooleanValue())
        {
            renderer.breezeDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
            vtx.draw();
        }
        if (RendererToggle.DEBUG_RAID_CENTER.getBooleanValue())
        {
            renderer.raidCenterDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
            vtx.draw();
        }
        if (RendererToggle.DEBUG_GOAL_SELECTOR.getBooleanValue())
        {
            renderer.goalSelectorDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
            vtx.draw();
        }
        if (RendererToggle.DEBUG_SKYLIGHT.getBooleanValue())
        {
            renderer.skyLightDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
            vtx.draw();
        }
        if (RendererToggle.DEBUG_GAME_EVENT.getBooleanValue())
        {
            renderer.gameEventDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
            vtx.draw();
        }
        if (RendererToggle.DEBUG_LIGHT.getBooleanValue())
        {
            renderer.lightDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
            vtx.draw();
        }

        // FIXME These cause a custom_payload crash
        /*
        if (RendererToggle.DEBUG_PATH_FINDING.getBooleanValue())
        {
            renderer.pathfindingDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
            vtx.draw();
        }
        if (RendererToggle.DEBUG_REDSTONE_UPDATE_ORDER.getBooleanValue())
        {
            renderer.redstoneUpdateOrderDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
            vtx.draw();
        }
        if (RendererToggle.DEBUG_VILLAGE.getBooleanValue())
        {
            renderer.villageDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
            vtx.draw();
        }
        if (RendererToggle.DEBUG_BEEDATA.getBooleanValue())
        {
            renderer.beeDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
            vtx.draw();
        }
        if (RendererToggle.DEBUG_GAME_TEST.getBooleanValue())
        {
            renderer.gameTestDebugRenderer.render(matrixStack, vtx, cameraX, cameraY, cameraZ);
            vtx.draw();
        }
         */
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
