package fi.dy.masa.minihud.renderer;

import fi.dy.masa.malilib.util.*;
import fi.dy.masa.minihud.gui.InventoryOverlayScreen;
import fi.dy.masa.minihud.renderer.shapes.SideQuad;
import fi.dy.masa.minihud.util.RayTraceUtils;
import fi.dy.masa.minihud.util.ShapeRenderType;
import fi.dy.masa.minihud.util.shape.SphereUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.Collection;

public class RenderUtils
{
    public static void renderWallsWithLines(
            BlockPos posStart,
            BlockPos posEnd,
            Vec3d cameraPos,
            double lineIntervalH,
            double lineIntervalV,
            boolean alignLinesToModulo,
            Color4f color,
            BufferBuilder bufferQuads, BufferBuilder bufferLines)
    {
        Entity entity = EntityUtils.getCameraEntity();
        final int boxMinX = Math.min(posStart.getX(), posEnd.getX());
        final int boxMinZ = Math.min(posStart.getZ(), posEnd.getZ());
        final int boxMaxX = Math.max(posStart.getX(), posEnd.getX());
        final int boxMaxZ = Math.max(posStart.getZ(), posEnd.getZ());

        final int centerX = (int) Math.floor(entity.getX());
        final int centerZ = (int) Math.floor(entity.getZ());
        final int maxDist = MinecraftClient.getInstance().options.getViewDistance().getValue() * 32; // double the view distance in blocks
        final int rangeMinX = centerX - maxDist;
        final int rangeMinZ = centerZ - maxDist;
        final int rangeMaxX = centerX + maxDist;
        final int rangeMaxZ = centerZ + maxDist;
        final double minY = Math.min(posStart.getY(), posEnd.getY());
        final double maxY = Math.max(posStart.getY(), posEnd.getY()) + 1;
        double minX, minZ, maxX, maxZ;

        // The sides of the box along the x-axis can be at least partially inside the range
        if (rangeMinX <= boxMaxX && rangeMaxX >= boxMinX)
        {
            minX = Math.max(boxMinX, rangeMinX);
            maxX = Math.min(boxMaxX, rangeMaxX) + 1;

            if (rangeMinZ <= boxMinZ && rangeMaxZ >= boxMinZ)
            {
                minZ = maxZ = boxMinZ;
                renderWallWithLines((float) minX, (float) minY, (float) minZ, (float) maxX, (float) maxY, (float) maxZ, lineIntervalH, lineIntervalV, alignLinesToModulo, cameraPos, color, bufferQuads, bufferLines);
            }

            if (rangeMinZ <= boxMaxZ && rangeMaxZ >= boxMaxZ)
            {
                minZ = maxZ = boxMaxZ + 1;
                renderWallWithLines((float) minX, (float) minY, (float) minZ, (float) maxX, (float) maxY, (float) maxZ, lineIntervalH, lineIntervalV, alignLinesToModulo, cameraPos, color, bufferQuads, bufferLines);
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
                renderWallWithLines((float) minX, (float) minY, (float) minZ, (float) maxX, (float) maxY, (float) maxZ, lineIntervalH, lineIntervalV, alignLinesToModulo, cameraPos, color, bufferQuads, bufferLines);
            }

            if (rangeMinX <= boxMaxX && rangeMaxX >= boxMaxX)
            {
                minX = maxX = boxMaxX + 1;
                renderWallWithLines((float) minX, (float) minY, (float) minZ, (float) maxX, (float) maxY, (float) maxZ, lineIntervalH, lineIntervalV, alignLinesToModulo, cameraPos, color, bufferQuads, bufferLines);
            }
        }
    }

    public static void renderWallWithLines(
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ,
            double lineIntervalH, double lineIntervalV,
            boolean alignLinesToModulo,
            Vec3d cameraPos,
            Color4f color,
            BufferBuilder bufferQuads, BufferBuilder bufferLines)
    {
        float cx = (float) cameraPos.x;
        float cy = (float) cameraPos.y;
        float cz = (float) cameraPos.z;

        bufferQuads.vertex(minX - cx, maxY - cy, minZ - cz).color(color.r, color.g, color.b, color.a);
        bufferQuads.vertex(minX - cx, minY - cy, minZ - cz).color(color.r, color.g, color.b, color.a);
        bufferQuads.vertex(maxX - cx, minY - cy, maxZ - cz).color(color.r, color.g, color.b, color.a);
        bufferQuads.vertex(maxX - cx, maxY - cy, maxZ - cz).color(color.r, color.g, color.b, color.a);

        if (lineIntervalV > 0.0)
        {
            double lineY = alignLinesToModulo ? roundUp(minY, lineIntervalV) : minY;

            while (lineY <= maxY)
            {
                bufferLines.vertex(minX - cx, (float) (lineY - cy), minZ - cz).color(color.r, color.g, color.b, 1.0F);
                bufferLines.vertex(maxX - cx, (float) (lineY - cy), maxZ - cz).color(color.r, color.g, color.b, 1.0F);
                lineY += lineIntervalV;
            }
        }

        if (lineIntervalH > 0.0)
        {
            if (minX == maxX)
            {
                double lineZ = alignLinesToModulo ? roundUp(minZ, lineIntervalH) : minZ;

                while (lineZ <= maxZ)
                {
                    bufferLines.vertex(minX - cx, minY - cy, (float) (lineZ - cz)).color(color.r, color.g, color.b, 1.0F);
                    bufferLines.vertex(minX - cx, maxY - cy, (float) (lineZ - cz)).color(color.r, color.g, color.b, 1.0F);
                    lineZ += lineIntervalH;
                }
            }
            else if (minZ == maxZ)
            {
                double lineX = alignLinesToModulo ? roundUp(minX, lineIntervalH) : minX;

                while (lineX <= maxX)
                {
                    bufferLines.vertex((float) (lineX - cx), minY - cy, minZ - cz).color(color.r, color.g, color.b, 1.0F);
                    bufferLines.vertex((float) (lineX - cx), maxY - cy, minZ - cz).color(color.r, color.g, color.b, 1.0F);
                    lineX += lineIntervalH;
                }
            }
        }
    }


    /**
     * Assumes a BufferBuilder in GL_QUADS mode has been initialized
     */
    public static void drawBlockSpaceSideBatchedQuads(long posLong, Direction side,
                                                      Color4f color, double expand,
                                                      Vec3d cameraPos, BufferBuilder buffer)
    {
        int x = BlockPos.unpackLongX(posLong);
        int y = BlockPos.unpackLongY(posLong);
        int z = BlockPos.unpackLongZ(posLong);
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
                buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a);
                break;

            case UP:
                buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a);
                break;

            case NORTH:
                buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a);
                break;

            case SOUTH:
                buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
                break;

            case WEST:
                buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a);
                break;

            case EAST:
                buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
                break;
        }
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
                    int adjX = BlockPos.unpackLongX(adjPosLong);
                    int adjY = BlockPos.unpackLongY(adjPosLong);
                    int adjZ = BlockPos.unpackLongZ(adjPosLong);
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

    public static void renderQuads(Collection<SideQuad> quads, Color4f color, double expand,
                                   Vec3d cameraPos, BufferBuilder buffer)
    {
        for (SideQuad quad : quads)
        {
            RenderUtils.renderInsetQuad(quad.startPos(), quad.width(), quad.height(), quad.side(),
                                        -expand, color, cameraPos, buffer);
        }
        //System.out.printf("merged: rendered %d quads\n", quads.size());
    }

    public static void renderInsetQuad(Vec3i minPos, int width, int height, Direction side,
                                       double inset, Color4f color, Vec3d cameraPos, BufferBuilder buffer)
    {
        renderInsetQuad(minPos.getX(), minPos.getY(), minPos.getZ(), width, height, side, inset, color, cameraPos, buffer);
    }

    public static void renderInsetQuad(long minPos, int width, int height, Direction side,
                                       double inset, Color4f color, Vec3d cameraPos, BufferBuilder buffer)
    {
        int x = BlockPos.unpackLongX(minPos);
        int y = BlockPos.unpackLongY(minPos);
        int z = BlockPos.unpackLongZ(minPos);

        renderInsetQuad(x, y, z, width, height, side, inset, color, cameraPos, buffer);
    }

    public static void renderInsetQuad(int x, int y, int z, int width, int height, Direction side,
                                       double inset, Color4f color, Vec3d cameraPos, BufferBuilder buffer)
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
                buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a);
                break;
            case EAST:
                maxX += (float) (1 - inset);
                buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a);
                break;

            case NORTH:
                minZ += (float) inset;
                buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a);
                break;

            case SOUTH:
                maxZ += (float) (1 - inset);
                buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a);
                break;

            case DOWN:
                minY += (float) inset;
                buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a);
                break;

            case UP:
                maxY += (float) (1 - inset);
                buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
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
            buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, 1f);
            buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, 1f);

            for (float z = minZ; z < maxZ + 0.5; z += 1.0F)
            {
                buffer.vertex(minX, minY, z).color(color.r, color.g, color.b, 1f);
                buffer.vertex(maxX, maxY, z).color(color.r, color.g, color.b, 1f);
            }
        }
        else
        {
            // Vertical line at the "start" end of the quad
            buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, 1f);
            buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, 1f);

            for (float y = minY; y < maxY + 0.5; y += 1.0F)
            {
                buffer.vertex(minX, y, minZ).color(color.r, color.g, color.b, 1f);
                buffer.vertex(maxX, y, maxZ).color(color.r, color.g, color.b, 1f);
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

    public static void renderInventoryOverlay(RayTraceUtils.InventoryPreviewData inventory, DrawContext drawContext)
    {
        var screen = new InventoryOverlayScreen(inventory);
        screen.init(MinecraftClient.getInstance(), 0, 0);
        screen.render(drawContext, 0, 0, 0);
    }
}
