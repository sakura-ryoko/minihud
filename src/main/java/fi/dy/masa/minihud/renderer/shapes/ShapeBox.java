package fi.dy.masa.minihud.renderer.shapes;

import java.util.List;
import com.google.gson.JsonObject;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlBufferTarget;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.ShaderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;

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
    public static final Box DEFAULT_BOX = new Box(0, 0, 0, 1, 1, 1);
    protected static final double MAX_DIMENSIONS = 10000.0;

    protected Box box;
    protected Box renderPerimeter;
    protected Vec3d corner1;
    protected Vec3d corner2;
    protected int enabledSidesMask;
    protected double maxDimensions;
    protected boolean gridEnabled;
    protected Vec3d gridSize;
    protected Vec3d gridStartOffset;
    protected Vec3d gridEndOffset;

    private Box renderBox;
    private boolean hasData;

    public ShapeBox()
    {
        super(ShapeType.BOX, Configs.Colors.SHAPE_BOX.getColor());
        this.box = DEFAULT_BOX;
        this.renderPerimeter = DEFAULT_BOX;
        this.corner1 = Vec3d.ZERO;
        this.corner2 = Vec3d.ZERO;
        this.enabledSidesMask = 0x3F;
        this.maxDimensions = MAX_DIMENSIONS;
        this.gridEnabled = true;
        this.gridSize = new Vec3d(16.0, 16.0, 16.0);
        this.gridStartOffset = Vec3d.ZERO;
        this.gridEndOffset = Vec3d.ZERO;
        this.renderBox = null;
        this.hasData = false;
        this.useCulling = true;
    }

    public Box getBox()
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

    public Vec3d getGridSize()
    {
        return this.gridSize;
    }

    public Vec3d getGridStartOffset()
    {
        return this.gridStartOffset;
    }

    public Vec3d getGridEndOffset()
    {
        return this.gridEndOffset;
    }

    public Vec3d getCorner1()
    {
        return this.corner1;
    }

    public Vec3d getCorner2()
    {
        return this.corner2;
    }

    public void setCorner1(Vec3d corner1)
    {
        this.corner1 = corner1;
        this.setBoxFromCorners();
    }

    public void setCorner2(Vec3d corner2)
    {
        this.corner2 = corner2;
        this.setBoxFromCorners();
    }

    protected void setBoxFromCorners()
    {
        Box box = new Box(this.corner1, this.corner2);
        this.box = this.clampBox(box, this.maxDimensions);

        double margin = MinecraftClient.getInstance().options.getViewDistance().getValue() * 16 * 2;
        this.renderPerimeter = box.expand(margin);
        this.setNeedsUpdate();
    }

    protected Box clampBox(Box box, double maxSize)
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

    public void setGridSize(Vec3d gridSize)
    {
        double x = MathHelper.clamp(gridSize.x, 0.5, 1024);
        double y = MathHelper.clamp(gridSize.y, 0.5, 1024);
        double z = MathHelper.clamp(gridSize.z, 0.5, 1024);
        this.gridSize = new Vec3d(x, y, z);
        this.setNeedsUpdate();
    }

    public void setGridStartOffset(Vec3d gridStartOffset)
    {
        double x = MathHelper.clamp(gridStartOffset.x, 0.0, 1024);
        double y = MathHelper.clamp(gridStartOffset.y, 0.0, 1024);
        double z = MathHelper.clamp(gridStartOffset.z, 0.0, 1024);
        this.gridStartOffset = new Vec3d(x, y, z);
        this.setNeedsUpdate();
    }

    public void setGridEndOffset(Vec3d gridEndOffset)
    {
        double x = MathHelper.clamp(gridEndOffset.x, 0.0, 1024);
        double y = MathHelper.clamp(gridEndOffset.y, 0.0, 1024);
        double z = MathHelper.clamp(gridEndOffset.z, 0.0, 1024);
        this.gridEndOffset = new Vec3d(x, y, z);
        this.setNeedsUpdate();
    }

    @Override
    public boolean shouldRender(MinecraftClient mc)
    {
        Entity entity = EntityUtils.getCameraEntity();
        return super.shouldRender(mc) && entity != null && this.renderPerimeter.contains(entity.getPos());
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity, MinecraftClient mc, Profiler profiler)
    {
        this.renderBox = this.box.offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        this.hasData = true;
        this.renderThrough = Configs.Generic.SHAPE_RENDER_THROUGH.getBooleanValue();
//        this.colorLines = Color4f.fromColor(this.color.intValue, 1f);
        this.render(cameraPos, mc, profiler);
        this.needsUpdate = false;
    }

    @Override
    public boolean hasData()
    {
        return this.hasData && this.renderBox != null;
    }

    @Override
    public void render(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        this.allocateBuffers();
        this.renderBoxQuads(cameraPos, mc, profiler);
        this.renderBoxOutlines(cameraPos, mc, profiler);
    }

    @Override
    public void reset()
    {
        super.reset();
        this.renderBox = null;
        this.hasData = false;
    }

    protected void renderBoxQuads(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        profiler.push("box_quads");
        RenderObjectVbo ctx = this.renderObjects.getFirst();
        BufferBuilder builder = ctx.start(() -> "Box Quads", this.renderThrough ? MaLiLibPipelines.POSITION_COLOR_SIMPLE : MaLiLibPipelines.POSITION_COLOR_LESSER_DEPTH, GlUsage.STATIC_WRITE);
        MatrixStack matrices = new MatrixStack();

        matrices.push();

        for (Direction side : PositionUtils.ALL_DIRECTIONS)
        {
            if (isSideEnabled(side, this.enabledSidesMask))
            {
                renderBoxSideQuad(this.renderBox, side, this.color, builder);
            }
        }

        try
        {
            ctx.upload(builder.endNullable(), GlBufferTarget.VERTICES);
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("ShapeBox#renderBoxQuads(): Exception; {}", err.getMessage());
        }

        matrices.pop();
        profiler.pop();
    }

    protected void renderBoxOutlines(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        profiler.push("box_outlines");
        Color4f color = Color4f.fromColor(this.color.intValue, 1f);

        RenderObjectVbo ctx = this.renderObjects.get(1);
        BufferBuilder builder = ctx.start(() -> "Box Lines", ShaderPipelines.LINES, GlUsage.STATIC_WRITE);
        MatrixStack matrices = new MatrixStack();

        matrices.push();
        MatrixStack.Entry e = matrices.peek();

        this.renderBoxEnabledEdgeLines(this.renderBox, color, this.enabledSidesMask, builder, e);

        if (this.gridEnabled)
        {
            this.renderGridLines(this.renderBox, color, builder, e);
        }

        try
        {
            ctx.upload(builder.endNullable(), GlBufferTarget.VERTICES);
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererRegion#renderOutlines(): Exception; {}", err.getMessage());
        }

        matrices.pop();
        profiler.pop();
    }

    protected void renderGridLines(Box box, Color4f color, BufferBuilder builder, MatrixStack.Entry e)
    {
        if (isSideEnabled(Direction.DOWN, this.enabledSidesMask))
        {
            this.renderGridLinesY(box, box.minY, color, builder, e);
        }

        if (isSideEnabled(Direction.UP, this.enabledSidesMask))
        {
            this.renderGridLinesY(box, box.maxY, color, builder, e);
        }

        if (isSideEnabled(Direction.NORTH, this.enabledSidesMask))
        {
            this.renderGridLinesZ(box, box.minZ, color, builder, e);
        }

        if (isSideEnabled(Direction.SOUTH, this.enabledSidesMask))
        {
            this.renderGridLinesZ(box, box.maxZ, color, builder, e);
        }

        if (isSideEnabled(Direction.WEST, this.enabledSidesMask))
        {
            this.renderGridLinesX(box, box.minX, color, builder, e);
        }

        if (isSideEnabled(Direction.EAST, this.enabledSidesMask))
        {
            this.renderGridLinesX(box, box.maxX, color, builder, e);
        }
    }

    protected void renderGridLinesX(Box box, double x, Color4f color, BufferBuilder buffer, MatrixStack.Entry e)
    {
        double end = box.maxY - this.gridEndOffset.y;
        double min = box.minZ + this.gridStartOffset.z;
        double max = box.maxZ - this.gridEndOffset.z;

        for (double y = box.minY + this.gridStartOffset.y; y <= end; y += this.gridSize.y)
        {
            buffer.vertex(e, (float) x, (float) y, (float) min).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
            buffer.vertex(e, (float) x, (float) y, (float) max).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        }

        end = box.maxZ - this.gridEndOffset.z;
        min = box.minY + this.gridStartOffset.y;
        max = box.maxY - this.gridEndOffset.y;

        for (double z = box.minZ + this.gridStartOffset.z; z <= end; z += this.gridSize.z)
        {
            buffer.vertex(e, (float) x, (float) min, (float) z).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
            buffer.vertex(e, (float) x, (float) max, (float) z).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        }
    }

    protected void renderGridLinesY(Box box, double y, Color4f color, BufferBuilder buffer, MatrixStack.Entry e)
    {
        double end = box.maxX - this.gridEndOffset.x;
        double min = box.minZ + this.gridStartOffset.z;
        double max = box.maxZ - this.gridEndOffset.z;

        for (double x = box.minX + this.gridStartOffset.x; x <= end; x += this.gridSize.x)
        {
            buffer.vertex(e, (float) x, (float) y, (float) min).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
            buffer.vertex(e, (float) x, (float) y, (float) max).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        }

        end = box.maxZ - this.gridEndOffset.z;
        min = box.minX + this.gridStartOffset.x;
        max = box.maxX - this.gridEndOffset.x;

        for (double z = box.minZ + this.gridStartOffset.z; z <= end; z += this.gridSize.z)
        {
            buffer.vertex(e, (float) min, (float) y, (float) z).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
            buffer.vertex(e, (float) max, (float) y, (float) z).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        }
    }

    protected void renderGridLinesZ(Box box, double z, Color4f color, BufferBuilder buffer, MatrixStack.Entry e)
    {
        double end = box.maxX - this.gridEndOffset.x;
        double min = box.minY + this.gridStartOffset.y;
        double max = box.maxY - this.gridEndOffset.y;

        for (double x = box.minX + this.gridStartOffset.x; x <= end; x += this.gridSize.x)
        {
            buffer.vertex(e, (float) x, (float) min, (float) z).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
            buffer.vertex(e, (float) x, (float) max, (float) z).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        }

        end = box.maxY - this.gridEndOffset.y;
        min = box.minX + this.gridStartOffset.x;
        max = box.maxX - this.gridEndOffset.x;

        for (double y = box.minY + this.gridStartOffset.y; y <= end; y += this.gridSize.y)
        {
            buffer.vertex(e, (float) min, (float) y, (float) z).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
            buffer.vertex(e, (float) max, (float) y, (float) z).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        }
    }

    public boolean isSideEnabled(Direction side)
    {
        return isSideEnabled(side, this.enabledSidesMask);
    }

    public static boolean isSideEnabled(Direction side, int enabledSidesMask)
    {
        return (enabledSidesMask & (1 << side.getIndex())) != 0;
    }

    public static void renderBoxSideQuad(Box box, Direction side, Color4f color, BufferBuilder buffer)
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
                buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a);
                break;

            case UP:
                buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a);
                break;

            case NORTH:
                buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a);
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
                buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a);
                break;
        }
    }

    protected void renderBoxEnabledEdgeLines(Box box, Color4f color, int enabledSidesMask, BufferBuilder buffer, MatrixStack.Entry e)
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
            buffer.vertex(e, minX, minY, minZ).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
            buffer.vertex(e, maxX, minY, minZ).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        }

        if (up || north)
        {
            buffer.vertex(e, minX, maxY, minZ).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
            buffer.vertex(e, maxX, maxY, minZ).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        }

        if (down || south)
        {
            buffer.vertex(e, minX, minY, maxZ).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
            buffer.vertex(e, maxX, minY, maxZ).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        }

        if (up || south)
        {
            buffer.vertex(e, minX, maxY, maxZ).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
            buffer.vertex(e, maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        }

        // Lines along the z-axis
        if (down || west)
        {
            buffer.vertex(e, minX, minY, minZ).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
            buffer.vertex(e, minX, minY, maxZ).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        }

        if (up || west)
        {
            buffer.vertex(e, minX, maxY, minZ).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
            buffer.vertex(e, minX, maxY, maxZ).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        }

        if (down || east)
        {
            buffer.vertex(e, maxX, minY, minZ).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
            buffer.vertex(e, maxX, minY, maxZ).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        }

        if (up || east)
        {
            buffer.vertex(e, maxX, maxY, minZ).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
            buffer.vertex(e, maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        }

        // Lines along the y-axis
        if (north || west)
        {
            buffer.vertex(e, minX, minY, minZ).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
            buffer.vertex(e, minX, maxY, minZ).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        }

        if (south || west)
        {
            buffer.vertex(e, minX, minY, maxZ).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
            buffer.vertex(e, minX, maxY, maxZ).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        }

        if (north || east)
        {
            buffer.vertex(e, maxX, minY, minZ).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
            buffer.vertex(e, maxX, maxY, minZ).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        }

        if (south || east)
        {
            buffer.vertex(e, maxX, minY, maxZ).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
            buffer.vertex(e, maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        }
    }

    @Override
    public List<String> getWidgetHoverLines()
    {
        List<String> lines = super.getWidgetHoverLines();
        Box box = this.box;
        lines.add(StringUtils.translate("minihud.gui.label.shape.box.min_corner", box.minX, box.minY, box.minZ));
        lines.add(StringUtils.translate("minihud.gui.label.shape.box.max_corner", box.maxX, box.maxY, box.maxZ));
        return lines;
    }

    @Override
    public JsonObject toJson()
    {
        JsonObject obj = super.toJson();

        obj.addProperty("enabled_sides", this.enabledSidesMask);
        obj.addProperty("grid_enabled", this.gridEnabled);
        obj.add("grid_size",         JsonUtils.vec3dToJson(this.gridSize));
        obj.add("grid_start_offset", JsonUtils.vec3dToJson(this.gridStartOffset));
        obj.add("grid_end_offset",   JsonUtils.vec3dToJson(this.gridEndOffset));

        obj.add("corner1", JsonUtils.vec3dToJson(this.corner1));
        obj.add("corner2", JsonUtils.vec3dToJson(this.corner2));

        return obj;
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

        if (this.gridSize == null)        { this.gridSize = new Vec3d(16.0, 16.0, 16.0); }
        if (this.gridStartOffset == null) { this.gridStartOffset = Vec3d.ZERO; }
        if (this.gridEndOffset == null)   { this.gridEndOffset = Vec3d.ZERO; }

        Vec3d corner1 = JsonUtils.vec3dFromJson(obj, "corner1");
        Vec3d corner2 = JsonUtils.vec3dFromJson(obj, "corner2");

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

            this.corner1 = new Vec3d(minX, minY, minZ);
            this.corner2 = new Vec3d(maxX, maxY, maxZ);
        }

        this.setBoxFromCorners();
    }
}
