package fi.dy.masa.minihud.info.camera;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

public class InfoLineFacing extends InfoLine
{
    private static final String FACING_KEY = Reference.MOD_ID+".info_line.facing";

    public InfoLineFacing(InfoToggle type)
    {
        super(type);
    }

    public InfoLineFacing()
    {
        this(InfoToggle.FACING);
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
        List<Entry> list = new ArrayList<>();

	    Direction facing = ent.getDirection();
	    String facingName = StringUtils.translate(FACING_KEY+"." + facing.name().toLowerCase() + ".name");
	    String str;

	    if (facingName.contains(FACING_KEY+"." + facing.name().toLowerCase() + ".name"))
	    {
		    facingName = facing.name().toLowerCase();
		    str = StringUtils.translate("minihud.info_line.invalid_value");
	    }
	    else
	    {
		    str = StringUtils.translate(FACING_KEY+"." + facing.name().toLowerCase());
	    }

	    list.add(this.translate(FACING_KEY, facingName, str));

        return list;
    }
}
