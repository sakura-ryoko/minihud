package fi.dy.masa.minihud.info.camera;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;
import fi.dy.masa.minihud.util.SpeedUnits;

public class InfoLineSpeedHV extends InfoLine
{
    private static final String SPEED_KEY = Reference.MOD_ID+".info_line.speed_hv_";

    public InfoLineSpeedHV(InfoToggle type)
    {
        super(type);
    }

    public InfoLineSpeedHV()
    {
        this(InfoToggle.SPEED_HV);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@Nonnull InfoLineContext ctx)
    {
        if (ctx.world() == null) return null;

        return ctx.ent() != null ? this.parseEnt(ctx.world(), ctx.ent()) : null;
    }

    @Override
    public List<Entry> parseEnt(@Nonnull Level world, @Nonnull Entity ent)
    {
	    SpeedUnits speedUnits = (SpeedUnits) Configs.Generic.SPEED_UNITS.getOptionListValue();
        List<Entry> list = new ArrayList<>();
	    double dx = ent.getX() - ent.xOld;
	    double dy = ent.getY() - ent.yOld;
	    double dz = ent.getZ() - ent.zOld;

	    list.add(this.translate(SPEED_KEY+speedUnits.suffix,
	                            speedUnits.convert(Math.sqrt(dx * dx + dz * dz) * 20),
	                            speedUnits.convert(dy * 20)
	    ));

        return list;
    }
}
