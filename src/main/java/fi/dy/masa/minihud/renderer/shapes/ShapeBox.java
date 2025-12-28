package fi.dy.masa.minihud.renderer.shapes;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.position.PositionUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.renderer.RenderObjectVbo;

public class ShapeBox extends ShapeBase
{
    public static final AABB DEFAULT_BOX = new AABB(0, 0, 0, 1, 1, 1);
    protected static final double MAX_DIMENSIONS = 10000.0;

    protected AABB box;
    protected AABB renderPerimeter;
    protected Vec3 corner1;
    protected Vec3 corner2;
    protected int enabledSidesMask;
    protected double maxDimensions;
    protected boolean gridEnabled;
    protected Vec3 gridSize;
    protected Vec3 gridStartOffset;
    protected Vec3 gridEndOffset;

    private AABB renderBox;
    private boolean hasData;

    public ShapeBox()
    {
        this(ShapeType.BOX, Configs.Colors.SHAPE_BOX.getColor());
    }
    
    public ShapeBox(ShapeType type)
    {
        this(type, Configs.Colors.SHAPE_BOX.getColor());
    }

    public ShapeBox(ShapeType type, Color4f color)
    {
        super(type, color);
        this.initializeBox();
    }

    protected void initializeBox()
	{
        this.box = DEFAULT_BOX;
        this.renderPerimeter = DEFAULT_BOX;
        this.corner1 = Vec3.ZERO;
        this.corner2 = Vec3.ZERO;
        this.enabledSidesMask = 0x3F;
        this.maxDimensions = MAX_DIMENSIONS;
        this.gridEnabled = true;
        this.gridSize = new Vec3(16.0, 16.0, 16.0);
        this.gridStartOffset = Vec3.ZERO;
        this.gridEndOffset = Vec3.ZERO;
        this.renderBox = null;
        this.hasData = false;
        this.useCulling = false;
    }

	@Override
	public void onShapeInit()
	{
		Entity cameraEntity = EntityUtils.getCameraEntity();

		if (cameraEntity != null &&
			this.getCorner1() == Vec3.ZERO)
		{
			Vec3 pos = cameraEntity.position();
			this.corner1 = pos;
			this.corner2 = pos.add(this.gridSize);
			this.setBoxFromCorners();
		}
	}

    public AABB getBox()
    {
        return this.box;
    }

    public int getEnabledSidesMask()
    {
        return this.enabledSidesMask;
    }

    public boolean isGridEnabled()
    {
        return this.gridEnabled;
    }

    public Vec3 getGridSize()
    {
        return this.gridSize;
    }

    public Vec3 getGridStartOffset()
    {
        return this.gridStartOffset;
    }

    public Vec3 getGridEndOffset()
    {
        return this.gridEndOffset;
    }

    public Vec3 getCorner1()
    {
        return this.corner1;
    }

    public Vec3 getCorner2()
    {
        return this.corner2;
    }

    public void setCorner1(Vec3 corner1)
    {
        this.corner1 = corner1;
        this.setBoxFromCorners();
    }

    public void setCorner2(Vec3 corner2)
    {
        this.corner2 = corner2;
        this.setBoxFromCorners();
    }

    protected void setBoxFromCorners()
    {
        AABB box = new AABB(this.corner1, this.corner2);
        this.box = this.clampBox(box, this.maxDimensions);

        double margin = Minecraft.getInstance().options.renderDistance().get() * 16 * 2;
        this.renderPerimeter = box.inflate(margin);
        this.setNeedsUpdate();
    }

    protected AABB clampBox(AABB box, double maxSize)
    {
        if (Math.abs(box.maxX - box.minX) > maxSize ||
            Math.abs(box.maxY - box.minY) > maxSize ||
            Math.abs(box.maxZ - box.minZ) > maxSize)
        {
            box = DEFAULT_BOX;
        }

        return box;
    }

    public void setEnabledSidesMask(int enabledSidesMask)
    {
        this.enabledSidesMask = enabledSidesMask;
        this.setNeedsUpdate();
    }

    public void toggleGridEnabled()
    {
        this.gridEnabled = ! this.gridEnabled;
        this.setNeedsUpdate();
    }

    public void setGridSize(Vec3 gridSize)
    {
        double x = Mth.clamp(gridSize.x, 0.5, 1024);
        double y = Mth.clamp(gridSize.y, 0.5, 1024);
        double z = Mth.clamp(gridSize.z, 0.5, 1024);
        this.gridSize = new Vec3(x, y, z);
        this.setNeedsUpdate();
    }

    public void setGridStartOffset(Vec3 gridStartOffset)
    {
        double x = Mth.clamp(gridStartOffset.x, 0.0, 1024);
        double y = Mth.clamp(gridStartOffset.y, 0.0, 1024);
        double z = Mth.clamp(gridStartOffset.z, 0.0, 1024);
        this.gridStartOffset = new Vec3(x, y, z);
        this.setNeedsUpdate();
    }

    public void setGridEndOffset(Vec3 gridEndOffset)
    {
        double x = Mth.clamp(gridEndOffset.x, 0.0, 1024);
        double y = Mth.clamp(gridEndOffset.y, 0.0, 1024);
        double z = Mth.clamp(gridEndOffset.z, 0.0, 1024);
        this.gridEndOffset = new Vec3(x, y, z);
        this.setNeedsUpdate();
    }

    @Override
    public boolean shouldRender(Minecraft mc)
    {
        Entity entity = EntityUtils.getCameraEntity();
        return super.shouldRender(mc) && entity != null && this.renderPerimeter.contains(entity.position());
    }

    @Override
    public void update(Vec3 cameraPos, Entity entity, Minecraft mc, ProfilerFiller profiler)
    {
        this.renderBox = this.box.move(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        this.hasData = true;
        this.render(cameraPos, mc, profiler);
        this.needsUpdate = false;
    }

    @Override
    public boolean hasData()
    {
        return this.hasData && this.renderBox != null;
    }

    @Override
    public void render(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        this.allocateBuffers(this.renderLines);
        this.renderBoxQuads(cameraPos, mc, profiler);

        if (this.renderLines)
        {
            this.renderBoxOutlines(cameraPos, mc, profiler);
        }
    }

    @Override
    public void reset()
    {
        super.reset();
        this.renderBox = null;
        this.hasData = false;
    }

    protected void renderBoxQuads(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null || mc.player == null)
        {
            return;
        }

        profiler.push("box_quads");
        RenderObjectVbo ctx = this.renderObjects.getFirst();
        BufferBuilder builder = ctx.start(() -> "minihud:box/quads", this.renderThroughShape ? MaLiLibPipelines.MINIHUD_SHAPE_NO_DEPTH_OFFSET : MaLiLibPipelines.MINIHUD_SHAPE_OFFSET_NO_CULL);
        PoseStack matrices = new PoseStack();

        matrices.pushPose();

        for (Direction side : PositionUtils.ALL_DIRECTIONS)
        {
            if (isSideEnabled(side, this.enabledSidesMask))
            {
                renderBoxSideQuad(this.renderBox, side, this.color, builder);
            }
        }

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
            MiniHUD.LOGGER.error("ShapeBox#renderBoxQuads(): Exception; {}", err.getMessage());
        }

        matrices.popPose();
        profiler.pop();
    }

    protected void renderBoxOutlines(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null || mc.player == null || !this.renderLines)
        {
            return;
        }

        profiler.push("box_outlines");
//        Color4f color = Color4f.fromColor(this.color.intValue, 1f);
//        Color4f color = Configs.Colors.SHAPE_OUTLINES.getColor();

        RenderObjectVbo ctx = this.renderObjects.get(1);
        BufferBuilder builder = ctx.start(() -> "minihud:box/outlines", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH);
        PoseStack matrices = new PoseStack();

        matrices.pushPose();
        PoseStack.Pose e = matrices.last();

        this.renderBoxEnabledEdgeLines(this.renderBox, this.colorLines, this.enabledSidesMask, builder, e, this.glLineWidth);

        if (this.gridEnabled)
        {
            this.renderGridLines(this.renderBox, this.colorLines, builder, e, this.glLineWidth);
        }

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
            MiniHUD.LOGGER.error("OverlayRendererRegion#renderOutlines(): Exception; {}", err.getMessage());
        }

        matrices.popPose();
        profiler.pop();
    }

    protected void renderGridLines(AABB box, Color4f color, BufferBuilder builder, PoseStack.Pose e, float lineWidth)
    {
        if (isSideEnabled(Direction.DOWN, this.enabledSidesMask))
        {
            this.renderGridLinesY(box, box.minY, color, builder, e, lineWidth);
        }

        if (isSideEnabled(Direction.UP, this.enabledSidesMask))
        {
            this.renderGridLinesY(box, box.maxY, color, builder, e, lineWidth);
        }

        if (isSideEnabled(Direction.NORTH, this.enabledSidesMask))
        {
            this.renderGridLinesZ(box, box.minZ, color, builder, e, lineWidth);
        }

        if (isSideEnabled(Direction.SOUTH, this.enabledSidesMask))
        {
            this.renderGridLinesZ(box, box.maxZ, color, builder, e, lineWidth);
        }

        if (isSideEnabled(Direction.WEST, this.enabledSidesMask))
        {
            this.renderGridLinesX(box, box.minX, color, builder, e, lineWidth);
        }

        if (isSideEnabled(Direction.EAST, this.enabledSidesMask))
        {
            this.renderGridLinesX(box, box.maxX, color, builder, e, lineWidth);
        }
    }

    protected void renderGridLinesX(AABB box, double x, Color4f color, BufferBuilder buffer, PoseStack.Pose e, float lineWidth)
    {
        double end = box.maxY - this.gridEndOffset.y;
        double min = box.minZ + this.gridStartOffset.z;
        double max = box.maxZ - this.gridEndOffset.z;

        for (double y = box.minY + this.gridStartOffset.y; y <= end; y += this.gridSize.y)
        {
            //.setNormal(e, 0.0f, 0.0f, 0.0f)
            buffer.addVertex(e, (float) x, (float) y, (float) min).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
            buffer.addVertex(e, (float) x, (float) y, (float) max).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        }

        end = box.maxZ - this.gridEndOffset.z;
        min = box.minY + this.gridStartOffset.y;
        max = box.maxY - this.gridEndOffset.y;

        for (double z = box.minZ + this.gridStartOffset.z; z <= end; z += this.gridSize.z)
        {
            buffer.addVertex(e, (float) x, (float) min, (float) z).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
            buffer.addVertex(e, (float) x, (float) max, (float) z).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        }
    }

    protected void renderGridLinesY(AABB box, double y, Color4f color, BufferBuilder buffer, PoseStack.Pose e, float lineWidth)
    {
        double end = box.maxX - this.gridEndOffset.x;
        double min = box.minZ + this.gridStartOffset.z;
        double max = box.maxZ - this.gridEndOffset.z;

        for (double x = box.minX + this.gridStartOffset.x; x <= end; x += this.gridSize.x)
        {
            buffer.addVertex(e, (float) x, (float) y, (float) min).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
            buffer.addVertex(e, (float) x, (float) y, (float) max).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        }

        end = box.maxZ - this.gridEndOffset.z;
        min = box.minX + this.gridStartOffset.x;
        max = box.maxX - this.gridEndOffset.x;

        for (double z = box.minZ + this.gridStartOffset.z; z <= end; z += this.gridSize.z)
        {
            buffer.addVertex(e, (float) min, (float) y, (float) z).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
            buffer.addVertex(e, (float) max, (float) y, (float) z).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        }
    }

    protected void renderGridLinesZ(AABB box, double z, Color4f color, BufferBuilder buffer, PoseStack.Pose e, float lineWidth)
    {
        double end = box.maxX - this.gridEndOffset.x;
        double min = box.minY + this.gridStartOffset.y;
        double max = box.maxY - this.gridEndOffset.y;

        for (double x = box.minX + this.gridStartOffset.x; x <= end; x += this.gridSize.x)
        {
            buffer.addVertex(e, (float) x, (float) min, (float) z).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
            buffer.addVertex(e, (float) x, (float) max, (float) z).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        }

        end = box.maxY - this.gridEndOffset.y;
        min = box.minX + this.gridStartOffset.x;
        max = box.maxX - this.gridEndOffset.x;

        for (double y = box.minY + this.gridStartOffset.y; y <= end; y += this.gridSize.y)
        {
            buffer.addVertex(e, (float) min, (float) y, (float) z).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
            buffer.addVertex(e, (float) max, (float) y, (float) z).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        }
    }

    public boolean isSideEnabled(Direction side)
    {
        return isSideEnabled(side, this.enabledSidesMask);
    }

    public static boolean isSideEnabled(Direction side, int enabledSidesMask)
    {
        return (enabledSidesMask & (1 << side.get3DDataValue())) != 0;
    }

    public static void renderBoxSideQuad(AABB box, Direction side, Color4f color, BufferBuilder buffer)
    {
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        switch (side)
        {
            case DOWN:
                buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a);
                break;

            case UP:
                buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);
                break;

            case NORTH:
                buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
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
                buffer.addVertex(maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a);
                break;
        }
    }

    protected void renderBoxEnabledEdgeLines(AABB box, Color4f color, int enabledSidesMask, BufferBuilder buffer, PoseStack.Pose e, float lineWidth)
    {
        boolean down  = isSideEnabled(Direction.DOWN,   enabledSidesMask);
        boolean up    = isSideEnabled(Direction.UP,     enabledSidesMask);
        boolean north = isSideEnabled(Direction.NORTH,  enabledSidesMask);
        boolean south = isSideEnabled(Direction.SOUTH,  enabledSidesMask);
        boolean west  = isSideEnabled(Direction.WEST,   enabledSidesMask);
        boolean east  = isSideEnabled(Direction.EAST,   enabledSidesMask);

        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        // Lines along the x-axis
        if (down || north)
        {
            buffer.addVertex(e, minX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
            buffer.addVertex(e, maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        }

        if (up || north)
        {
            buffer.addVertex(e, minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
            buffer.addVertex(e, maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        }

        if (down || south)
        {
            buffer.addVertex(e, minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
            buffer.addVertex(e, maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        }

        if (up || south)
        {
            buffer.addVertex(e, minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
            buffer.addVertex(e, maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        }

        // Lines along the z-axis
        if (down || west)
        {
            buffer.addVertex(e, minX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
            buffer.addVertex(e, minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        }

        if (up || west)
        {
            buffer.addVertex(e, minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
            buffer.addVertex(e, minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        }

        if (down || east)
        {
            buffer.addVertex(e, maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
            buffer.addVertex(e, maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        }

        if (up || east)
        {
            buffer.addVertex(e, maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
            buffer.addVertex(e, maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        }

        // Lines along the y-axis
        if (north || west)
        {
            buffer.addVertex(e, minX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
            buffer.addVertex(e, minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        }

        if (south || west)
        {
            buffer.addVertex(e, minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
            buffer.addVertex(e, minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        }

        if (north || east)
        {
            buffer.addVertex(e, maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
            buffer.addVertex(e, maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        }

        if (south || east)
        {
            buffer.addVertex(e, maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
            buffer.addVertex(e, maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        }
    }

    @Override
    public List<String> getWidgetHoverLines()
    {
        List<String> lines = super.getWidgetHoverLines();
        AABB box = this.box;
        lines.add(StringUtils.translate("minihud.gui.label.shape.box.min_corner", box.minX, box.minY, box.minZ));
        lines.add(StringUtils.translate("minihud.gui.label.shape.box.max_corner", box.maxX, box.maxY, box.maxZ));
        return lines;
    }

    @Override
    public JsonObject toJson()
    {
        JsonObject obj = super.toJson();

		if (obj != null)
		{
			obj.addProperty("enabled_sides", this.enabledSidesMask);
			obj.addProperty("grid_enabled", this.gridEnabled);
			obj.add("grid_size", JsonUtils.vec3dToJson(this.gridSize));
			obj.add("grid_start_offset", JsonUtils.vec3dToJson(this.gridStartOffset));
			obj.add("grid_end_offset", JsonUtils.vec3dToJson(this.gridEndOffset));

			obj.add("corner1", JsonUtils.vec3dToJson(this.corner1));
			obj.add("corner2", JsonUtils.vec3dToJson(this.corner2));

			return obj;
		}

		return new JsonObject();
    }

    @Override
    public void fromJson(JsonObject obj)
    {
        super.fromJson(obj);

        this.enabledSidesMask = JsonUtils.getIntegerOrDefault(obj, "enabled_sides", 0x3F);
        this.gridEnabled     = JsonUtils.getBooleanOrDefault(obj, "grid_enabled", true);
        this.gridSize        = JsonUtils.vec3dFromJson(obj, "grid_size");
        this.gridStartOffset = JsonUtils.vec3dFromJson(obj, "grid_start_offset");
        this.gridEndOffset   = JsonUtils.vec3dFromJson(obj, "grid_end_offset");

        if (this.gridSize == null)        { this.gridSize = new Vec3(16.0, 16.0, 16.0); }
        if (this.gridStartOffset == null) { this.gridStartOffset = Vec3.ZERO; }
        if (this.gridEndOffset == null)   { this.gridEndOffset = Vec3.ZERO; }

        Vec3 corner1 = JsonUtils.vec3dFromJson(obj, "corner1");
        Vec3 corner2 = JsonUtils.vec3dFromJson(obj, "corner2");

        if (corner1 != null && corner2 != null)
        {
            this.corner1 = corner1;
            this.corner2 = corner2;
        }
        else
        {
            double minX = JsonUtils.getDoubleOrDefault(obj, "minX", 0);
            double minY = JsonUtils.getDoubleOrDefault(obj, "minY", 0);
            double minZ = JsonUtils.getDoubleOrDefault(obj, "minZ", 0);
            double maxX = JsonUtils.getDoubleOrDefault(obj, "maxX", 0);
            double maxY = JsonUtils.getDoubleOrDefault(obj, "maxY", 0);
            double maxZ = JsonUtils.getDoubleOrDefault(obj, "maxZ", 0);

            this.corner1 = new Vec3(minX, minY, minZ);
            this.corner2 = new Vec3(maxX, maxY, maxZ);
        }

        this.setBoxFromCorners();
    }
}
