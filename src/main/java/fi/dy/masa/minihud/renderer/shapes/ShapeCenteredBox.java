package fi.dy.masa.minihud.renderer.shapes;

import com.google.gson.JsonObject;
import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.minihud.config.Configs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class ShapeCenteredBox extends ShapeBox
{
    protected double width = 16;
    protected double depth = 16;
    protected double height = 16;
    protected Vec3 center = Vec3.ZERO;

    public ShapeCenteredBox()
	{
        super(ShapeType.CENTERED_BOX, Configs.Colors.SHAPE_BOX.getColor());
    }

	@Override
	public void onShapeInit()
	{
		super.onShapeInit();
		Entity cameraEntity = EntityUtils.getCameraEntity();

		if (cameraEntity != null &&
			this.center == Vec3.ZERO)
		{
			this.setCenter(cameraEntity.position());
		}
	}

    protected void setBoxFromDimension()
	{
        this.corner1 = new Vec3(this.center.x - (this.width / 2), this.center.y - (this.height / 2), this.center.z - (this.depth/2));
        this.corner2 = new Vec3(this.center.x + (this.width / 2), this.center.y + (this.height / 2), this.center.z + (this.depth/2));
        this.setBoxFromCorners();
    }

    public void setCenter(Vec3 center)
	{
        this.center = center;
        this.setBoxFromDimension();
    }

    public void setWidth(double width)
	{
        this.width = Math.max(width, 0.);
        this.setBoxFromDimension();
    }

    public void setDepth(double depth)
	{
        this.depth = Math.max(depth, 0.);
        this.setBoxFromDimension();
    }

    public void setHeight(double height)
	{
        this.height = Math.max(height, 0.);
        this.setBoxFromDimension();
    }

    public Vec3 getCenter()
	{
        return this.center;
    }

    public double getWidth()
	{
        return this.width;
    }

    public double getDepth()
	{
        return this.depth;
    }

    public double getHeight()
	{
        return this.height;
    }

    @Override
    public JsonObject toJson()
	{
        JsonObject obj = super.toJson();

		if (obj != null)
		{
			obj.addProperty("width", this.width);
			obj.addProperty("height", this.height);
			obj.addProperty("depth", this.depth);
			obj.add("center", JsonUtils.vec3dToJson(this.center));

			return obj;
		}

		return new JsonObject();
    }

    @Override
    public void fromJson(JsonObject obj)
	{
        super.fromJson(obj);

        this.center =  JsonUtils.vec3dFromJson(obj, "center");
        this.width = JsonUtils.getDouble(obj, "width");
        this.depth = JsonUtils.getDouble(obj,"depth");
        this.height = JsonUtils.getDouble(obj,"height");
        this.setBoxFromDimension();
    }
}
