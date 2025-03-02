package fi.dy.masa.minihud.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;

public class OverlayRendererRegion extends OverlayRendererBase
{
    protected static boolean needsUpdate = true;

    public static void setNeedsUpdate()
    {
        needsUpdate = true;
    }

    public OverlayRendererRegion()
    {
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
        if (needsUpdate)
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
    public void update(Vec3d cameraPos, Entity entity, MinecraftClient mc)
    {
        RenderObjectBase renderQuads = this.renderObjects.get(0);
        RenderObjectBase renderLines = this.renderObjects.get(1);
        /*
        BUFFER_1 = TESSELLATOR_1.begin(renderQuads.getGlMode(), VertexFormats.POSITION_COLOR);
        BUFFER_2 = TESSELLATOR_2.begin(renderLines.getGlMode(), VertexFormats.POSITION_COLOR);
         */

        BufferBuilder builder1 = CONTEXT_1.startNoShader(VertexFormats.POSITION_COLOR, renderQuads.getGlMode());
        BufferBuilder builder2 = CONTEXT_2.startNoShader(VertexFormats.POSITION_COLOR, renderLines.getGlMode());
        CONTEXT_1.setShader(MaLiLibPipelines.POSITION_COLOR_SIMPLE);
        CONTEXT_2.setShader(MaLiLibPipelines.POSITION_COLOR_SIMPLE);

        World world = entity.getEntityWorld();
        int minY = world != null ? world.getBottomY() : -64;
        int maxY = world != null ? world.getTopYInclusive() + 1 : 320;
        int rx = MathHelper.floor(entity.getX()) & ~0x1FF;
        int rz = MathHelper.floor(entity.getZ()) & ~0x1FF;
        BlockPos pos1 = new BlockPos(rx,       minY, rz      );
        BlockPos pos2 = new BlockPos(rx + 511, maxY, rz + 511);
        Color4f color = Configs.Colors.REGION_OVERLAY_COLOR.getColor();

        RenderUtils.renderWallsWithLines(pos1, pos2, cameraPos, 16, 16, true, color, builder1, builder2);

        CONTEXT_1 = CONTEXT_1.setBuilder(builder1);
        CONTEXT_2 = CONTEXT_2.setBuilder(builder2);

        renderQuads.uploadData(builder1);
        renderLines.uploadData(builder2);

        needsUpdate = false;
    }
}
