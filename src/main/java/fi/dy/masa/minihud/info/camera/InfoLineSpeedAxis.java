package fi.dy.masa.minihud.info.camera;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.util.SpeedUnits;

public class InfoLineSpeedAxis extends InfoLine
{
    private static final String SPEED_KEY = Reference.MOD_ID+".info_line.speed_axis_";

    public InfoLineSpeedAxis(InfoToggle type)
    {
        super(type);
    }

    public InfoLineSpeedAxis()
    {
        this(InfoToggle.SPEED_AXIS);
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
	    SpeedUnits speedUnits = (SpeedUnits) Configs.Generic.SPEED_UNITS.getOptionListValue();
	    List<Entry> list = new ArrayList<>();
	    double dx = ent.getX() - ent.lastRenderX;
	    double dy = ent.getY() - ent.lastRenderY;
	    double dz = ent.getZ() - ent.lastRenderZ;

	    list.add(this.translate(SPEED_KEY+ speedUnits.suffix,
	                            speedUnits.convert(dx * 20),
	                            speedUnits.convert(dy * 20),
	                            speedUnits.convert(dz * 20)
	    ));

        return list;
    }
}
