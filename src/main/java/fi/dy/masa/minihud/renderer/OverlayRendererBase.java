package fi.dy.masa.minihud.renderer;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.WorldUtils;

public abstract class OverlayRendererBase implements IOverlayRenderer
{
    protected final List<RenderObjectVbo> renderObjects = new ArrayList<>();
    protected boolean renderThrough;
    protected boolean useCulling;
    protected float glLineWidth;
    @Nullable protected BlockPos lastUpdatePos;
    private Vec3 updateCameraPos;
    protected boolean shouldResort;

    public OverlayRendererBase()
    {
//        this.glLineWidth = Configs.Generic.DEFAULT_GL_LINE_WIDTH.getFloatValue();
        this.glLineWidth = 1.6f;
        this.lastUpdatePos = BlockPos.ZERO;
        this.updateCameraPos = Vec3.ZERO;
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
    public final Vec3 getUpdatePosition()
    {
        return this.updateCameraPos;
    }

    @Override
    public final void setUpdatePosition(Vec3 cameraPosition)
    {
        this.updateCameraPos = cameraPosition;
    }

    protected int getTopYOverTerrain(Level world, BlockPos pos, int range)
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
                LevelChunk chunk = world.getChunk(cx, cz);
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
    public void draw(Vec3 cameraPos)
    {
        for (RenderObjectVbo obj : this.renderObjects)
        {
            if (!obj.isStarted()) continue;
            if (!obj.isUploaded()) continue;

            // TODO (nvidia only?)
            if (this.shouldResort && obj.shouldResort())
            {
                obj.resortTranslucent(obj.createVertexSorter(cameraPos));
            }

            obj.drawPost(null, false, false);
        }
    }

    @Override
    public void reset()
    {
        this.resetBuffers();
        this.glLineWidth = 1.0f;
        this.lastUpdatePos = BlockPos.ZERO;
        this.updateCameraPos = Vec3.ZERO;
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
