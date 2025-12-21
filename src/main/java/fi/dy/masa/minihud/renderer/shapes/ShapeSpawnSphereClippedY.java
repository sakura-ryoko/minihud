package fi.dy.masa.minihud.renderer.shapes;

import java.util.List;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.util.shape.SphereUtils;

public class ShapeSpawnSphereClippedY extends ShapeSpawnSphere
{
    private double topTrim;
    private double bottomTrim;

    public ShapeSpawnSphereClippedY()
    {
        this(ShapeType.CLIPPED_SPAWN_SPHERE_Y, Configs.Colors.SHAPE_ADJUSTABLE_SPAWN_SPHERE.getColor(), 24.0);
    }

    public ShapeSpawnSphereClippedY(ShapeType shape, Color4f color, double radius)
    {
        super(shape, color, radius);
        this.topTrim = 0.0;
        this.bottomTrim = 0.0;
    }

    public double getTopTrim()
    {
        return this.topTrim;
    }

    public double getBottomTrim()
    {
        return this.bottomTrim;
    }

    public void setTopTrim(double v)
    {
        this.topTrim = Math.max(0.0, v);
        this.setNeedsUpdate();
    }

    public void setBottomTrim(double v)
    {
        this.bottomTrim = Math.max(0.0, v);
        this.setNeedsUpdate();
    }

    @Override
    protected SphereUtils.RingPositionTest getPositionTest()
    {
        return this::isInsideSphereWithYClip;
    }

    private boolean isInsideSphereWithYClip(int x, int y, int z, Direction outSide)
    {
        Vec3 c = this.getEffectiveCenter();
        double r = this.getRadius();

        double minY = c.y - (r - this.bottomTrim);
        double maxY = c.y + (r - this.topTrim);

        double py = y + 0.5;
        if (py < minY || py > maxY)
        {
            return false;
        }

        return super.getPositionTest().isInsideOrCloserThan(x, y, z, outSide);
    }

    @Override
    public List<String> getWidgetHoverLines()
    {
        List<String> lines = super.getWidgetHoverLines();
        // Insert after Radius (index 3)
        lines.add(4, StringUtils.translate("minihud.gui.hover.shape.clip_top_value", d2(this.topTrim)));
        lines.add(5, StringUtils.translate("minihud.gui.hover.shape.clip_bottom_value", d2(this.bottomTrim)));
        return lines;
    }

    @Override
    public JsonObject toJson()
    {
        JsonObject obj = super.toJson();
        obj.add("top_trim", new JsonPrimitive(this.topTrim));
        obj.add("bottom_trim", new JsonPrimitive(this.bottomTrim));
        return obj;
    }

    @Override
    public void fromJson(JsonObject obj)
    {
        super.fromJson(obj);

        if (JsonUtils.hasDouble(obj, "top_trim"))
        {
            this.topTrim = JsonUtils.getDouble(obj, "top_trim");
        }

        if (JsonUtils.hasDouble(obj, "bottom_trim"))
        {
            this.bottomTrim = JsonUtils.getDouble(obj, "bottom_trim");
        }

        this.setNeedsUpdate();
    }
}
