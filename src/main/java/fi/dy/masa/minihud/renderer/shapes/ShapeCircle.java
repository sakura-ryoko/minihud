package fi.dy.masa.minihud.renderer.shapes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.position.PositionUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.renderer.RenderObjectVbo;
import fi.dy.masa.minihud.renderer.RenderUtils;
import fi.dy.masa.minihud.util.ShapeRenderType;
import fi.dy.masa.minihud.util.shape.SphereUtils;

public class ShapeCircle extends ShapeCircleBase
{
    protected int height = 1;
    private boolean hasData;

    public ShapeCircle()
    {
        super(ShapeType.CIRCLE, Configs.Colors.SHAPE_CIRCLE.getColor(), 16);
        this.hasData = false;
        this.useCulling = false;
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

        profiler.push("circle_quads");
        RenderObjectVbo ctx = this.renderObjects.getFirst();
        BufferBuilder builder = ctx.start(() -> "minihud:circle/quads", this.renderThroughShape ? MaLiLibPipelines.MINIHUD_SHAPE_NO_DEPTH_OFFSET : MaLiLibPipelines.MINIHUD_SHAPE_OFFSET_NO_CULL);

        this.renderCircleShapeQuads(cameraPos, builder);

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
            MiniHUD.LOGGER.error("ShapeCircle#renderQuads(): Exception; {}", err.getMessage());
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

        profiler.push("circle_outlines");
        RenderObjectVbo ctx = this.renderObjects.get(1);
        BufferBuilder builder = ctx.start(() -> "minihud:circle/outlines", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH);

        this.renderCircleShapeOutlines(cameraPos, this.glLineWidth, builder);

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
            MiniHUD.LOGGER.error("ShapeCircle#renderOutlines(): Exception; {}", err.getMessage());
        }

        profiler.pop();
    }

    @Override
    public void reset()
    {
        super.reset();
        this.hasData = false;
    }

    public int getHeight()
    {
        return this.height;
    }

    public void setHeight(int height)
    {
        this.height = Mth.clamp(height, 1, 8192);
        this.setNeedsUpdate();
    }

    protected void renderCircleShapeQuads(Vec3 cameraPos, BufferBuilder builder)
    {
        LongOpenHashSet positions = new LongOpenHashSet();
        Consumer<BlockPos.MutableBlockPos> positionConsumer = this.getPositionCollector(positions);
        SphereUtils.RingPositionTest test = this::isPositionOnOrInsideRing;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        Vec3 effectiveCenter = this.getEffectiveCenter();
        Direction.Axis axis = this.mainAxis.getAxis();
        double expand = 0;

        if (this.getCombineQuads())
        {
            mutablePos.set(effectiveCenter.x, effectiveCenter.y, effectiveCenter.z);

            if (axis == Direction.Axis.Y)
            {
                SphereUtils.addPositionsOnHorizontalBlockRing(positionConsumer, mutablePos, test);
            }
            else
            {
                SphereUtils.addPositionsOnVerticalBlockRing(positionConsumer, mutablePos, this.mainAxis, test);
            }

            Long2ObjectOpenHashMap<SideQuad> strips =
                    SphereUtils.buildSphereShellToStrips(positions, axis, test, this.renderType, this.layerRange);
            List<SideQuad> quads = buildStripsToQuadsForCircle(strips, this.mainAxis, this.height);

            RenderUtils.renderQuads(quads, this.color, expand, cameraPos, builder);
        }
        else
        {
            BlockPos posCenter = BlockPos.containing(effectiveCenter);
            int offX = this.mainAxis.getStepX();
            int offY = this.mainAxis.getStepY();
            int offZ = this.mainAxis.getStepZ();
    
            for (int i = 0; i < this.height; ++i)
            {
                mutablePos.set(posCenter.getX() + offX * i,
                               posCenter.getY() + offY * i,
                               posCenter.getZ() + offZ * i);
    
                if (axis == Direction.Axis.Y)
                {
                    SphereUtils.addPositionsOnHorizontalBlockRing(positionConsumer, mutablePos, test);
                }
                else
                {
                    SphereUtils.addPositionsOnVerticalBlockRing(positionConsumer, mutablePos, this.mainAxis, test);
                }
            }

            Direction[] sides = this.getSides();
            RenderUtils.renderCircleBlockPositions(positions, sides, test, this.renderType, this.layerRange,
                                                   this.color, expand, cameraPos, builder);
        }
    }

    protected void renderCircleShapeOutlines(Vec3 cameraPos,
                                             float lineWidth,
                                             BufferBuilder builder)
    {
        LongOpenHashSet positions = new LongOpenHashSet();
        Consumer<BlockPos.MutableBlockPos> positionConsumer = this.getPositionCollector(positions);
        SphereUtils.RingPositionTest test = this::isPositionOnOrInsideRing;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        Vec3 effectiveCenter = this.getEffectiveCenter();
        Direction.Axis axis = this.mainAxis.getAxis();
        double expand = 0;

        if (this.getCombineQuads())
        {
            mutablePos.set(effectiveCenter.x, effectiveCenter.y, effectiveCenter.z);

            if (axis == Direction.Axis.Y)
            {
                SphereUtils.addPositionsOnHorizontalBlockRing(positionConsumer, mutablePos, test);
            }
            else
            {
                SphereUtils.addPositionsOnVerticalBlockRing(positionConsumer, mutablePos, this.mainAxis, test);
            }

            Long2ObjectOpenHashMap<SideQuad> strips =
                    SphereUtils.buildSphereShellToStrips(positions, axis, test, this.renderType, this.layerRange);
            List<SideQuad> quads = buildStripsToQuadsForCircle(strips, this.mainAxis, this.height);

            RenderUtils.renderQuadLines(quads, this.colorLines, expand, cameraPos, lineWidth, builder);
        }
        else
        {
            BlockPos posCenter = BlockPos.containing(effectiveCenter);
            int offX = this.mainAxis.getStepX();
            int offY = this.mainAxis.getStepY();
            int offZ = this.mainAxis.getStepZ();

            for (int i = 0; i < this.height; ++i)
            {
                mutablePos.set(posCenter.getX() + offX * i,
                               posCenter.getY() + offY * i,
                               posCenter.getZ() + offZ * i);

                if (axis == Direction.Axis.Y)
                {
                    SphereUtils.addPositionsOnHorizontalBlockRing(positionConsumer, mutablePos, test);
                }
                else
                {
                    SphereUtils.addPositionsOnVerticalBlockRing(positionConsumer, mutablePos, this.mainAxis, test);
                }
            }

            Direction[] sides = this.getSides();
            RenderUtils.renderCircleBlockOutlines(positions, sides, test, this.renderType, this.layerRange,
                                                  this.colorLines, expand, cameraPos, lineWidth, builder);
        }
    }

    protected Direction[] getSides()
    {
        // Exclude the two sides on the main axis
        if (this.renderType != ShapeRenderType.FULL_BLOCK)
        {
            return SphereUtils.getDirectionsNotOnAxis(this.mainAxis.getAxis());
        }

        return PositionUtils.ALL_DIRECTIONS;
    }

    protected boolean isPositionOnOrInsideRing(int blockX, int blockY, int blockZ, Direction outSide)
    {
        Direction.Axis axis = this.mainAxis.getAxis();

        Vec3 effectiveCenter = this.getEffectiveCenter();
        double radiusSq = this.getSquaredRadius();
        double x = axis == Direction.Axis.X ? effectiveCenter.x : (double) blockX + 0.5;
        double y = axis == Direction.Axis.Y ? effectiveCenter.y : (double) blockY + 0.5;
        double z = axis == Direction.Axis.Z ? effectiveCenter.z : (double) blockZ + 0.5;
        double distSq = effectiveCenter.distanceToSqr(x, y, z);
        double diff = radiusSq - distSq;

        return diff >= 0;
    }

    public static List<SideQuad> buildStripsToQuadsForCircle(Long2ObjectOpenHashMap<SideQuad> strips,
                                                                         Direction mainAxisDirection, int circleHeight)
    {
        List<SideQuad> quads = new ArrayList<>();
        Long2ByteOpenHashMap handledPositions = new Long2ByteOpenHashMap();
        final Direction.Axis mainAxis = mainAxisDirection.getAxis();

        for (SideQuad strip : strips.values())
        {
            final long pos = strip.startPos();
            final Direction side = strip.side();

            if (SphereUtils.isHandledAndMarkHandled(pos, side, handledPositions))
            {
                continue;
            }

            final long startPos = side == mainAxisDirection ? SphereUtils.offsetPos(pos, mainAxisDirection, circleHeight - 1) : pos;
            final int height = side.getAxis() != mainAxis ? circleHeight : strip.height();

            quads.add(new SideQuad(startPos, strip.width(), height, side));
        }

        return quads;
    }

    @Override
    public List<String> getWidgetHoverLines()
    {
        List<String> lines = super.getWidgetHoverLines();

        lines.add(2, StringUtils.translate("minihud.gui.hover.shape.circle.main_axis_value",
                org.apache.commons.lang3.StringUtils.capitalize(this.getMainAxis().toString().toLowerCase())));
        lines.add(3, StringUtils.translate("minihud.gui.hover.shape.height_value", this.getHeight()));

        return lines;
    }

    @Override
    public JsonObject toJson()
    {
        JsonObject obj = super.toJson();

		if (obj != null)
		{
			obj.add("height", new JsonPrimitive(this.height));
			return obj;
		}

		return new JsonObject();
    }

    @Override
    public void fromJson(JsonObject obj)
    {
        super.fromJson(obj);

        this.setHeight(JsonUtils.getInteger(obj, "height"));
    }
}
