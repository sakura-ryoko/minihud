package fi.dy.masa.minihud.renderer.shapes;

import java.util.List;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;

import fi.dy.masa.malilib.util.MathUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.data.json.JsonUtils;
import fi.dy.masa.malilib.util.position.Quadrant;
import fi.dy.masa.malilib.util.position.Vec3d;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.renderer.OverlayRendererLightLevel;
import fi.dy.masa.minihud.util.LightLevelPosLimiter;
import fi.dy.masa.minihud.util.shape.SphereUtils;

public class ShapeSpawnSphere extends ShapeSphereBlocky
{
    protected Vec3d[] quadrantCenters;
    protected boolean useCornerQuadrants;
    protected double margin = 0.0;

    private long cacheKey = -1;
    private boolean mayIntersectCache;
    private int minYCache, maxYCache, xzMaskCache;

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
    public void update(Vec3d cameraPos, Entity entity, Minecraft mc, ProfilerFiller profiler)
    {
        super.update(cameraPos, entity, mc, profiler);

        this.cacheKey = -1;
        OverlayRendererLightLevel.INSTANCE.onSpawnSphereUpdate();
    }

    @Override
    public void toggleEnabled()
    {
        super.toggleEnabled();

        if (this.isEnabled() == false)
        {
            this.cacheKey = -1;
            OverlayRendererLightLevel.INSTANCE.onSpawnSphereUpdate();
        }
    }

    @Override
    protected void updateEffectiveCenter()
    {
        super.updateEffectiveCenter();
        this.updateQuadrantPoints();
    }

    private void updateQuadrantPoints()
    {
        Vec3d center = this.getEffectiveCenter();

        if (this.quadrantCenters == null)
        {
            this.quadrantCenters = new Vec3d[4];
        }

        this.quadrantCenters[Quadrant.NORTH_WEST.ordinal()] = new Vec3d(center.x - this.margin, center.y, center.z - this.margin);
        this.quadrantCenters[Quadrant.NORTH_EAST.ordinal()] = new Vec3d(center.x + this.margin, center.y, center.z - this.margin);
        this.quadrantCenters[Quadrant.SOUTH_WEST.ordinal()] = new Vec3d(center.x - this.margin, center.y, center.z + this.margin);
        this.quadrantCenters[Quadrant.SOUTH_EAST.ordinal()] = new Vec3d(center.x + this.margin, center.y, center.z + this.margin);

        this.setNeedsUpdate();
    }

    @Override
    public void onRemove()
    {
        OverlayRendererLightLevel.INSTANCE.onSpawnSphereUpdate();
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

    public boolean contains(int x, int y, int z)
    {
        return isPositionOnOrInsideRing(x, y, z, Direction.DOWN);
    }

    private void updateCache(int chunkX, int chunkZ)
    {
        long key = ((long) chunkX << 32L) | ((long) chunkZ & ((1L << 32L) - 1L));
        if (this.cacheKey == key)
        {
            return;
        }
        this.cacheKey = key;

        final Vec3d effectiveCenter = this.getEffectiveCenter();
        double x = MathUtils.clamp(effectiveCenter.x, chunkX << 4, (chunkX << 4) + 16);
        double z = MathUtils.clamp(effectiveCenter.z, chunkZ << 4, (chunkZ << 4) + 16);
        double margin = this.margin;
        double centerX = MathUtils.clamp(x, effectiveCenter.x - margin, effectiveCenter.x + margin);
        double centerZ = MathUtils.clamp(z, effectiveCenter.z - margin, effectiveCenter.z + margin);
        double distX = x - centerX;
        double distZ = z - centerZ;
        double distXSq = distX * distX;
        double distZSq = distZ * distZ;
        double minDistSq = distXSq + distZSq;

        double radiusSq = this.getSquaredRadius();
        this.mayIntersectCache = minDistSq < radiusSq;

        if (this.mayIntersectCache)
        {
            double r = Math.sqrt(Math.max(0, radiusSq - minDistSq));
            this.minYCache = (int) Math.floor(effectiveCenter.y - r);
            this.maxYCache = (int) Math.ceil(effectiveCenter.y + r);
            double xr = Math.sqrt(Math.max(0, radiusSq - distZSq));
            int minX = (int) Math.floor(centerX - xr);
            int maxX = (int) Math.ceil(centerX + xr);
            double zr = Math.sqrt(Math.max(0, radiusSq - distXSq));
            int minZ = (int) Math.floor(centerZ - zr);
            int maxZ = (int) Math.ceil(centerZ + zr);
            this.xzMaskCache = LightLevelPosLimiter.createXZMask(chunkX, chunkZ, minX, minZ, maxX, maxZ);
        }
    }

    public boolean mayIntersectWithChunk(int chunkX, int chunkZ)
    {
        this.updateCache(chunkX, chunkZ);
        return this.mayIntersectCache;
    }

    // I'm not sure if this is accurate if useCornerQuadrants is true.
    // let's hope that no one would use it with 100 margin.
    public boolean containsChunkLayer(int chunkX, int chunkZ, int y)
    {
        int ox = chunkX << 4;
        int oz = chunkZ << 4;
        final Vec3d effectiveCenter = this.getEffectiveCenter();
        int x = effectiveCenter.x > ox + 8 ? ox - 2 : ox + 18;
        int z = effectiveCenter.z > oz + 8 ? oz - 2 : oz + 18;
        return contains(x, y, z);
    }

    public int getChunkIntersectMinY(int chunkX, int chunkZ)
    {
        this.updateCache(chunkX, chunkZ);
        return this.minYCache;
    }

    public int getChunkIntersectMaxY(int chunkX, int chunkZ)
    {
        this.updateCache(chunkX, chunkZ);
        return this.maxYCache;
    }

    public int getChunkIntersectXZMask(int chunkX, int chunkZ)
    {
        this.updateCache(chunkX, chunkZ);
        return this.xzMaskCache;
    }

    @Override
    protected SphereUtils.RingPositionTest getPositionTest()
    {
        return this::isPositionOnOrInsideRing;
    }

    protected boolean isPositionOnOrInsideRing(int x, int y, int z, Direction outSide)
    {
        final Vec3d effectiveCenter = this.getEffectiveCenter();
        final double maxDistSq = this.getSquaredRadius();
        final double posX = x + 0.5;
        final double posY = y + 1;
        final double posZ = z + 0.5;

        if (this.useCornerQuadrants)
        {
            Vec3d quadrantCenter = this.quadrantCenters[Quadrant.getQuadrant(x, z, effectiveCenter.toVanilla()).ordinal()];

            return quadrantCenter.getSquaredDistanceTo(posX, posY, posZ) < maxDistSq ||
                   effectiveCenter.getSquaredDistanceTo(posX, posY, posZ) < maxDistSq;
        }
        else
        {
            double margin = this.margin;
            double centerX = MathUtils.clamp(posX, effectiveCenter.x - margin, effectiveCenter.x + margin);
            double centerY = effectiveCenter.y;
            double centerZ = MathUtils.clamp(posZ, effectiveCenter.z - margin, effectiveCenter.z + margin);
            double distX = posX - centerX;
            double distY = posY - centerY;
            double distZ = posZ - centerZ;
            double distSq = distX * distX + distY * distY + distZ * distZ;
            return distSq < maxDistSq;
        }
    }
}
