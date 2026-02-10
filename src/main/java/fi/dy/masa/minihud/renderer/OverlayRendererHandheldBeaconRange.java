package fi.dy.masa.minihud.renderer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.RendererToggle;
import net.minecraft.client.Minecraft;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class OverlayRendererHandheldBeaconRange extends OverlayRendererBase
{
    public static final OverlayRendererHandheldBeaconRange INSTANCE = new OverlayRendererHandheldBeaconRange();

    private boolean needsUpdate;
    protected int updateDistance = 2;
    // How often it updates in BlockPos changes

    private int level;
    private AABB box;
    private boolean hasData;

    protected OverlayRendererHandheldBeaconRange()
    {
        this.level = -1;
        this.useCulling = false;
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
    public boolean shouldRender(Minecraft mc)
    {
        if (mc.player == null) return false;
        Item item = mc.player.getMainHandItem().getItem();

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
    public boolean needsUpdate(Entity entity, Minecraft mc)
    {
        return this.needsUpdate || this.lastUpdatePos == null ||
                Math.abs(entity.getX() - this.lastUpdatePos.getX()) > this.updateDistance ||
                Math.abs(entity.getZ() - this.lastUpdatePos.getZ()) > this.updateDistance ||
                Math.abs(entity.getY() - this.lastUpdatePos.getY()) > this.updateDistance;
    }

    @Override
    public void update(Vec3 cameraPos, Entity entity, Minecraft mc, ProfilerFiller profiler)
    {
        if (RendererToggle.OVERLAY_BEACON_RANGE.getBooleanValue())
        {
            this.calculateBeaconBoxForPlayer(entity.level(), entity, mc);

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
    public void render(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        this.allocateBuffers();
        this.renderQuads(cameraPos, mc, profiler);
        this.renderOutlines(cameraPos, mc, profiler);
    }

    private void renderQuads(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null || mc.player == null)
        {
            return;
        }

        profiler.push("held_beacon_quads");
        Color4f color = OverlayRendererBeaconRange.getColorForLevel(this.level);

        RenderObjectVbo ctx = this.renderObjects.getFirst();
        BufferBuilder builder = ctx.start(() -> "minihud:held_beacon/quads", MaLiLibPipelines.MINIHUD_SHAPE_OFFSET_NO_CULL);
        PoseStack matrices = new PoseStack();

        matrices.pushPose();

        RenderUtils.drawBoxAllSidesBatchedQuads(this.box, Color4f.fromColor(color.intValue, 0.3f), builder);

        try
        {
            MeshData meshData = builder.build();

            if (meshData != null)
            {
                ctx.upload(meshData, this.shouldResort);

                if (this.shouldResort)
                {
                    ctx.startResorting(meshData, ctx.createVertexSorter(cameraPos));
                }

                meshData.close();
            }
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererHandheldBeaconRange#renderQuads(): Exception; {}", err.getMessage());
        }

        matrices.popPose();
        profiler.pop();
    }

    private void renderOutlines(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null || mc.player == null)
        {
            return;
        }

        profiler.push("held_beacon_outlines");
        final Color4f color = Color4f.fromColor(OverlayRendererBeaconRange.getColorForLevel(this.level), 0xFF);

        RenderObjectVbo ctx = this.renderObjects.get(1);
        BufferBuilder builder = ctx.start(() -> "minihud:held_beacon/outlines", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH);

        RenderUtils.drawBoxAllEdgesBatchedLines(this.box, color, this.glLineWidth, builder);

        try
        {
            MeshData meshData = builder.build();

            if (meshData != null)
            {
                ctx.upload(meshData, false);
                meshData.close();
            }
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererHandheldBeaconRange#renderOutlines(): Exception; {}", err.getMessage());
        }

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

    private void calculateBeaconBoxForPlayer(Level world, Entity entity, Minecraft mc)
    {
        if (mc.player == null) return;
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();
        double x = Math.floor(entity.getX()) - cameraPos.x;
        double y = Math.floor(entity.getY()) - cameraPos.y;
        double z = Math.floor(entity.getZ()) - cameraPos.z;
        // Use the slot number as the level if sneaking

        this.level = mc.player.isShiftKeyDown() ? Math.min(4, mc.player.getInventory().getSelectedSlot() + 1) : 4;
        float range = this.level * 10 + 10;
        float minX = (float) (x - range);
        float minY = (float) (y - range);
        float minZ = (float) (z - range);
        float maxX = (float) (x + range + 1);
        float maxY = (float) (y + range + 24);  // the getYTop doesn't seem to work here
        float maxZ = (float) (z + range + 1);

        this.box = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        this.hasData = true;
    }
}
