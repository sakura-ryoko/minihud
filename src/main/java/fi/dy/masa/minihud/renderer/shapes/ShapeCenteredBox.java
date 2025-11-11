package fi.dy.masa.minihud.renderer.shapes;

import com.google.gson.JsonObject;
import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.minihud.config.Configs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class ShapeCenteredBox extends ShapeBox
{
    protected int width = 16;
    protected int depth = 16;
    protected int height = 16;
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
        this.corner1 = new Vec3(this.center.x - (this.width / 2), this.center.y ,this.center.z - (this.depth/2));
        this.corner2 = new Vec3(this.center.x + (this.width / 2), this.center.y + this.height, this.center.z + (this.depth/2));
        this.setBoxFromCorners();
    }

    public void setCenter(Vec3 center)
	{
        this.center = center;
        this.setBoxFromDimension();
    }

    public void setWidth(int width)
	{
        this.width = Mth.clamp(width, 1,8192);;
        this.setBoxFromDimension();
    }
    
    public void setDepth(int depth)
	{
        this.depth = Mth.clamp(depth, 1,8192);
        this.setBoxFromDimension();
    }
    
    public void setHeight(int height)
	{
        this.height = height;
        this.setBoxFromDimension();
    }

    public Vec3 getCenter()
	{
        return this.center;
    }

    public int getWidth()
	{
        return this.width;
    }

    public int getDepth()
	{
        return this.depth;
    }
    
    public int getHeight()
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
        this.width = JsonUtils.getIntegerOrDefault(obj, "width",1);
        this.depth = JsonUtils.getIntegerOrDefault(obj,"depth",1);
        this.height = JsonUtils.getIntegerOrDefault(obj,"height",1);
        this.setBoxFromDimension();
    }
}