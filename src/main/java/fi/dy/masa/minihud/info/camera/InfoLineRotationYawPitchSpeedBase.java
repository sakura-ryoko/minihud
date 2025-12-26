package fi.dy.masa.minihud.info.camera;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.util.Mth;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;
import fi.dy.masa.minihud.util.SpeedUnits;

public abstract class InfoLineRotationYawPitchSpeedBase extends InfoLine
{
    private static final String SPEED_KEY = Reference.MOD_ID+".info_line.speed_";
	private static final String ROT_YAW_KEY = Reference.MOD_ID+".info_line.rotation_yaw";
	private static final String ROT_PITCH_KEY = Reference.MOD_ID+".info_line.rotation_pitch";

    public InfoLineRotationYawPitchSpeedBase(InfoToggle type)
    {
        super(type);
    }

    @Override
    public boolean succeededType() { return this.succeeded; }

    @Override
    public List<Entry> parse(@Nonnull InfoLineContext ctx)
    {
        if (ctx.world() == null || ctx.ent() == null) return null;

	    SpeedUnits speedUnits = (SpeedUnits) Configs.Generic.SPEED_UNITS.getOptionListValue();
	    List<Entry> list = new ArrayList<>();
	    String pre = "";
	    StringBuilder str = new StringBuilder(128);

	    if (InfoToggle.ROTATION_YAW.getBooleanValue())
	    {
			str.append(this.qt(ROT_YAW_KEY, Mth.wrapDegrees(ctx.ent().getYRot())));
		    pre = " / ";
	    }

	    if (InfoToggle.ROTATION_PITCH.getBooleanValue())
	    {
		    str.append(pre).append(this.qt(ROT_PITCH_KEY, Mth.wrapDegrees(ctx.ent().getXRot())));
		    pre = " / ";
	    }

	    if (InfoToggle.SPEED.getBooleanValue())
	    {
		    double dx = ctx.ent().getX() - ctx.ent().xOld;
		    double dy = ctx.ent().getY() - ctx.ent().yOld;
		    double dz = ctx.ent().getZ() - ctx.ent().zOld;
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
