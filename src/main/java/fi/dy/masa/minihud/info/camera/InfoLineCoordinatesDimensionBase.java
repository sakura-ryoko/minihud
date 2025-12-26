package fi.dy.masa.minihud.info.camera;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.world.level.Level;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

public abstract class InfoLineCoordinatesDimensionBase extends InfoLine
{
    private static final String COORD_KEY = Reference.MOD_ID+".info_line.coordinates";
	private static final String COORD_SCALED_KEY = Reference.MOD_ID+".info_line.coordinates_scaled";
	private static final String DIM_KEY = Reference.MOD_ID+".info_line.dimension";

    public InfoLineCoordinatesDimensionBase(InfoToggle type)
    {
        super(type);
    }

    public InfoLineCoordinatesDimensionBase()
    {
        this(InfoToggle.COORDINATES);
    }

    @Override
    public boolean succeededType() { return this.succeeded; }

    @Override
    public List<Entry> parse(@Nonnull InfoLineContext ctx)
    {
        if (ctx.world() == null || ctx.ent() == null) return null;

        List<Entry> list = new ArrayList<>();
	    String pre = "";
	    StringBuilder str = new StringBuilder(128);
	    String fmtStr = Configs.Generic.COORDINATE_FORMAT_STRING.getStringValue();
	    double x = ctx.ent().getX();
	    double y = ctx.ent().getY();
	    double z = ctx.ent().getZ();

	    if (InfoToggle.COORDINATES.getBooleanValue())
	    {
		    if (Configs.Generic.USE_CUSTOMIZED_COORDINATES.getBooleanValue())
		    {
			    try
			    {
				    str.append(String.format(fmtStr, x, y, z));
			    }
			    // Uh oh, someone done goofed their format string... :P
			    catch (Exception e)
			    {
				    str.append(this.qt(COORD_KEY+".exception"));
			    }
		    }
		    else
		    {
			    str.append(this.qt(COORD_KEY+".format", x, y, z));
		    }

		    pre = " / ";
	    }

	    if (InfoToggle.COORDINATES_SCALED.getBooleanValue() &&
		    (ctx.world().dimension() == Level.NETHER || ctx.world().dimension() == Level.OVERWORLD))
	    {
		    boolean isNether = ctx.world().dimension() == Level.NETHER;
		    double scale = isNether ? 8.0 : 1.0 / 8.0;
		    x *= scale;
		    z *= scale;

		    str.append(pre);

		    if (isNether)
		    {
			    str.append(this.qt(COORD_SCALED_KEY+".overworld"));
		    }
		    else
		    {
			    str.append(this.qt(COORD_SCALED_KEY+".nether"));
		    }

		    if (Configs.Generic.USE_CUSTOMIZED_COORDINATES.getBooleanValue())
		    {
			    try
			    {
				    str.append(String.format(fmtStr, x, y, z));
			    }
			    // Uh oh, someone done goofed their format string... :P
			    catch (Exception e)
			    {
				    str.append(this.qt(COORD_KEY+".exception"));
			    }
		    }
		    else
		    {
			    str.append(this.qt(COORD_KEY+".format", x, y, z));
		    }

		    pre = " / ";
	    }

	    if (InfoToggle.DIMENSION.getBooleanValue())
	    {
		    String dimName = ctx.world().dimension().identifier().toString();
		    str.append(pre).append(this.qt(DIM_KEY)).append(dimName);
	    }

	    list.add(this.of(str.toString()));
	    this.succeeded = true;

        return list;
    }
}
