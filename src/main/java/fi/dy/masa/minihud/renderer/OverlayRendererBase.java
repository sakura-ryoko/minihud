package fi.dy.masa.minihud.renderer;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import com.google.gson.JsonObject;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.WorldUtils;

public abstract class OverlayRendererBase implements IOverlayRenderer
{
    protected final List<RenderObjectVbo> renderObjects = new ArrayList<>();
    protected boolean renderThrough;
    protected boolean useCulling;
    protected float glLineWidth;
    @Nullable protected BlockPos lastUpdatePos;
    private Vec3d updateCameraPos;
    protected boolean shouldResort;

    public OverlayRendererBase()
    {
        this.glLineWidth = 1.0f;
        this.lastUpdatePos = BlockPos.ORIGIN;
        this.updateCameraPos = Vec3d.ZERO;
        this.renderThrough = false;
        this.useCulling = false;
        this.shouldResort = false;
    }

    protected void clearBuffers()
    {
        if (!this.renderObjects.isEmpty())
        {
            this.resetBuffers();
            this.renderObjects.clear();
        }
    }

    protected void allocateBuffers()
    {
        this.allocateBuffers(true);
    }

    protected void allocateBuffers(boolean useOutlines)
    {
        this.clearBuffers();
        this.renderObjects.add(new RenderObjectVbo(() -> this.getName()+"/Quads", MaLiLibPipelines.MINIHUD_SHAPE_OFFSET_NO_CULL));

        if (useOutlines)
        {
            this.renderObjects.add(new RenderObjectVbo(() -> this.getName() + "/Outlines", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH));
        }
    }

    protected void resetBuffers()
    {
        this.renderObjects.forEach(RenderObjectVbo::reset);
    }

    @Override
    public final Vec3d getUpdatePosition()
    {
        return this.updateCameraPos;
    }

    @Override
    public final void setUpdatePosition(Vec3d cameraPosition)
    {
        this.updateCameraPos = cameraPosition;
    }

//    protected void preRender()
//    {
//        RenderSystem.lineWidth(this.glLineWidth);
//
//        if (this.renderThrough)
//        {
//            RenderUtils.depthTest(false);
//            RenderUtils.depthMask(false);
//        }
//
//        if (this.useCulling)
//        {
//            RenderUtils.culling(true);
//        }
//    }

//    protected void postRender()
//    {
//        if (this.renderThrough)
//        {
//            RenderUtils.depthTest(true);
//            RenderUtils.depthMask(true);
//        }
//
//        if (this.useCulling)
//        {
//            RenderUtils.culling(false);
//        }
//    }

    protected int getTopYOverTerrain(World world, BlockPos pos, int range)
    {
        final int minX = pos.getX() - range;
        final int minZ = pos.getZ() - range;
        final int maxX = pos.getX() + range;
        final int maxZ = pos.getZ() + range;

        final int minCX = minX >> 4;
        final int minCZ = minZ >> 4;
        final int maxCX = maxX >> 4;
        final int maxCZ = maxZ >> 4;
        int maxY = 0;

        for (int cz = minCZ; cz <= maxCZ; ++cz)
        {
            for (int cx = minCX; cx <= maxCX; ++cx)
            {
                WorldChunk chunk = world.getChunk(cx, cz);
                int height = WorldUtils.getHighestSectionYOffset(chunk) + 15;

                if (height > maxY)
                {
                    maxY = height;
                }
            }
        }

        return maxY + 4;
    }

    @Override
    public void draw(Vec3d cameraPos)
    {
//        this.preRender();

        for (RenderObjectVbo obj : this.renderObjects)
        {
            if (!obj.isStarted()) continue;
            if (!obj.isUploaded()) continue;

            // TODO (nvidia only?)
            if (this.shouldResort && obj.shouldResort())
            {
                obj.resortTranslucent(obj.createVertexSorter(cameraPos));
            }

            if (obj.getDrawMode() == VertexFormat.DrawMode.LINES || obj.getDrawMode() == VertexFormat.DrawMode.DEBUG_LINES)
            {
                obj.lineWidth(this.glLineWidth);
                obj.drawPost(null, false, true);
            }
            else
            {
                obj.drawPost(null, false, false);
            }
        }

//        this.postRender();
    }

    @Override
    public void reset()
    {
        this.resetBuffers();
        this.glLineWidth = 1f;
        this.lastUpdatePos = BlockPos.ORIGIN;
        this.updateCameraPos = Vec3d.ZERO;
    }

    public void setRenderThrough(boolean renderThrough)
    {
        this.renderThrough = renderThrough;
    }

    public String getSaveId()
    {
        return "";
    }

    @Nullable
    public JsonObject toJson()
    {
        return null;
    }

    public void fromJson(JsonObject obj)
    {
    }
}
