package fi.dy.masa.minihud.renderer.shapes;

import java.util.List;
import java.util.function.LongConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.*;
import fi.dy.masa.malilib.util.position.PositionUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.renderer.RenderObjectVbo;
import fi.dy.masa.minihud.renderer.RenderUtils;
import fi.dy.masa.minihud.util.RayTracer;
import fi.dy.masa.minihud.util.shape.SphereUtils;

public class ShapeLineBlock extends ShapeBlocky
{
    protected Vec3 startPos = Vec3.ZERO;
    protected Vec3 endPos = Vec3.ZERO;
    protected Vec3 effectiveStartPos = Vec3.ZERO;
    protected Vec3 effectiveEndPos = Vec3.ZERO;
	protected Vec3 initialSize = new Vec3(16.0D, 16.0D, 16.0D);

    private boolean hasData;

    public ShapeLineBlock()
    {
        super(ShapeType.BLOCK_LINE, Configs.Colors.SHAPE_LINE_BLOCKY.getColor());

        this.setBlockSnap(BlockSnap.CENTER);
        this.hasData = false;
        this.useCulling = false;
    }

	@Override
	public void onShapeInit()
	{
		super.onShapeInit();

		Entity cameraEntity = EntityUtils.getCameraEntity();

		if (cameraEntity != null &&
			this.startPos == Vec3.ZERO)
		{
			Vec3 pos = cameraEntity.position();

			this.startPos = pos;
			this.endPos = pos.add(this.initialSize);
			this.updateEffectivePositions();
		}
	}

    public Vec3 getStartPos()
    {
        return this.effectiveStartPos;
    }

    public Vec3 getEndPos()
    {
        return this.effectiveEndPos;
    }

    public void setStartPos(Vec3 startPos)
    {
        this.startPos = startPos;
        this.updateEffectivePositions();
    }

    public void setEndPos(Vec3 endPos)
    {
        this.endPos = endPos;
        this.updateEffectivePositions();
    }

    @Override
    public void moveToPosition(Vec3 pos)
    {
        Vec3 diff = this.endPos.subtract(this.startPos);
        this.startPos = pos;
        this.endPos = pos.add(diff);
        this.updateEffectivePositions();
        InfoUtils.printActionbarMessage(String.format("Moved shape to %.1f %.1f %.1f",
                                                      pos.x(), pos.y(), pos.z()));
    }

    @Override
    public void setBlockSnap(BlockSnap snap)
    {
        super.setBlockSnap(snap);
        this.updateEffectivePositions();
    }

    @Override
    public void update(Vec3 cameraPos, Entity entity, Minecraft mc, ProfilerFiller profiler)
    {
        this.hasData = true;
        this.render(cameraPos, mc, profiler);
        this.needsUpdate = false;
    }

    @Override
    public boolean hasData()
    {
        return this.hasData;
    }

    @Override
    public void render(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        this.allocateBuffers(this.renderLines);
        this.renderQuads(cameraPos, mc, profiler);

        if (this.renderLines)
        {
            this.renderOutlines(cameraPos, mc, profiler);
        }
    }

    private void renderQuads(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null || mc.player == null)
        {
            return;
        }

        profiler.push("line_block_quads");
        RenderObjectVbo ctx = this.renderObjects.getFirst();
        BufferBuilder builder = ctx.start(() -> "minihud:line_block/quads", this.renderThroughShape ? MaLiLibPipelines.MINIHUD_SHAPE_NO_DEPTH_OFFSET : MaLiLibPipelines.MINIHUD_SHAPE_OFFSET_NO_CULL);
//        MatrixStack matrices = new MatrixStack();

//        matrices.push();
        this.renderLineShapeQuads(cameraPos, builder);

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
            MiniHUD.LOGGER.error("ShapeLineBlock#renderQuads(): Exception; {}", err.getMessage());
        }

//        matrices.pop();
        profiler.pop();
    }

    private void renderOutlines(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null || mc.player == null || !this.renderLines)
        {
            return;
        }

        profiler.push("line_block_outlines");
        RenderObjectVbo ctx = this.renderObjects.get(1);
        BufferBuilder builder = ctx.start(() -> "minihud:line_block/outlines", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH);

        this.renderLineShapeLines(cameraPos, this.glLineWidth, builder);

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
            MiniHUD.LOGGER.error("ShapeLineBlock#renderOutlines(): Exception; {}", err.getMessage());
        }

        profiler.pop();
    }

    @Override
    public void reset()
    {
        super.reset();
        this.hasData = false;
    }

    @Override
    public List<String> getWidgetHoverLines()
    {
        List<String> lines = super.getWidgetHoverLines();
        Vec3 s = this.startPos;
        Vec3 e = this.endPos;

        lines.add(StringUtils.translate("minihud.gui.label.shape.line.start", d2(s.x), d2(s.y), d2(s.z)));
        lines.add(StringUtils.translate("minihud.gui.label.shape.line.end",   d2(e.x), d2(e.y), d2(e.z)));

        return lines;
    }

    @Override
    public JsonObject toJson()
    {
        JsonObject obj = super.toJson();

		if (obj != null)
		{
			obj.add("start", JsonUtils.vec3dToJson(this.startPos));
			obj.add("end", JsonUtils.vec3dToJson(this.endPos));
			return obj;
		}

		return new JsonObject();
    }

    @Override
    public void fromJson(JsonObject obj)
    {
        super.fromJson(obj);

        Vec3 startPos = JsonUtils.vec3dFromJson(obj, "start");
        Vec3 endPos = JsonUtils.vec3dFromJson(obj, "end");

        if (startPos != null)
        {
            this.startPos = startPos;
        }

        if (endPos != null)
        {
            this.endPos = endPos;
        }

        this.updateEffectivePositions();
    }

    protected void updateRenderPerimeter()
    {
        double range = 512;
        double minX = Math.min(this.effectiveStartPos.x(), this.effectiveEndPos.x()) - range;
        double minY = Math.min(this.effectiveStartPos.y(), this.effectiveEndPos.y()) - range;
        double minZ = Math.min(this.effectiveStartPos.z(), this.effectiveEndPos.z()) - range;
        double maxX = Math.max(this.effectiveStartPos.x(), this.effectiveEndPos.x()) + range;
        double maxY = Math.max(this.effectiveStartPos.y(), this.effectiveEndPos.y()) + range;
        double maxZ = Math.max(this.effectiveStartPos.z(), this.effectiveEndPos.z()) + range;

        this.renderPerimeter = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    protected void updateEffectivePositions()
    {
        this.effectiveStartPos = this.getBlockSnappedPosition(this.startPos);
        this.effectiveEndPos = this.getBlockSnappedPosition(this.endPos);
        this.updateRenderPerimeter();
        this.setNeedsUpdate();
    }

    protected void renderLineShapeQuads(Vec3 cameraPos, BufferBuilder builder)
    {
        final double maxDist = 30000;

        if (this.effectiveEndPos.distanceTo(this.effectiveStartPos) > maxDist)
        {
            return;
        }

        LongOpenHashSet positions = new LongOpenHashSet();
        RayTracer tracer = new RayTracer(this.effectiveStartPos, this.effectiveEndPos);
        double expand = 0;

        tracer.iterateAllPositions(this.getLinePositionCollector(positions));

        if (this.getCombineQuads())
        {
            Long2ObjectOpenHashMap<SideQuad> strips = this.buildPositionsToStrips(positions, this.layerRange);
            RenderUtils.renderQuads(strips.values(), this.color, expand, cameraPos, builder);
        }
        else
        {
            RenderUtils.renderBlockPositions(positions, this.layerRange, this.color, expand, cameraPos, builder);
        }
    }

    protected void renderLineShapeLines(Vec3 cameraPos,
										float lineWidth,
                                        BufferBuilder builder)
    {
        final double maxDist = 30000;

        if (this.effectiveEndPos.distanceTo(this.effectiveStartPos) > maxDist)
        {
            return;
        }

        LongOpenHashSet positions = new LongOpenHashSet();
        RayTracer tracer = new RayTracer(this.effectiveStartPos, this.effectiveEndPos);
        double expand = 0;

        tracer.iterateAllPositions(this.getLinePositionCollector(positions));

        if (this.getCombineQuads())
        {
            Long2ObjectOpenHashMap<SideQuad> strips = this.buildPositionsToStrips(positions, this.layerRange);
            RenderUtils.renderQuadLines(strips.values(), this.colorLines, expand, cameraPos, lineWidth, builder);
        }
        else
        {
            RenderUtils.renderBlockPositionOutlines(positions, this.layerRange, this.colorLines, expand, cameraPos, lineWidth, builder);
        }
    }

    protected LongConsumer getLinePositionCollector(LongOpenHashSet positionsOut)
    {
        IntBoundingBox box = this.layerRange.getExpandedBox(this.mc.level, 0);

        LongConsumer positionCollector = (pos) -> {
            if (box.containsPos(pos))
            {
                positionsOut.add(pos);
            }
        };

        return positionCollector;
    }

    public Long2ObjectOpenHashMap<SideQuad> buildPositionsToStrips(LongOpenHashSet positions, LayerRange layerRange)
    {
        Long2ObjectOpenHashMap<SideQuad> strips = new Long2ObjectOpenHashMap<>();
        Long2ByteOpenHashMap handledPositions = new Long2ByteOpenHashMap();
        Direction[] sides = PositionUtils.ALL_DIRECTIONS;
        double lengthX = Math.abs(this.effectiveEndPos.x() - this.effectiveStartPos.x());
        double lengthY = Math.abs(this.effectiveEndPos.y() - this.effectiveStartPos.y());
        double lengthZ = Math.abs(this.effectiveEndPos.z() - this.effectiveStartPos.z());
        Direction mainAxisHor = lengthX >= lengthZ ? Direction.WEST : Direction.NORTH;
        Direction mainAxisAll = lengthY >= lengthX && lengthY >= lengthZ ? Direction.DOWN : mainAxisHor;

        for (long pos : positions)
        {
            if (layerRange.isPositionWithinRange(pos) == false)
            {
                continue;
            }

            for (Direction side : sides)
            {
                if (SphereUtils.isHandledAndMarkHandled(pos, side, handledPositions) ||
                    positions.contains(BlockPos.offset(pos, side)))
                {
                    continue;
                }

                final Direction minDir = side.getAxis().isVertical() ? mainAxisHor : mainAxisAll;
                final Direction maxDir = minDir.getOpposite();
                final int lengthMin = getStripLengthOnSide(pos, side, minDir, positions, handledPositions);
                final int lengthMax = getStripLengthOnSide(pos, side, maxDir, positions, handledPositions);
                final long startPosLong = SphereUtils.offsetPos(pos, minDir, lengthMin);
                final long index = SphereUtils.getCompressedPosSide(startPosLong, side);
                int width = lengthMin + lengthMax + 1;
                int height = 1;

                // The render method considers the width of top and bottom quads as going along the x-axis,
                // and thus the height goes along the z-axis.
                // So if the strip on the top or bottom face was built along the z-axis, then we need to swap the values.
                // And since we don't do quad merging for this line shape, we also need to swap the values
                // if the strips on the horizontal sides are going vertically.
                if ((side.getAxis().isVertical() && mainAxisHor.getAxis() == Direction.Axis.Z) ||
                    (side.getAxis().isHorizontal() && mainAxisAll.getAxis().isVertical()))
                {
                    height = width;
                    width = 1;
                }

                strips.put(index, new SideQuad(startPosLong, width, height, side));
            }
        }

        return strips;
    }

    protected static int getStripLengthOnSide(long pos,
                                              Direction side,
                                              Direction moveDirection,
                                              LongOpenHashSet positions,
                                              Long2ByteOpenHashMap handledPositions)
    {
        int length = 0;
        long adjPos = BlockPos.offset(pos, moveDirection);

        while (positions.contains(adjPos))
        {
            if (positions.contains(BlockPos.offset(adjPos, side)) ||
                SphereUtils.isHandledAndMarkHandled(adjPos, side, handledPositions))
            {
                break;
            }

            ++length;
            adjPos = BlockPos.offset(adjPos, moveDirection);
        }

        return length;
    }
}
