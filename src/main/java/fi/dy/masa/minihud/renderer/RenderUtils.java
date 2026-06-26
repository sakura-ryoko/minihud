package fi.dy.masa.minihud.renderer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.position.*;
import fi.dy.masa.minihud.renderer.shapes.SideQuad;
import fi.dy.masa.minihud.util.ShapeRenderType;
import fi.dy.masa.minihud.util.shape.SphereUtils;

public class RenderUtils
{
    public static List<AABB> calculateBoxes(
            BlockPos posStart,
            BlockPos posEnd)
    {
        Entity entity = EntityUtils.getCameraEntity();
        if (entity == null) return List.of();
        final int boxMinX = Math.min(posStart.getX(), posEnd.getX());
        final int boxMinZ = Math.min(posStart.getZ(), posEnd.getZ());
        final int boxMaxX = Math.max(posStart.getX(), posEnd.getX());
        final int boxMaxZ = Math.max(posStart.getZ(), posEnd.getZ());

        final int centerX = (int) Math.floor(entity.getX());
        final int centerZ = (int) Math.floor(entity.getZ());
        final int maxDist = Minecraft.getInstance().options.renderDistance().get() * 32; // double the view distance in blocks
        final int rangeMinX = centerX - maxDist;
        final int rangeMinZ = centerZ - maxDist;
        final int rangeMaxX = centerX + maxDist;
        final int rangeMaxZ = centerZ + maxDist;
        final double minY = Math.min(posStart.getY(), posEnd.getY());
        final double maxY = Math.max(posStart.getY(), posEnd.getY()) + 1;
        double minX, minZ, maxX, maxZ;

        List<AABB> boxes = new ArrayList<>();

        // The sides of the box along the x-axis can be at least partially inside the range
        if (rangeMinX <= boxMaxX && rangeMaxX >= boxMinX)
        {
            minX = Math.max(boxMinX, rangeMinX);
            maxX = Math.min(boxMaxX, rangeMaxX) + 1;

            if (rangeMinZ <= boxMinZ && rangeMaxZ >= boxMinZ)
            {
                minZ = maxZ = boxMinZ;
                boxes.add(new AABB(minX, minY, minZ, maxX, maxY, maxZ));
            }

            if (rangeMinZ <= boxMaxZ && rangeMaxZ >= boxMaxZ)
            {
                minZ = maxZ = boxMaxZ + 1;
                boxes.add(new AABB(minX, minY, minZ, maxX, maxY, maxZ));
            }
        }

        // The sides of the box along the z-axis can be at least partially inside the range
        if (rangeMinZ <= boxMaxZ && rangeMaxZ >= boxMinZ)
        {
            minZ = Math.max(boxMinZ, rangeMinZ);
            maxZ = Math.min(boxMaxZ, rangeMaxZ) + 1;

            if (rangeMinX <= boxMinX && rangeMaxX >= boxMinX)
            {
                minX = maxX = boxMinX;
                boxes.add(new AABB(minX, minY, minZ, maxX, maxY, maxZ));
            }

            if (rangeMinX <= boxMaxX && rangeMaxX >= boxMaxX)
            {
                minX = maxX = boxMaxX + 1;
                boxes.add(new AABB(minX, minY, minZ, maxX, maxY, maxZ));
            }
        }

        return boxes;
    }

    public static void renderWallQuads(AABB box, Vec3d cameraPos, Color4f color,
                                       BufferBuilder bufferQuads)
    {
        double cx = cameraPos.x;
        double cy = cameraPos.y;
        double cz = cameraPos.z;

        bufferQuads.addVertex((float) (box.minX - cx), (float) (box.maxY - cy), (float) (box.minZ - cz)).setColor(color.r, color.g, color.b, color.a);
        bufferQuads.addVertex((float) (box.minX - cx), (float) (box.minY - cy), (float) (box.minZ - cz)).setColor(color.r, color.g, color.b, color.a);
        bufferQuads.addVertex((float) (box.maxX - cx), (float) (box.minY - cy), (float) (box.maxZ - cz)).setColor(color.r, color.g, color.b, color.a);
        bufferQuads.addVertex((float) (box.maxX - cx), (float) (box.maxY - cy), (float) (box.maxZ - cz)).setColor(color.r, color.g, color.b, color.a);
    }

    public static void renderWallOutlines(
            AABB box,
            double lineIntervalH, double lineIntervalV,
            boolean alignLinesToModulo,
            Vec3d cameraPos,
            Color4f color,
			float lineWidth,
            BufferBuilder bufferLines)
    {
        double cx = cameraPos.x;
        double cy = cameraPos.y;
        double cz = cameraPos.z;

        if (lineIntervalV > 0.0)
        {
            double lineY = alignLinesToModulo ? roundUp(box.minY, lineIntervalV) : box.minY;

            while (lineY <= box.maxY)
            {
                bufferLines.addVertex((float) (box.minX - cx), (float) (lineY - cy), (float) (box.minZ - cz)).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                bufferLines.addVertex((float) (box.maxX - cx), (float) (lineY - cy), (float) (box.maxZ - cz)).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

                lineY += lineIntervalV;
            }
        }

        if (lineIntervalH > 0.0)
        {
            if (box.minX == box.maxX)
            {
                double lineZ = alignLinesToModulo ? roundUp(box.minZ, lineIntervalH) : box.minZ;

                while (lineZ <= box.maxZ)
                {
                    bufferLines.addVertex((float) (box.minX - cx), (float) (box.minY - cy), (float) (lineZ - cz)).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                    bufferLines.addVertex((float) (box.minX - cx), (float) (box.maxY - cy), (float) (lineZ - cz)).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

                    lineZ += lineIntervalH;
                }
            }
            else if (box.minZ == box.maxZ)
            {
                double lineX = alignLinesToModulo ? roundUp(box.minX, lineIntervalH) : box.minX;

                while (lineX <= box.maxX)
                {
                    bufferLines.addVertex((float) (lineX - cx), (float) (box.minY - cy), (float) (box.minZ - cz)).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                    bufferLines.addVertex((float) (lineX - cx), (float) (box.maxY - cy), (float) (box.minZ - cz)).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

                    lineX += lineIntervalH;
                }
            }
        }
    }

    public static void drawBoxQuads(IntBoundingBox bb, Vec3d cameraPos, Color4f color,
                                    BufferBuilder bufferQuads)
    {
        float minX = (float) (bb.minX() - cameraPos.x);
        float minY = (float) (bb.minY() - cameraPos.y);
        float minZ = (float) (bb.minZ() - cameraPos.z);
        float maxX = (float) (bb.maxX() + 1 - cameraPos.x);
        float maxY = (float) (bb.maxY() + 1 - cameraPos.y);
        float maxZ = (float) (bb.maxZ() + 1 - cameraPos.z);

        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllSidesBatchedQuads(minX, minY, minZ, maxX, maxY, maxZ, color, bufferQuads);
    }

	public static void drawBoxOutlines(IntBoundingBox bb, Vec3d cameraPos, Color4f color,
									   float lineWidth,
	                                   BufferBuilder bufferQuads)
	{
		float minX = (float) (bb.minX() - cameraPos.x);
		float minY = (float) (bb.minY() - cameraPos.y);
		float minZ = (float) (bb.minZ() - cameraPos.z);
		float maxX = (float) (bb.maxX() + 1 - cameraPos.x);
		float maxY = (float) (bb.maxY() + 1 - cameraPos.y);
		float maxZ = (float) (bb.maxZ() + 1 - cameraPos.z);

		fi.dy.masa.malilib.render.RenderUtils.drawBoxAllEdgesBatchedLines(minX, minY, minZ, maxX, maxY, maxZ, color, lineWidth, bufferQuads);
	}

	/**
     * Assumes a BufferBuilder in GL_QUADS mode has been initialized
     */
    public static void drawBlockSpaceSideBatchedQuads(long posLong, Direction side,
                                                      Color4f color, double expand,
                                                      Vec3d cameraPos,
                                                      BufferBuilder buffer)
    {
        int x = BlockPos.getX(posLong);
        int y = BlockPos.getY(posLong);
        int z = BlockPos.getZ(posLong);
        float offsetX = (float) (x - cameraPos.x);
        float offsetY = (float) (y - cameraPos.y);
        float offsetZ = (float) (z - cameraPos.z);
        float minX = (float) (offsetX - expand);
        float minY = (float) (offsetY - expand);
        float minZ = (float) (offsetZ - expand);
        float maxX = (float) (offsetX + expand + 1);
        float maxY = (float) (offsetY + expand + 1);
        float maxZ = (float) (offsetZ + expand + 1);

        switch (side)
        {
            case DOWN:
                buffer.addVertex(maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
                break;

            case UP:
                buffer.addVertex(minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);
                break;

            case NORTH:
                buffer.addVertex(maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);
                break;

            case SOUTH:
                buffer.addVertex(minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a);
                break;

            case WEST:
                buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);
                break;

            case EAST:
                buffer.addVertex(maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a);
                break;
        }
    }

    public static void drawBlockSpaceSideBatchedLines(long posLong, Direction side,
                                                      Color4f color, double expand, Vec3d cameraPos,
													  float lineWidth,
                                                      BufferBuilder buffer)
    {
        int x = BlockPos.getX(posLong);
        int y = BlockPos.getY(posLong);
        int z = BlockPos.getZ(posLong);
        float offsetX = (float) (x - cameraPos.x);
        float offsetY = (float) (y - cameraPos.y);
        float offsetZ = (float) (z - cameraPos.z);
        float minX = (float) (offsetX - expand);
        float minY = (float) (offsetY - expand);
        float minZ = (float) (offsetZ - expand);
        float maxX = (float) (offsetX + expand + 1);
        float maxY = (float) (offsetY + expand + 1);
        float maxZ = (float) (offsetZ + expand + 1);

        switch (side)
        {
            case DOWN:
                buffer.addVertex(maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                break;
            case UP:
                buffer.addVertex(minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                break;
            case NORTH:
                buffer.addVertex(maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                break;
            case SOUTH:
                buffer.addVertex(minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                break;
            case WEST:
                buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                break;
            case EAST:
                buffer.addVertex(maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                break;
        }
    }

    public static void renderCircleBlockOutlines(LongOpenHashSet positions,
                                                 Direction[] sides,
                                                 SphereUtils.RingPositionTest test,
                                                 ShapeRenderType renderType,
                                                 LayerRange range,
                                                 Color4f color,
                                                 double expand,
                                                 Vec3d cameraPos,
												 float lineWidth,
                                                 BufferBuilder buffer)
    {
        boolean full = renderType == ShapeRenderType.FULL_BLOCK;
        boolean outer = renderType == ShapeRenderType.OUTER_EDGE;
        boolean inner = renderType == ShapeRenderType.INNER_EDGE;
        //int count = 0;

        for (long posLong : positions)
        {
            if (range.isPositionWithinRange(posLong) == false)
            {
                continue;
            }

            for (Direction side : sides)
            {
                long adjPosLong = BlockPos.offset(posLong, side);

                if (positions.contains(adjPosLong))
                {
                    continue;
                }

                boolean render = full;

                if (full == false)
                {
                    int adjX = BlockPos.getX(adjPosLong);
                    int adjY = BlockPos.getY(adjPosLong);
                    int adjZ = BlockPos.getZ(adjPosLong);
                    boolean onOrIn = test.isInsideOrCloserThan(adjX, adjY, adjZ, side);
                    render = ((outer && onOrIn == false) || (inner && onOrIn));
                }

                if (render)
                {
                    RenderUtils.drawBlockSpaceSideBatchedLines(posLong, side, color, expand, cameraPos, lineWidth, buffer);
                    //++count;
                }
            }
        }
        //System.out.printf("individual: rendered %d quads\n", count);
    }

    public static void renderCircleBlockPositions(LongOpenHashSet positions,
                                                  Direction[] sides,
                                                  SphereUtils.RingPositionTest test,
                                                  ShapeRenderType renderType,
                                                  LayerRange range,
                                                  Color4f color,
                                                  double expand,
                                                  Vec3d cameraPos,
                                                  BufferBuilder buffer)
    {
        boolean full = renderType == ShapeRenderType.FULL_BLOCK;
        boolean outer = renderType == ShapeRenderType.OUTER_EDGE;
        boolean inner = renderType == ShapeRenderType.INNER_EDGE;
        //int count = 0;

        for (long posLong : positions)
        {
            if (range.isPositionWithinRange(posLong) == false)
            {
                continue;
            }

            for (Direction side : sides)
            {
                long adjPosLong = BlockPos.offset(posLong, side);

                if (positions.contains(adjPosLong))
                {
                    continue;
                }

                boolean render = full;

                if (full == false)
                {
                    int adjX = BlockPos.getX(adjPosLong);
                    int adjY = BlockPos.getY(adjPosLong);
                    int adjZ = BlockPos.getZ(adjPosLong);
                    boolean onOrIn = test.isInsideOrCloserThan(adjX, adjY, adjZ, side);
                    render = ((outer && onOrIn == false) || (inner && onOrIn));
                }

                if (render)
                {
                    RenderUtils.drawBlockSpaceSideBatchedQuads(posLong, side, color, expand, cameraPos, buffer);
                    //++count;
                }
            }
        }
        //System.out.printf("individual: rendered %d quads\n", count);
    }

    public static void renderBlockPositions(LongOpenHashSet positions,
                                            LayerRange range,
                                            Color4f color,
                                            double expand,
                                            Vec3d cameraPos,
                                            BufferBuilder buffer)
    {
        //int count = 0;
        for (long posLong : positions)
        {
            if (range.isPositionWithinRange(posLong) == false)
            {
                continue;
            }

            for (Direction side : PositionUtils.ALL_DIRECTIONS)
            {
                long adjPosLong = BlockPos.offset(posLong, side);

                if (positions.contains(adjPosLong))
                {
                    continue;
                }

                RenderUtils.drawBlockSpaceSideBatchedQuads(posLong, side, color, expand, cameraPos, buffer);
                //++count;
            }
        }
        //System.out.printf("individual: rendered %d quads\n", count);
    }

    public static void renderBlockPositionOutlines(LongOpenHashSet positions,
                                                   LayerRange range,
                                                   Color4f color,
                                                   double expand,
                                                   Vec3d cameraPos,
												   float lineWidth,
                                                   BufferBuilder buffer)
    {
        //int count = 0;
        for (long posLong : positions)
        {
            if (range.isPositionWithinRange(posLong) == false)
            {
                continue;
            }

            for (Direction side : PositionUtils.ALL_DIRECTIONS)
            {
                long adjPosLong = BlockPos.offset(posLong, side);

                if (positions.contains(adjPosLong))
                {
                    continue;
                }

                RenderUtils.drawBlockSpaceSideBatchedLines(posLong, side, color, expand, cameraPos, lineWidth, buffer);
                //++count;
            }
        }

        //System.out.printf("individual: rendered %d quads\n", count);
    }

    public static void renderQuads(Collection<SideQuad> quads, Color4f color, double expand,
                                   Vec3d cameraPos,
                                   BufferBuilder buffer)
    {
        for (SideQuad quad : quads)
        {
            renderInsetQuad(quad.startPos(), quad.width(), quad.height(), quad.side(),
                                        -expand, color, cameraPos, buffer);
        }
        //System.out.printf("merged: rendered %d quads\n", quads.size());
    }

    public static void renderInsetQuad(Vec3i minPos, int width, int height, Direction side,
                                       double inset, Color4f color, Vec3d cameraPos,
                                       BufferBuilder buffer)
    {
        renderInsetQuad(minPos.getX(), minPos.getY(), minPos.getZ(), width, height, side, inset, color, cameraPos, buffer);
    }

    public static void renderInsetQuad(long minPos, int width, int height, Direction side,
                                       double inset, Color4f color, Vec3d cameraPos,
                                       BufferBuilder buffer)
    {
        int x = BlockPos.getX(minPos);
        int y = BlockPos.getY(minPos);
        int z = BlockPos.getZ(minPos);

        renderInsetQuad(x, y, z, width, height, side, inset, color, cameraPos, buffer);
    }

    public static void renderInsetQuad(int x, int y, int z, int width, int height, Direction side,
                                       double inset, Color4f color, Vec3d cameraPos,
                                       BufferBuilder buffer)
    {
        float minX = (float) (x - cameraPos.x);
        float minY = (float) (y - cameraPos.y);
        float minZ = (float) (z - cameraPos.z);
        float maxX = minX;
        float maxY = minY;
        float maxZ = minZ;

        if (side.getAxis() == Direction.Axis.Z)
        {
            maxX += width;
            maxY += height;
        }
        else if (side.getAxis() == Direction.Axis.X)
        {
            maxY += height;
            maxZ += width;
        }
        else if (side.getAxis() == Direction.Axis.Y)
        {
            maxX += width;
            maxZ += height;
        }

        switch (side)
        {
            case WEST:
                minX += (float) inset;
                buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a);
                break;
            case EAST:
                maxX += (float) (1 - inset);
                buffer.addVertex(maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);
                break;

            case NORTH:
                minZ += (float) inset;
                buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);
                break;

            case SOUTH:
                maxZ += (float) (1 - inset);
                buffer.addVertex(minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a);
                break;

            case DOWN:
                minY += (float) inset;
                buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
                break;

            case UP:
                maxY += (float) (1 - inset);
                buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a);
                break;
        }
    }

    public static void renderQuadLines(Collection<SideQuad> quads, Color4f color, double expand, Vec3d cameraPos,
									   float lineWidth,
                                       BufferBuilder buffer)
    {
        for (SideQuad quad : quads)
        {
            renderInsetQuadLines(quad.startPos(), quad.width(), quad.height(), quad.side(),
                                 -expand, color, cameraPos, lineWidth, buffer);
        }
        //System.out.printf("merged: rendered %d quads\n", quads.size());
    }

    public static void renderInsetQuadLines(Vec3i minPos, int width, int height, Direction side,
                                            double inset, Color4f color, Vec3d cameraPos,
                                            float lineWidth,
                                            BufferBuilder buffer)
    {
        renderInsetQuadLines(minPos.getX(), minPos.getY(), minPos.getZ(), width, height, side, inset, color, cameraPos, lineWidth, buffer);
    }

    public static void renderInsetQuadLines(long minPos, int width, int height, Direction side,
                                            double inset, Color4f color, Vec3d cameraPos,
                                            float lineWidth,
                                            BufferBuilder buffer)

    {
        int x = BlockPos.getX(minPos);
        int y = BlockPos.getY(minPos);
        int z = BlockPos.getZ(minPos);

        renderInsetQuadLines(x, y, z, width, height, side, inset, color, cameraPos, lineWidth, buffer);
    }

    public static void renderInsetQuadLines(int x, int y, int z, int width, int height, Direction side,
                                            double inset, Color4f color, Vec3d cameraPos,
                                            float lineWidth,
                                            BufferBuilder buffer)
    {
        float minX = (float) (x - cameraPos.x);
        float minY = (float) (y - cameraPos.y);
        float minZ = (float) (z - cameraPos.z);
        float maxX = minX;
        float maxY = minY;
        float maxZ = minZ;

        if (side.getAxis() == Direction.Axis.Z)
        {
            maxX += width;
            maxY += height;
        }
        else if (side.getAxis() == Direction.Axis.X)
        {
            maxY += height;
            maxZ += width;
        }
        else if (side.getAxis() == Direction.Axis.Y)
        {
            maxX += width;
            maxZ += height;
        }

        switch (side)
        {
            case WEST:
                minX += (float) inset;
                buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                break;
            case EAST:
                maxX += (float) (1 - inset);
                buffer.addVertex(maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                break;
            case NORTH:
                minZ += (float) inset;
                buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                break;
            case SOUTH:
                maxZ += (float) (1 - inset);
                buffer.addVertex(minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                break;
            case DOWN:
                minY += (float) inset;
                buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                break;
            case UP:
                maxY += (float) (1 - inset);
                buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                break;
        }
    }

    public static void renderBiomeBorderLines(Vec3i minPos,
                                              int width,
                                              int height,
                                              Direction side,
                                              double inset,
                                              Color4f color,
                                              Vec3d cameraPos,
                                              float lineWidth,
                                              BufferBuilder buffer)
    {
        float minX = (float) (minPos.getX() - cameraPos.x);
        float minY = (float) (minPos.getY() - cameraPos.y);
        float minZ = (float) (minPos.getZ() - cameraPos.z);

        switch (side)
        {
            case WEST   -> minX += (float) inset;
            case EAST   -> minX += (float) (1 - inset);
            case NORTH  -> minZ += (float) inset;
            case SOUTH  -> minZ += (float) (1 - inset);
            case DOWN   -> minY += (float) inset;
            case UP     -> minY += (float) (1 - inset);
        }

        float maxX = minX;
        float maxY = minY;
        float maxZ = minZ;

        if (side.getAxis() == Direction.Axis.Z)
        {
            maxX += width;
            maxY += height;
        }
        else if (side.getAxis() == Direction.Axis.X)
        {
            maxY += height;
            maxZ += width;
        }
        else if (side.getAxis() == Direction.Axis.Y)
        {
            maxX += width;
            maxZ += height;
        }

        if (side.getAxis() == Direction.Axis.Y)
        {
            // Line at the "start" end of the quad
            buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
            buffer.addVertex(minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

            for (float z = minZ; z < maxZ + 0.5; z += 1.0F)
            {
                buffer.addVertex(minX, minY, z).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(maxX, maxY, z).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
            }
        }
        else
        {
            // Vertical line at the "start" end of the quad
            buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
            buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

            for (float y = minY; y < maxY + 0.5; y += 1.0F)
            {
                buffer.addVertex(minX, y, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
                buffer.addVertex(maxX, y, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
            }
        }
    }

    public static double roundUp(double value, double interval)
    {
        if (interval == 0.0)
        {
            return 0.0;
        }
        else if (value == 0.0)
        {
            return interval;
        }
        else
        {
            if (value < 0.0)
            {
                interval *= -1.0;
            }

            double remainder = value % interval;

            return remainder == 0.0 ? value : value + interval - remainder;
        }
    }

    public static void drawBoxAllSidesBatchedQuads(AABB bb, Color4f color, BufferBuilder buffer)
    {
        float minX = (float) bb.minX;
        float minY = (float) bb.minY;
        float minZ = (float) bb.minZ;
        float maxX = (float) bb.maxX;
        float maxY = (float) bb.maxY;
        float maxZ = (float) bb.maxZ;

        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllSidesBatchedQuads(minX, minY, minZ, maxX, maxY, maxZ, color, buffer);
    }

    public static void drawBoxAllEdgesBatchedLines(AABB bb, Color4f color,
												   float lineWidth,
                                                   BufferBuilder buffer)
    {
        float minX = (float) bb.minX;
        float minY = (float) bb.minY;
        float minZ = (float) bb.minZ;
        float maxX = (float) bb.maxX;
        float maxY = (float) bb.maxY;
        float maxZ = (float) bb.maxZ;

        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllEdgesBatchedLines(minX, minY, minZ, maxX, maxY, maxZ, color, lineWidth, buffer);
    }
}
