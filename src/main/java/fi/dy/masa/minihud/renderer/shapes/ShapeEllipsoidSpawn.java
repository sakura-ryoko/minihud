package fi.dy.masa.minihud.renderer.shapes;

import java.util.List;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.util.shape.SphereUtils;

public class ShapeEllipsoidSpawn extends ShapeSpawnSphere
{
    protected double radiusY;
    protected double radiusZ;

    public ShapeEllipsoidSpawn()
    {
        this(ShapeType.ELLIPSOID_SPAWN, Configs.Colors.SHAPE_ADJUSTABLE_SPAWN_SPHERE.getColor(), 24.0);
    }

    public ShapeEllipsoidSpawn(ShapeType type, fi.dy.masa.malilib.util.data.Color4f color, double radius)
    {
        super(type, color, radius);
        this.radiusY = radius;
        this.radiusZ = radius;
    }

    public double getRadiusY()
    {
        return this.radiusY;
    }

    public double getRadiusZ()
    {
        return this.radiusZ;
    }

    public void setRadiusY(double radiusY)
    {
        this.radiusY = Mth.clamp(radiusY, 0.0, 1024.0);
        this.setNeedsUpdate();
    }

    public void setRadiusZ(double radiusZ)
    {
        this.radiusZ = Mth.clamp(radiusZ, 0.0, 1024.0);
        this.setNeedsUpdate();
    }

    @Override
    protected SphereUtils.RingPositionTest getPositionTest()
    {
        return this::isPositionOnOrInsideEllipsoidRing;
    }

    protected boolean isPositionOnOrInsideEllipsoidRing(int x, int y, int z, Direction outSide)
    {
        Vec3 center = this.getEffectiveCenter();
        double rx = this.getRadius();
        double ry = this.radiusY;
        double rz = this.radiusZ;

        double dx = (x + 0.5) - center.x;
        double dy = (y + 0.5) - center.y;
        double dz = (z + 0.5) - center.z;

        double normalizedDist = (dx * dx) / (rx * rx) + (dy * dy) / (ry * ry) + (dz * dz) / (rz * rz);

        return normalizedDist <= 1.0;
    }

    @Override
    public List<String> getWidgetHoverLines()
    {
        List<String> lines = super.getWidgetHoverLines();
        // Insert after Radius (index 3)
        lines.add(4, StringUtils.translate("minihud.gui.hover.shape.radius_y_value", d2(this.radiusY)));
        lines.add(5, StringUtils.translate("minihud.gui.hover.shape.radius_z_value", d2(this.radiusZ)));
        return lines;
    }

    @Override
    public JsonObject toJson()
    {
        JsonObject obj = super.toJson();
        obj.add("radius_y", new JsonPrimitive(this.radiusY));
        obj.add("radius_z", new JsonPrimitive(this.radiusZ));
        return obj;
    }

    @Override
    public void fromJson(JsonObject obj)
    {
        super.fromJson(obj);

        if (JsonUtils.hasDouble(obj, "radius_y"))
        {
            this.radiusY = JsonUtils.getDouble(obj, "radius_y");
        }
        else
        {
            this.radiusY = this.getRadius();
        }

        if (JsonUtils.hasDouble(obj, "radius_z"))
        {
            this.radiusZ = JsonUtils.getDouble(obj, "radius_z");
        }
        else
        {
            this.radiusZ = this.getRadius();
        }

        this.setNeedsUpdate();
    }
}
