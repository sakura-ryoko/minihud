package fi.dy.masa.minihud.info.camera;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.util.SpeedUnits;

public class InfoLineRotationYaw extends InfoLine
{
	private static final String SPEED_KEY = Reference.MOD_ID+".info_line.speed_";
	private static final String ROT_YAW_KEY = Reference.MOD_ID+".info_line.rotation_yaw";
	private static final String ROT_PITCH_KEY = Reference.MOD_ID+".info_line.rotation_pitch";

    public InfoLineRotationYaw(InfoToggle type)
    {
        super(type);
    }

    public InfoLineRotationYaw()
    {
        this(InfoToggle.ROTATION_YAW);
    }

    @Override
    public boolean succeededType() { return this.succeeded; }

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
	    String pre = "";
	    StringBuilder str = new StringBuilder(128);

	    if (InfoToggle.ROTATION_YAW.getBooleanValue())
	    {
		    str.append(this.qt(ROT_YAW_KEY, MathHelper.wrapDegrees(ent.getYaw())));
		    pre = " / ";
	    }

	    if (InfoToggle.ROTATION_PITCH.getBooleanValue())
	    {
		    str.append(pre).append(this.qt(ROT_PITCH_KEY, MathHelper.wrapDegrees(ent.getPitch())));
		    pre = " / ";
	    }

		if (InfoToggle.SPEED.getBooleanValue())
		{
			double dx = ent.getX() - ent.lastRenderX;
			double dy = ent.getY() - ent.lastRenderY;
			double dz = ent.getZ() - ent.lastRenderZ;
			double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

			str.append(pre).append(this.qt(SPEED_KEY + speedUnits.suffix,
			                               speedUnits.convert(dist * 20)
			));
		}

	    list.add(this.of(str.toString()));
	    this.succeeded = true;

	    return list;
    }
}
