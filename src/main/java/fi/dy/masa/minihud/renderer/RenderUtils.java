package fi.dy.masa.minihud.renderer;

import java.util.*;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.phys.*;

import fi.dy.masa.malilib.mixin.entity.IMixinAbstractHorseEntity;
import fi.dy.masa.malilib.render.InventoryOverlay;
import fi.dy.masa.malilib.util.*;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.game.BlockUtils;
import fi.dy.masa.malilib.util.game.RayTraceUtils;
import fi.dy.masa.malilib.util.position.PositionUtils;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.data.EntitiesDataManager;
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
//        World world = entity.getEntityWorld();
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

    public static void renderWallQuads(AABB box, Vec3 cameraPos, Color4f color,
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
            Vec3 cameraPos,
            Color4f color,
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
                bufferLines.addVertex((float) (box.minX - cx), (float) (lineY - cy), (float) (box.minZ - cz)).setColor(color.r, color.g, color.b, 1.0F);
                bufferLines.addVertex((float) (box.maxX - cx), (float) (lineY - cy), (float) (box.maxZ - cz)).setColor(color.r, color.g, color.b, 1.0F);

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
                    bufferLines.addVertex((float) (box.minX - cx), (float) (box.minY - cy), (float) (lineZ - cz)).setColor(color.r, color.g, color.b, 1.0F);
                    bufferLines.addVertex((float) (box.minX - cx), (float) (box.maxY - cy), (float) (lineZ - cz)).setColor(color.r, color.g, color.b, 1.0F);

                    lineZ += lineIntervalH;
                }
            }
            else if (box.minZ == box.maxZ)
            {
                double lineX = alignLinesToModulo ? roundUp(box.minX, lineIntervalH) : box.minX;

                while (lineX <= box.maxX)
                {
                    bufferLines.addVertex((float) (lineX - cx), (float) (box.minY - cy), (float) (box.minZ - cz)).setColor(color.r, color.g, color.b, 1.0F);
                    bufferLines.addVertex((float) (lineX - cx), (float) (box.maxY - cy), (float) (box.minZ - cz)).setColor(color.r, color.g, color.b, 1.0F);

                    lineX += lineIntervalH;
                }
            }
        }
    }

    public static void drawBoxNoOutlines(IntBoundingBox bb, Vec3 cameraPos, Color4f color,
//                                         BufferBuilder bufferQuads, MatrixStack.Entry e)
                                         BufferBuilder bufferQuads)
    {
        float minX = (float) (bb.minX - cameraPos.x);
        float minY = (float) (bb.minY - cameraPos.y);
        float minZ = (float) (bb.minZ - cameraPos.z);
        float maxX = (float) (bb.maxX + 1 - cameraPos.x);
        float maxY = (float) (bb.maxY + 1 - cameraPos.y);
        float maxZ = (float) (bb.maxZ + 1 - cameraPos.z);

        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllSidesBatchedQuads(minX, minY, minZ, maxX, maxY, maxZ, color, bufferQuads);
    }

    /**
     * Assumes a BufferBuilder in GL_QUADS mode has been initialized
     */
    public static void drawBlockSpaceSideBatchedQuads(long posLong, Direction side,
                                                      Color4f color, double expand,
                                                      Vec3 cameraPos,
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
                                                      Color4f color, double expand, Vec3 cameraPos,
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

    public static void renderCircleBlockOutlines(LongOpenHashSet positions,
                                                 Direction[] sides,
                                                 SphereUtils.RingPositionTest test,
                                                 ShapeRenderType renderType,
                                                 LayerRange range,
                                                 Color4f color,
                                                 double expand,
                                                 Vec3 cameraPos,
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
                    RenderUtils.drawBlockSpaceSideBatchedLines(posLong, side, color, expand, cameraPos, buffer);
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
                                                  Vec3 cameraPos,
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
                                            Vec3 cameraPos,
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
                                                   Vec3 cameraPos,
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

                RenderUtils.drawBlockSpaceSideBatchedLines(posLong, side, color, expand, cameraPos, buffer);
                //++count;
            }
        }

        //System.out.printf("individual: rendered %d quads\n", count);
    }

    public static void renderQuads(Collection<SideQuad> quads, Color4f color, double expand,
                                   Vec3 cameraPos,
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
                                       double inset, Color4f color, Vec3 cameraPos,
                                       BufferBuilder buffer)
    {
        renderInsetQuad(minPos.getX(), minPos.getY(), minPos.getZ(), width, height, side, inset, color, cameraPos, buffer);
    }

    public static void renderInsetQuad(long minPos, int width, int height, Direction side,
                                       double inset, Color4f color, Vec3 cameraPos,
                                       BufferBuilder buffer)
    {
        int x = BlockPos.getX(minPos);
        int y = BlockPos.getY(minPos);
        int z = BlockPos.getZ(minPos);

        renderInsetQuad(x, y, z, width, height, side, inset, color, cameraPos, buffer);
    }

    public static void renderInsetQuad(int x, int y, int z, int width, int height, Direction side,
                                       double inset, Color4f color, Vec3 cameraPos,
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

    public static void renderQuadLines(Collection<SideQuad> quads, Color4f color, double expand, Vec3 cameraPos,
                                       BufferBuilder buffer)
    {
        for (SideQuad quad : quads)
        {
            renderInsetQuadLines(quad.startPos(), quad.width(), quad.height(), quad.side(),
                                 -expand, color, cameraPos, buffer);
        }
        //System.out.printf("merged: rendered %d quads\n", quads.size());
    }

    public static void renderInsetQuadLines(Vec3i minPos, int width, int height, Direction side,
                                            double inset, Color4f color, Vec3 cameraPos,
                                            BufferBuilder buffer)
    {
        renderInsetQuadLines(minPos.getX(), minPos.getY(), minPos.getZ(), width, height, side, inset, color, cameraPos, buffer);
    }

    public static void renderInsetQuadLines(long minPos, int width, int height, Direction side,
                                            double inset, Color4f color, Vec3 cameraPos,
                                            BufferBuilder buffer)

    {
        int x = BlockPos.getX(minPos);
        int y = BlockPos.getY(minPos);
        int z = BlockPos.getZ(minPos);

        renderInsetQuadLines(x, y, z, width, height, side, inset, color, cameraPos, buffer);
    }

    public static void renderInsetQuadLines(int x, int y, int z, int width, int height, Direction side,
                                            double inset, Color4f color, Vec3 cameraPos,
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

    public static void renderBiomeBorderLines(Vec3i minPos,
                                              int width,
                                              int height,
                                              Direction side,
                                              double inset,
                                              Color4f color,
                                              Vec3 cameraPos,
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
            buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, 1f);
            buffer.addVertex(minX, maxY, maxZ).setColor(color.r, color.g, color.b, 1f);

            for (float z = minZ; z < maxZ + 0.5; z += 1.0F)
            {
                buffer.addVertex(minX, minY, z).setColor(color.r, color.g, color.b, 1f);
                buffer.addVertex(maxX, maxY, z).setColor(color.r, color.g, color.b, 1f);
            }
        }
        else
        {
            // Vertical line at the "start" end of the quad
            buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, 1f);
            buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, 1f);

            for (float y = minY; y < maxY + 0.5; y += 1.0F)
            {
                buffer.addVertex(minX, y, minZ).setColor(color.r, color.g, color.b, 1f);
                buffer.addVertex(maxX, y, maxZ).setColor(color.r, color.g, color.b, 1f);
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
//                                                   BufferBuilder buffer, MatrixStack matrices)
                                                   BufferBuilder buffer)
    {
        float minX = (float) bb.minX;
        float minY = (float) bb.minY;
        float minZ = (float) bb.minZ;
        float maxX = (float) bb.maxX;
        float maxY = (float) bb.maxY;
        float maxZ = (float) bb.maxZ;

        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllEdgesBatchedLines(minX, minY, minZ, maxX, maxY, maxZ, color, buffer);
    }

    // OG Method (Works)
    @Deprecated
    public static void renderInventoryOverlay(Minecraft mc, GuiGraphics drawContext)
    {
        Level world = WorldUtils.getBestWorld(mc);
        Entity cameraEntity = EntityUtils.getCameraEntity();

        if (mc.player == null || world == null || cameraEntity == null)
        {
            return;
        }

        if (cameraEntity == mc.player && world instanceof ServerLevel)
        {
            // We need to get the player from the server world (if available, ie. in single player),
            // so that the player itself won't be included in the ray trace
            Entity serverPlayer = world.getPlayerByUUID(mc.player.getUUID());

            if (serverPlayer != null)
            {
                cameraEntity = serverPlayer;
            }
        }

        HitResult trace = RayTraceUtils.getRayTraceFromEntity(cameraEntity.level(), cameraEntity, ClipContext.Fluid.NONE);

		if (trace == null) return;
        BlockPos pos = null;
        Container inv = null;
        ShulkerBoxBlock shulkerBoxBlock = null;
        CrafterBlock crafterBlock = null;
        LivingEntity entityLivingBase = null;

        if (trace.getType() == HitResult.Type.BLOCK)
        {
            pos = ((BlockHitResult) trace).getBlockPos();
            Block blockTmp = world.getBlockState(pos).getBlock();

            if (blockTmp instanceof ShulkerBoxBlock)
            {
                shulkerBoxBlock = (ShulkerBoxBlock) blockTmp;
            }
            else if (blockTmp instanceof CrafterBlock)
            {
                crafterBlock = (CrafterBlock) blockTmp;
            }

            inv = fi.dy.masa.minihud.util.InventoryUtils.getInventory(world, pos);
        }
        else if (trace.getType() == HitResult.Type.ENTITY)
        {
            Entity entity = ((EntityHitResult) trace).getEntity();

            if (entity.level().isClientSide() &&
                Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue())
            {
                EntitiesDataManager.getInstance().requestEntity(world, entity.getId());
            }

            if (entity instanceof LivingEntity)
            {
                entityLivingBase = (LivingEntity) entity;
            }

            if (entity instanceof Container)
            {
                inv = (Container) entity;
            }
            else if (entity instanceof Villager)
            {
                inv = ((Villager) entity).getInventory();
            }
            else if (entity instanceof AbstractHorse)
            {
                inv = ((IMixinAbstractHorseEntity) entity).malilib_getHorseInventory();
            }
        }

        final boolean isWolf = (entityLivingBase instanceof Wolf);
        final int xCenter = GuiUtils.getScaledWindowWidth() / 2;
        final int yCenter = GuiUtils.getScaledWindowHeight() / 2;
        int x = xCenter - 52 / 2;
        int y = yCenter - 92;

        if (inv != null && inv.getContainerSize() > 0)
        {
            final boolean isHorse = (entityLivingBase instanceof AbstractHorse);
            final int totalSlots = isHorse ? inv.getContainerSize() - 1 : inv.getContainerSize();
            final int firstSlot = isHorse ? 1 : 0;

            final InventoryOverlay.InventoryRenderType type = (entityLivingBase instanceof Villager) ? InventoryOverlay.InventoryRenderType.VILLAGER : InventoryOverlay.getInventoryType(inv);
            final InventoryOverlay.InventoryProperties props = InventoryOverlay.getInventoryPropsTemp(type, totalSlots);
            final int rows = (int) Math.ceil((double) totalSlots / props.slotsPerRow);
            Set<Integer> lockedSlots = new HashSet<>();
            int xInv = xCenter - (props.width / 2);
            int yInv = yCenter - props.height - 6;

            if (rows > 6)
            {
                yInv -= (rows - 6) * 18;
                y -= (rows - 6) * 18;
            }

            if (entityLivingBase != null)
            {
                x = xCenter - 55;
                xInv = xCenter + 2;
                yInv = Math.min(yInv, yCenter - 92);
            }

            if (crafterBlock != null && pos != null)
            {
                CrafterBlockEntity cbe = (CrafterBlockEntity) world.getChunkAt(pos).getBlockEntity(pos);
                if (cbe != null)
                {
                    lockedSlots = BlockUtils.getDisabledSlots(cbe);
                }
            }

            fi.dy.masa.malilib.render.RenderUtils.setShulkerboxBackgroundTintColor(shulkerBoxBlock, Configs.Generic.SHULKER_DISPLAY_BACKGROUND_COLOR.getBooleanValue());

            if (isHorse)
            {
                Container horseInv = new SimpleContainer(2);
                ItemStack horseArmor = (((AbstractHorse) entityLivingBase).getBodyArmorItem());
                horseInv.setItem(0, horseArmor != null && !horseArmor.isEmpty() ? horseArmor : ItemStack.EMPTY);
                horseInv.setItem(1, inv.getItem(0));

                InventoryOverlay.renderInventoryBackground(drawContext, type, xInv, yInv, 1, 2, mc);

                if (type == InventoryOverlay.InventoryRenderType.LLAMA)
                {
                    InventoryOverlay.renderLlamaArmorBackgroundSlots(drawContext, horseInv, xInv + props.slotOffsetX, yInv + props.slotOffsetY);
                }
                else
                {
                    InventoryOverlay.renderHorseArmorBackgroundSlots(drawContext, horseInv, xInv + props.slotOffsetX, yInv + props.slotOffsetY);
                }
              
                InventoryOverlay.renderInventoryStacks(drawContext, type, horseInv, xInv + props.slotOffsetX, yInv + props.slotOffsetY, 1, 0, 2, mc);
                xInv += 32 + 4;
            }
            if (totalSlots > 0)
            {
                InventoryOverlay.renderInventoryBackground(drawContext, type, xInv, yInv, props.slotsPerRow, totalSlots, mc);

                if (type == InventoryOverlay.InventoryRenderType.BREWING_STAND)
                {
                    InventoryOverlay.renderBrewerBackgroundSlots(drawContext, inv, xInv, yInv);
                }
              
                InventoryOverlay.renderInventoryStacks(drawContext, type, inv, xInv + props.slotOffsetX, yInv + props.slotOffsetY, props.slotsPerRow, firstSlot, totalSlots, lockedSlots, mc);
            }
        }

        if (isWolf)
        {
            InventoryOverlay.InventoryRenderType type = InventoryOverlay.InventoryRenderType.HORSE;
            final InventoryOverlay.InventoryProperties props = InventoryOverlay.getInventoryPropsTemp(type, 2);
            final int rows = (int) Math.ceil((double) 2 / props.slotsPerRow);
            int xInv;
            int yInv = yCenter - props.height - 6;

            if (rows > 6)
            {
                yInv -= (rows - 6) * 18;
                y -= (rows - 6) * 18;
            }

            x = xCenter - 55;
            xInv = xCenter + 2;
            yInv = Math.min(yInv, yCenter - 92);

            Container wolfInv = new SimpleContainer(2);
            ItemStack wolfArmor = ((Wolf) entityLivingBase).getBodyArmorItem();
            wolfInv.setItem(0, wolfArmor != null && !wolfArmor.isEmpty() ? wolfArmor : ItemStack.EMPTY);
            InventoryOverlay.renderInventoryBackground(drawContext, type, xInv, yInv, 1, 2, mc);
            InventoryOverlay.renderWolfArmorBackgroundSlots(drawContext, wolfInv, xInv + props.slotOffsetX, yInv + props.slotOffsetY);
            InventoryOverlay.renderInventoryStacks(drawContext, type, wolfInv, xInv + props.slotOffsetX, yInv + props.slotOffsetY, 1, 0, 2, mc);
        }

        if (entityLivingBase != null)
        {
            InventoryOverlay.renderEquipmentOverlayBackground(drawContext, x, y, entityLivingBase);
            InventoryOverlay.renderEquipmentStacks(drawContext, entityLivingBase, x, y, mc);
        }
    }
}
