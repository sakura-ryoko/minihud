package fi.dy.masa.minihud.renderer.shapes;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.Quadrant;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.util.shape.SphereUtils;

public class ShapeSpawnSphere extends ShapeSphereBlocky
{
    protected Vec3[] quadrantCenters;
    protected boolean useCornerQuadrants;
    protected double margin = 0.0;

    public ShapeSpawnSphere()
    {
        this(ShapeType.ADJUSTABLE_SPAWN_SPHERE, Configs.Colors.SHAPE_ADJUSTABLE_SPAWN_SPHERE.getColor(), 24.0);
    }

    public ShapeSpawnSphere(ShapeType shape, Color4f color, double radius)
    {
        super(shape, color, radius);

        this.updateQuadrantPoints();
    }

    public boolean getUseCornerQuadrants()
    {
        return this.useCornerQuadrants;
    }

    public void toggleUseCornerQuadrants()
    {
        this.useCornerQuadrants = ! this.useCornerQuadrants;
        this.setNeedsUpdate();
    }

    @Override
    protected void updateEffectiveCenter()
    {
        super.updateEffectiveCenter();
        this.updateQuadrantPoints();
    }

    private void updateQuadrantPoints()
    {
        Vec3 center = this.getEffectiveCenter();

        if (this.quadrantCenters == null)
        {
            this.quadrantCenters = new Vec3[4];
        }

        this.quadrantCenters[Quadrant.NORTH_WEST.ordinal()] = new Vec3(center.x - this.margin, center.y, center.z - this.margin);
        this.quadrantCenters[Quadrant.NORTH_EAST.ordinal()] = new Vec3(center.x + this.margin, center.y, center.z - this.margin);
        this.quadrantCenters[Quadrant.SOUTH_WEST.ordinal()] = new Vec3(center.x - this.margin, center.y, center.z + this.margin);
        this.quadrantCenters[Quadrant.SOUTH_EAST.ordinal()] = new Vec3(center.x + this.margin, center.y, center.z + this.margin);

        this.setNeedsUpdate();
    }

    public double getMargin()
    {
        return this.margin;
    }

    public void setMargin(double margin)
    {
        this.margin = margin;

        // Update the quadrant centers
        this.updateQuadrantPoints();
    }

    @Override
    protected double getTotalRadius()
    {
        return this.getRadius() + this.margin;
    }

    @Override
    public List<String> getWidgetHoverLines()
    {
        List<String> lines = super.getWidgetHoverLines();
        lines.add(2, StringUtils.translate("minihud.gui.hover.shape.margin_value", d2(this.margin)));
        return lines;
    }

    @Override
    public JsonObject toJson()
    {
        JsonObject obj = super.toJson();

		if (obj != null)
		{
			obj.add("margin", new JsonPrimitive(this.margin));
			return obj;
		}

		return new JsonObject();
    }

    @Override
    public void fromJson(JsonObject obj)
    {
        super.fromJson(obj);

        this.setMargin(JsonUtils.getDouble(obj, "margin"));
    }

    @Override
    protected SphereUtils.RingPositionTest getPositionTest()
    {
        return this::isPositionOnOrInsideRing;
    }

    protected boolean isPositionOnOrInsideRing(int x, int y, int z, Direction outSide)
    {
        final Vec3 effectiveCenter = this.getEffectiveCenter();
        final double maxDistSq = this.getSquaredRadius();
        final double posX = x + 0.5;
        final double posY = y + 1;
        final double posZ = z + 0.5;

        if (this.useCornerQuadrants)
        {
            Vec3 quadrantCenter = this.quadrantCenters[Quadrant.getQuadrant(x, z, effectiveCenter).ordinal()];

            return quadrantCenter.distanceToSqr(posX, posY, posZ) < maxDistSq ||
                   effectiveCenter.distanceToSqr(posX, posY, posZ) < maxDistSq;
        }
        else
        {
            double margin = this.margin;
            double centerX = Mth.clamp(posX, effectiveCenter.x - margin, effectiveCenter.x + margin);
            double centerY = effectiveCenter.y;
            double centerZ = Mth.clamp(posZ, effectiveCenter.z - margin, effectiveCenter.z + margin);
            double distX = posX - centerX;
            double distY = posY - centerY;
            double distZ = posZ - centerZ;
            double distSq = distX * distX + distY * distY + distZ * distZ;
            return distSq < maxDistSq;
        }
    }
}
