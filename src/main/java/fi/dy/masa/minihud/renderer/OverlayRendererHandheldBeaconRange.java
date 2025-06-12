package fi.dy.masa.minihud.renderer;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;

import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.RendererToggle;

public class OverlayRendererHandheldBeaconRange extends OverlayRendererBase
{
    public static final OverlayRendererHandheldBeaconRange INSTANCE = new OverlayRendererHandheldBeaconRange();

    private boolean needsUpdate;
    protected int updateDistance = 2;
    // How often it updates in BlockPos changes

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
            this.calculateBeaconBoxForPlayer(entity.getWorld(), entity, mc);

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
        BufferBuilder builder = ctx.start(() -> "minihud:held_beacon/quads", MaLiLibPipelines.POSITION_COLOR_MASA_LEQUAL_DEPTH_OFFSET_1);
        MatrixStack matrices = new MatrixStack();

        matrices.push();

        RenderUtils.drawBoxAllSidesBatchedQuads(this.box, Color4f.fromColor(color.intValue, 0.3f), builder);

        try
        {
            BuiltBuffer meshData = builder.endNullable();

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
        BufferBuilder builder = ctx.start(() -> "minihud:held_beacon/outlines", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH);
//        MatrixStack matrices = new MatrixStack();

//        matrices.push();
        RenderUtils.drawBoxAllEdgesBatchedLines(this.box, Color4f.fromColor(color.intValue, 1f), builder);

        try
        {
            BuiltBuffer meshData = builder.endNullable();

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

//        matrices.pop();
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

    private void calculateBeaconBoxForPlayer(World world, Entity entity, MinecraftClient mc)
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
        float maxY = (float) (y + range + 24);  // the getYTop doesn't seem to work here
        float maxZ = (float) (z + range + 1);

        this.box = new Box(minX, minY, minZ, maxX, maxY, maxZ);
        this.hasData = true;
    }
}
