package fi.dy.masa.minihud.renderer;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.ShaderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;

import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.render.RenderContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.minihud.config.RendererToggle;

public class OverlayRenderer
{
    private static long loginTime;
    private static boolean canRender;

    public static void resetRenderTimeout()
    {
        canRender = false;
        loginTime = System.currentTimeMillis();
    }

    public static void renderOverlays(Matrix4f matrix4f, Matrix4f projMatrix, MinecraftClient mc, Frustum frustum, Camera camera, Fog fog, Profiler profiler)
    {
        Entity entity = EntityUtils.getCameraEntity();

        if (entity == null)
        {
            return;
        }

        if (canRender == false)
        {
            // Don't render before the player has been placed in the actual proper position,
            // otherwise some of the renderers mess up.
            // The magic 8.5, 65, 8.5 comes from the WorldClient constructor
            if (System.currentTimeMillis() - loginTime >= 5000 || entity.getX() != 8.5 || entity.getY() != 65 || entity.getZ() != 8.5)
            {
                canRender = true;
            }
            else
            {
                return;
            }
        }

        if (RendererToggle.OVERLAY_BEACON_RANGE.getBooleanValue())
        {
            profiler.push("BeaconRangeHeldItem");
            renderBeaconBoxForPlayerIfHoldingItem(entity, mc);
            profiler.pop();
        }

        RenderContainer.INSTANCE.render(entity, matrix4f, projMatrix, mc, camera, profiler);
    }


    public static void renderBeaconBoxForPlayerIfHoldingItem(Entity entity, MinecraftClient mc)
    {
        Item item = mc.player.getMainHandStack().getItem();

        if (item instanceof BlockItem && ((BlockItem) item).getBlock() == Blocks.BEACON)
        {
            renderBeaconBoxForPlayer(entity, mc);
            return;
        }

        item = mc.player.getMainHandStack().getItem();

        if (item instanceof BlockItem && ((BlockItem) item).getBlock() == Blocks.BEACON)
        {
            renderBeaconBoxForPlayer(entity, mc);
        }
    }

    private static void renderBeaconBoxForPlayer(Entity entity, MinecraftClient mc)
    {
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        double x = Math.floor(entity.getX()) - cameraPos.x;
        double y = Math.floor(entity.getY()) - cameraPos.y;
        double z = Math.floor(entity.getZ()) - cameraPos.z;
        // Use the slot number as the level if sneaking
        int level = mc.player.isSneaking() ? Math.min(4, mc.player.getInventory().getSelectedSlot() + 1) : 4;
        float range = level * 10 + 10;
        float minX = (float) (x - range);
        float minY = (float) (y - range);
        float minZ = (float) (z - range);
        float maxX = (float) (x + range + 1);
        float maxY = (float) (y + 4);
        float maxZ = (float) (z + range + 1);
        Color4f color = OverlayRendererBeaconRange.getColorForLevel(level);

        /*
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.polygonOffset(-3f, -3f);
        RenderSystem.enablePolygonOffset();
         */
        fi.dy.masa.malilib.render.RenderUtils.blend(true);
        fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);

        // RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        // ShaderPipelines.DEBUG_LINE_STRIP
        RenderContext ctx = new RenderContext(() -> "", VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS, GlUsage.STATIC_WRITE);
        ctx.setShader(MaLiLibPipelines.POSITION_COLOR_SIMPLE);
        BufferBuilder buffer = ctx.getBuilder();

        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllSidesBatchedQuads(minX, minY, minZ, maxX, maxY, maxZ, Color4f.fromColor(color.intValue, 0.3f), buffer);

        try
        {
            ctx.drawColor(buffer.endNullable());
            ctx.reset();
        }
        catch (Exception ignored) { }

        buffer = ctx.startNoShader(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES);
        ctx.setShader(MaLiLibPipelines.POSITION_COLOR_SIMPLE);

        //buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllEdgesBatchedLines(minX, minY, minZ, maxX, maxY, maxZ, Color4f.fromColor(color.intValue, 1f), buffer);

        try
        {
            ctx.drawColor(buffer.endNullable());
            ctx.close();
        }
        catch (Exception ignored) { }

        /*
        RenderSystem.polygonOffset(0f, 0f);
        RenderSystem.disablePolygonOffset();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
         */
        RenderUtils.blend(false);
    }
}
