package fi.dy.masa.minihud.info.camera;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.util.DataStorage;

public class InfoLineDistance extends InfoLine
{
    private static final String DIST_KEY = Reference.MOD_ID+".info_line.distance";

    public InfoLineDistance(InfoToggle type)
    {
        super(type);
    }

    public InfoLineDistance()
    {
        this(InfoToggle.DISTANCE);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@Nonnull Context ctx)
    {
        if (ctx.world() == null) return null;

        return ctx.ent() != null ? this.parseEnt(ctx.world(), ctx.ent()) : null;
    }

    @Override
    public List<Entry> parseEnt(@Nonnull World world, @Nonnull Entity ent)
    {
        List<Entry> list = new ArrayList<>();
	    Vec3d ref = DataStorage.getInstance().getDistanceReferencePoint();
	    double dist = Math.sqrt(ref.squaredDistanceTo(ent.getX(), ent.getY(), ent.getZ()));

	    list.add(this.translate(DIST_KEY,
                                dist,
                                ent.getX() - ref.x,
                                ent.getY() - ref.y,
                                ent.getZ() - ref.z,
                                ref.x, ref.y, ref.z));

        return list;
    }
}
