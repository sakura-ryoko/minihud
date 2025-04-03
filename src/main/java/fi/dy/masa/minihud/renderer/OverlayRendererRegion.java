package fi.dy.masa.minihud.renderer;

import com.mojang.blaze3d.buffers.BufferUsage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;

import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;

public class OverlayRendererRegion extends OverlayRendererBase
{
    public static final OverlayRendererRegion INSTANCE = new OverlayRendererRegion();
    protected boolean needsUpdate = true;
    private Box box;
    private boolean hasData;

    protected OverlayRendererRegion()
    {
        this.box = null;
        this.hasData = false;
        this.useCulling = true;
        this.renderThrough = false;
    }

    public void setNeedsUpdate()
    {
        this.needsUpdate = true;
    }

    @Override
    public String getName()
    {
        return "Region";
    }

    @Override
    public boolean shouldRender(MinecraftClient mc)
    {
        return RendererToggle.OVERLAY_REGION_FILE.getBooleanValue();
    }

    @Override
    public boolean needsUpdate(Entity entity, MinecraftClient mc)
    {
        if (this.needsUpdate)
        {
            return true;
        }

        int ex = (int) Math.floor(entity.getX());
        int ez = (int) Math.floor(entity.getZ());
        int lx = this.lastUpdatePos.getX();
        int lz = this.lastUpdatePos.getZ();

        return (ex >> 9) != (lx >> 9) || (ez >> 9) != (lz >> 9) || Math.abs(lx - ex) > 16 || Math.abs(lz - ez) > 16;
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity, MinecraftClient mc, Profiler profiler)
    {
        this.calculateRegions(entity);

        if (this.hasData())
        {
            this.render(cameraPos, mc, profiler);
        }

        this.needsUpdate = false;
    }

    private void calculateRegions(Entity entity)
    {
        World world = entity.getEntityWorld();
        int minY = world != null ? world.getBottomY() : -64;
        int maxY = world != null ? world.getTopYInclusive() + 1 : 320;
        int rx = MathHelper.floor(entity.getX()) & ~0x1FF;
        int rz = MathHelper.floor(entity.getZ()) & ~0x1FF;
        BlockPos pos1 = new BlockPos(rx,       minY, rz      );
        BlockPos pos2 = new BlockPos(rx + 511, maxY, rz + 511);
        this.box = Box.enclosing(pos1, pos2);
        this.hasData = true;
    }

    @Override
    public boolean hasData()
    {
        return this.hasData && this.box != null;
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

        profiler.push("region_quads");
        Color4f color = Configs.Colors.REGION_OVERLAY_COLOR.getColor();
        RenderObjectVbo ctx = this.renderObjects.getFirst();
        BufferBuilder builder = ctx.start(() -> "Region Quads", MaLiLibPipelines.POSITION_COLOR_MASA_LESSER_DEPTH_OFFSET_1, BufferUsage.STATIC_WRITE);
        MatrixStack matrices = new MatrixStack();

        matrices.push();
        MatrixStack.Entry e = matrices.peek();

        RenderUtils.renderWallQuads(this.box, cameraPos, color, builder, e);

        try
        {
            BuiltBuffer meshData = builder.endNullable();

            if (meshData != null)
            {
                ctx.upload(meshData);
                ctx.startResorting(meshData, ctx.createVertexSorter(cameraPos));
                meshData.close();
            }
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererRegion#renderQuads(): Exception; {}", err.getMessage());
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

        profiler.push("region_outlines");
        Color4f color = Configs.Colors.REGION_OVERLAY_COLOR.getColor();
        RenderObjectVbo ctx = this.renderObjects.get(1);
        BufferBuilder builder = ctx.start(() -> "Region Lines", RenderPipelines.LINES, BufferUsage.STATIC_WRITE);
        MatrixStack matrices = new MatrixStack();

        matrices.push();
        MatrixStack.Entry e = matrices.peek();

        RenderUtils.renderWallOutlines(this.box, 16, 16, true, cameraPos, color, builder, e);

        try
        {
            BuiltBuffer meshData = builder.endNullable();

            if (meshData != null)
            {
                ctx.upload(meshData);
                meshData.close();
            }
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererRegion#renderOutlines(): Exception; {}", err.getMessage());
        }

        matrices.pop();
        profiler.pop();
    }

    @Override
    public void reset()
    {
        super.reset();
        this.box = null;
        this.hasData = false;
    }
}
