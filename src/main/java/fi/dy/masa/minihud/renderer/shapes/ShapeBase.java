package fi.dy.masa.minihud.renderer.shapes;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.malilib.interfaces.IRangeChangeListener;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.renderer.OverlayRendererBase;
import fi.dy.masa.minihud.util.ShapeRenderType;

public abstract class ShapeBase extends OverlayRendererBase implements IRangeChangeListener
{
    protected static final DecimalFormat DEC_FMT = new DecimalFormat("#.##");

    protected final Minecraft mc;
    protected final ShapeType type;
    protected final LayerRange layerRange;
    protected String displayName;
    protected ShapeRenderType renderType;
    protected Color4f color;
    protected Color4f colorLines;
    protected boolean enabled;
    protected boolean needsUpdate;
    protected boolean renderLines;
    protected boolean renderThroughShape;

    public ShapeBase(ShapeType type, Color4f color)
    {
        this.mc = Minecraft.getInstance();
        this.type = type;
        this.color = color;
        this.colorLines = Color4f.WHITE;
        this.layerRange = new LayerRange(this);
        this.renderType = ShapeRenderType.OUTER_EDGE;
        this.displayName = type.getDisplayName();
        this.needsUpdate = true;
        this.renderLines = false;
        this.renderThroughShape = false;
        this.useCulling = false;
    }

    @Override
    public String getName()
    {
        return "Shapes_"+this.displayName;
    }

	/**
	 * Called when a shape is added to the list
	 */
	public void onShapeInit() {}

	public ShapeType getType()
    {
        return this.type;
    }

    public String getDisplayName()
    {
        return this.displayName;
    }

    public LayerRange getLayerRange()
    {
        return this.layerRange;
    }

    public Color4f getColor()
    {
        return this.color;
    }

    public Color4f getColorLines()
    {
        return this.colorLines;
    }

	public ShapeRenderType getRenderType()
    {
        return this.renderType;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    public void setRenderType(ShapeRenderType renderType)
    {
        this.renderType = renderType;
        this.setNeedsUpdate();
    }

    public void setColor(int newColor)
    {
        if (newColor != this.color.intValue)
        {
            this.color = Color4f.fromColor(newColor);
            this.setNeedsUpdate();
        }
    }

    public void setColorFromString(String newValue)
    {
        this.setColor(StringUtils.getColor(newValue, 0));
    }

    public void setColorLines(int newColor)
    {
        if (newColor != this.colorLines.intValue)
        {
            this.colorLines = Color4f.fromColor(newColor, 0xFF);
            this.setNeedsUpdate();
        }
    }

    public void setColorLinesFromString(String newValue)
    {
        this.setColorLines(StringUtils.getColor(newValue, 0));
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public void toggleEnabled()
    {
        this.enabled = ! this.enabled;

        if (this.enabled)
        {
            this.setNeedsUpdate();
        }
    }

    public void toggleRenderLines()
    {
        this.renderLines = ! this.renderLines;
        this.setNeedsUpdate();
    }

    public boolean shouldRenderLines()
    {
        return this.renderLines;
    }

    public void toggleRenderThrough()
    {
        this.renderThroughShape = ! this.renderThroughShape;
        this.setNeedsUpdate();
    }

    public boolean shouldRenderThrough()
    {
        return this.renderThroughShape;
    }

    public void setNeedsUpdate()
    {
        this.needsUpdate = true;
    }

    public void moveToPosition(Vec3 pos)
    {
    }

    @Override
    public boolean shouldRender(Minecraft mc)
    {
        return this.enabled && RendererToggle.SHAPE_RENDERER.getBooleanValue();
    }

    @Override
    public boolean needsUpdate(Entity entity, Minecraft mc)
    {
        return this.needsUpdate;
    }

    @Override
    public void updateAll()
    {
        this.setNeedsUpdate();
    }

    @Override
    public void updateBetweenX(int minX, int maxX)
    {
        this.setNeedsUpdate();
    }

    @Override
    public void updateBetweenY(int minY, int maxY)
    {
        this.setNeedsUpdate();
    }

    @Override
    public void updateBetweenZ(int minZ, int maxZ)
    {
        this.setNeedsUpdate();
    }

    public List<String> getWidgetHoverLines()
    {
        List<String> lines = new ArrayList<>();

        lines.add(StringUtils.translate("minihud.gui.hover.shape.type_value", this.type.getDisplayName()));

        return lines;
    }

    @Override
    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        obj.add("type", new JsonPrimitive(this.type.getId()));
        obj.add("color", new JsonPrimitive(this.color.intValue));
        obj.add("color_lines", new JsonPrimitive(this.colorLines.intValue));
        obj.add("enabled", new JsonPrimitive(this.enabled));
        obj.add("display_name", new JsonPrimitive(this.displayName));
        obj.add("render_lines", new JsonPrimitive(this.renderLines));
        obj.add("render_through", new JsonPrimitive(this.renderThroughShape));
        obj.add("render_type", new JsonPrimitive(this.renderType.getStringValue()));
        obj.add("layers", this.layerRange.toJson());

        return obj;
    }

    @Override
    public void fromJson(JsonObject obj)
    {
        this.enabled = JsonUtils.getBoolean(obj, "enabled");

        if (JsonUtils.hasInteger(obj, "color"))
        {
            this.color = Color4f.fromColor(JsonUtils.getInteger(obj, "color"));
        }

        if (JsonUtils.hasInteger(obj, "color_lines"))
        {
            this.colorLines = Color4f.fromColor(JsonUtils.getInteger(obj, "color_lines"));
        }

        if (JsonUtils.hasObject(obj, "layers"))
        {
            this.layerRange.fromJson(JsonUtils.getNestedObject(obj, "layers", false));
        }

        if (JsonUtils.hasBoolean(obj, "render_lines"))
        {
            this.renderLines = JsonUtils.getBoolean(obj, "render_lines");
        }

        if (JsonUtils.hasBoolean(obj, "render_through"))
        {
            this.renderThroughShape = JsonUtils.getBoolean(obj, "render_through");
        }

        if (JsonUtils.hasString(obj, "render_type"))
        {
            ShapeRenderType type = ShapeRenderType.fromStringStatic(obj.get("render_type").getAsString());

            if (type != null)
            {
                this.renderType = type;
            }
        }

        if (JsonUtils.hasString(obj, "display_name"))
        {
            this.displayName = obj.get("display_name").getAsString();
        }
    }

    public static String d2(double val)
    {
        return DEC_FMT.format(val);
    }
}
