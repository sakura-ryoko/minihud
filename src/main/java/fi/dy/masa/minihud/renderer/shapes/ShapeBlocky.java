package fi.dy.masa.minihud.renderer.shapes;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import fi.dy.masa.malilib.util.*;
import fi.dy.masa.malilib.util.data.Color4f;

public abstract class ShapeBlocky extends ShapeBase
{
    private BlockSnap snap;
    protected AABB renderPerimeter;
    private boolean combineQuads;

    public ShapeBlocky(ShapeType type, Color4f color)
    {
        super(type, color);
        this.snap = BlockSnap.CENTER;
        this.renderPerimeter = ShapeBox.DEFAULT_BOX;
    }

    public BlockSnap getBlockSnap()
    {
        return this.snap;
    }

    public void setBlockSnap(BlockSnap snap)
    {
        this.snap = snap;
    }

    public boolean getCombineQuads()
    {
        return this.combineQuads;
    }

    public boolean toggleCombineQuads()
    {
        this.combineQuads = ! this.combineQuads;
        this.setNeedsUpdate();
        return this.combineQuads;
    }

    protected void setRenderPerimeter(Vec3 center, double range)
    {
        this.renderPerimeter = new AABB(center.x - range, center.y - range, center.z - range,
                                       center.x + range, center.y + range, center.z + range);
    }

    protected Vec3 getBlockSnappedPosition(Vec3 pos)
    {
        BlockSnap snap = this.getBlockSnap();

        if (snap == BlockSnap.CENTER)
        {
            return new Vec3(Math.floor(pos.x) + 0.5, Math.floor(pos.y), Math.floor(pos.z) + 0.5);
        }
        else if (snap == BlockSnap.CORNER)
        {
            return new Vec3(Math.floor(pos.x), Math.floor(pos.y), Math.floor(pos.z));
        }

        return pos;
    }

    @Override
    public boolean shouldRender(Minecraft mc)
    {
        Entity entity = EntityUtils.getCameraEntity();
        return super.shouldRender(mc) && entity != null && this.renderPerimeter.contains(entity.position());
    }

    @Override
    public List<String> getWidgetHoverLines()
    {
        List<String> lines = super.getWidgetHoverLines();

        BlockSnap snap = this.getBlockSnap();
        lines.add(StringUtils.translate("minihud.gui.hover.shape.block_snap", snap.getDisplayName()));

        return lines;
    }

    @Override
    public JsonObject toJson()
    {
        JsonObject obj = super.toJson();
        obj.add("snap", new JsonPrimitive(this.snap.getStringValue()));
        obj.add("combine_quads", new JsonPrimitive(this.combineQuads));
        return obj;
    }

    @Override
    public void fromJson(JsonObject obj)
    {
        super.fromJson(obj);

        if (JsonUtils.hasString(obj, "snap"))
        {
            this.snap = BlockSnap.fromStringStatic(JsonUtils.getString(obj, "snap"));
        }

        this.combineQuads = JsonUtils.getBooleanOrDefault(obj, "combine_quads", false);
    }

    protected Consumer<BlockPos.MutableBlockPos> getPositionCollector(LongOpenHashSet positionsOut)
    {
        IntBoundingBox box = this.layerRange.getExpandedBox(this.mc.level, 0);

        Consumer<BlockPos.MutableBlockPos> positionCollector = (pos) -> {
            if (box.containsPos(pos))
            {
                positionsOut.add(pos.asLong());
            }
        };

        return positionCollector;
    }
}
