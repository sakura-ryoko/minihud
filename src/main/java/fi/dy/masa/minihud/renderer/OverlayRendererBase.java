package fi.dy.masa.minihud.renderer;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import com.google.gson.JsonObject;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11C;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

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
        this.renderObjects.add(this.createQuadsVbo());

        if (useOutlines)
        {
            this.renderObjects.add(this.createOutlinesVbo());
        }
    }

    protected RenderObjectVbo createQuadsVbo()
    {
        return new RenderObjectVbo(
                () -> this.getName()+" Quads",
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_COLOR,
                GameRenderer::getPositionColorProgram,
                VertexBuffer.Usage.STATIC);
    }

    protected RenderObjectVbo createTexturedQuadsVbo()
    {
        return new RenderObjectVbo(
                        () -> this.getName()+" Textured Quads",
                        VertexFormat.DrawMode.QUADS,
                        VertexFormats.POSITION_TEXTURE_COLOR,
                        GameRenderer::getPositionTexColorProgram,
                        VertexBuffer.Usage.STATIC);
    }

    protected RenderObjectVbo createOutlinesVbo()
    {
        return new RenderObjectVbo(
                        () -> this.getName() + " Outlines",
                        VertexFormat.DrawMode.DEBUG_LINES,
                        VertexFormats.POSITION_COLOR,
                        GameRenderer::getPositionColorProgram,
                        VertexBuffer.Usage.STATIC);
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

    protected void preRender()
    {
        if (this.renderThrough)
        {
            RenderSystem.disableDepthTest();
            //RenderSystem.depthMask(false);
        }
        else
        {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11C.GL_LEQUAL); // In case someone else was being cheeky.
        }

        if (this.useCulling)
        {
            RenderSystem.enableCull();
        }
        else
        {
            RenderSystem.disableCull();
        }

        RenderSystem.depthMask(false);
        RenderSystem.lineWidth(this.glLineWidth);
        RenderSystem.polygonOffset(-3f, -3f);
        RenderSystem.enablePolygonOffset();
        fi.dy.masa.malilib.render.RenderUtils.setupBlend();
        fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);
    }

    protected void postRender()
    {
        if (this.renderThrough)
        {
            RenderSystem.enableDepthTest();
            //RenderSystem.depthMask(true);
        }

        if (!this.useCulling)
        {
            RenderSystem.enableCull();
        }

        RenderSystem.polygonOffset(0f, 0f);
        RenderSystem.disablePolygonOffset();
        fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
    }

    @Override
    public void draw(Vec3d cameraPos, Matrix4f posMatrix, Matrix4f projMatrix)
    {
        for (RenderObjectVbo obj : this.renderObjects)
        {
            if (!obj.isStarted()) continue;

//            // TODO (nvidia only?)
//            if (this.shouldResort && obj.shouldResort())
//            {
//                obj.resortTranslucent(obj.createVertexSorter(cameraPos));
//            }

            this.preRender();
            obj.drawPost(posMatrix, projMatrix);
            this.postRender();
        }
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
