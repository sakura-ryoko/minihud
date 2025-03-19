package fi.dy.masa.minihud.renderer;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;

import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.RendererToggle;

public class OverlayRendererHandheldBeaconRange extends OverlayRendererBase
{
    public static final OverlayRendererHandheldBeaconRange INSTANCE = new OverlayRendererHandheldBeaconRange();

    private boolean needsUpdate;
    protected int updateDistance = 48;

    private int level;
    private Box box;
    private boolean hasData;

    protected OverlayRendererHandheldBeaconRange()
    {
        this.level = -1;
        this.useCulling = true;
        this.renderThrough = false;
        this.box = null;
        this.hasData = false;
    }

    @Override
    public String getName()
    {
        return "Handheld Beacon Range";
    }

    @Override
    public boolean shouldRender(MinecraftClient mc)
    {
        if (mc.player == null) return false;
        Item item = mc.player.getMainHandStack().getItem();

        if (RendererToggle.OVERLAY_BEACON_RANGE.getBooleanValue())
        {
            return item instanceof BlockItem && ((BlockItem) item).getBlock() == Blocks.BEACON;
        }

        return false;
    }

    public void setNeedsUpdate()
    {
        this.needsUpdate = true;
    }

    @Override
    public boolean needsUpdate(Entity entity, MinecraftClient mc)
    {
        return this.needsUpdate || this.lastUpdatePos == null ||
                Math.abs(entity.getX() - this.lastUpdatePos.getX()) > this.updateDistance ||
                Math.abs(entity.getZ() - this.lastUpdatePos.getZ()) > this.updateDistance ||
                Math.abs(entity.getY() - this.lastUpdatePos.getY()) > this.updateDistance;
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity, MinecraftClient mc, Profiler profiler)
    {
        if (RendererToggle.OVERLAY_BEACON_RANGE.getBooleanValue())
        {
            this.calculateBeaconBoxForPlayer(entity, mc);

            if (this.hasData())
            {
                this.render(cameraPos, mc, profiler);
            }
        }
    }

    @Override
    public boolean hasData()
    {
        return this.hasData && this.level > 0 && this.level < 5 && this.box != null;
    }

    @Override
    public void render(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        this.allocateBuffers();
        this.renderQuads(cameraPos, mc, profiler);
        this.renderOutlines(cameraPos, mc, profiler);
    }

    private void renderQuads(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        profiler.push("held_beacon_quads");
        Color4f color = OverlayRendererBeaconRange.getColorForLevel(this.level);

        RenderObjectVbo ctx = this.renderObjects.getFirst();
        BufferBuilder builder = ctx.start(() -> "Held Beacon Quads", MaLiLibPipelines.POSITION_COLOR_MASA_LESSER_DEPTH, BufferUsage.STATIC_WRITE);
        MatrixStack matrices = new MatrixStack();

        matrices.push();

        RenderUtils.drawBoxAllSidesBatchedQuads(this.box, Color4f.fromColor(color.intValue, 0.3f), builder);

        try
        {
            ctx.upload(builder.endNullable(), BufferType.VERTICES);
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererHandheldBeaconRange#renderQuads(): Exception; {}", err.getMessage());
        }

        matrices.pop();
        profiler.pop();
    }

    private void renderOutlines(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        profiler.push("held_beacon_outlines");
        Color4f color = OverlayRendererBeaconRange.getColorForLevel(this.level);

        RenderObjectVbo ctx = this.renderObjects.get(1);
        BufferBuilder builder = ctx.start(() -> "Held Beacon Lines", RenderPipelines.LINES, BufferUsage.STATIC_WRITE);
        MatrixStack matrices = new MatrixStack();

        matrices.push();
        RenderUtils.drawBoxAllEdgesBatchedLines(this.box, Color4f.fromColor(color.intValue, 1f), builder, matrices);

        try
        {
            ctx.upload(builder.endNullable(), BufferType.VERTICES);
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererHandheldBeaconRange#renderOutlines(): Exception; {}", err.getMessage());
        }

        matrices.pop();
        profiler.pop();
    }

    @Override
    public void reset()
    {
        super.reset();
        this.level = -1;
        this.box = null;
        this.hasData = false;
    }

    private void calculateBeaconBoxForPlayer(Entity entity, MinecraftClient mc)
    {
        if (mc.player == null) return;
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        double x = Math.floor(entity.getX()) - cameraPos.x;
        double y = Math.floor(entity.getY()) - cameraPos.y;
        double z = Math.floor(entity.getZ()) - cameraPos.z;
        // Use the slot number as the level if sneaking

        this.level = mc.player.isSneaking() ? Math.min(4, mc.player.getInventory().getSelectedSlot() + 1) : 4;
        float range = this.level * 10 + 10;
        float minX = (float) (x - range);
        float minY = (float) (y - range);
        float minZ = (float) (z - range);
        float maxX = (float) (x + range + 1);
        float maxY = (float) (y + 4);
        float maxZ = (float) (z + range + 1);

        this.box = new Box(minX, minY, minZ, maxX, maxY, maxZ);
        this.hasData = true;

        /*
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.polygonOffset(-3f, -3f);
        RenderSystem.enablePolygonOffset();
         */

//        fi.dy.masa.malilib.render.RenderUtils.blend(true);
//        fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);

        /*
        RenderSystem.polygonOffset(0f, 0f);
        RenderSystem.disablePolygonOffset();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
         */
//        RenderUtils.blend(false);
    }
}
