package fi.dy.masa.minihud.info.chunk;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

public abstract class InfoLineBlockChunkRegionBase extends InfoLine
{
    private static final String BLOCK_KEY = Reference.MOD_ID+".info_line.block_pos";
	private static final String CHUNK_KEY = Reference.MOD_ID+".info_line.chunk_pos";
	private static final String REGION_KEY = Reference.MOD_ID+".info_line.region_file";

    public InfoLineBlockChunkRegionBase(InfoToggle type)
    {
        super(type);
    }

    @Override
    public boolean succeededType() { return this.succeeded; }

    @Override
    public List<Entry> parse(@NotNull InfoLineContext ctx)
    {
        if (ctx.world() == null) return null;

        if (ctx.pos() != null && ctx.chunkPos() != null)
        {
	        List<Entry> list = new ArrayList<>();
	        String pre = "";
	        StringBuilder str = new StringBuilder(256);

	        if (InfoToggle.BLOCK_POS.getBooleanValue())
	        {
		        try
		        {
			        String fmt = Configs.Generic.BLOCK_POS_FORMAT_STRING.getStringValue();
			        str.append(String.format(fmt, ctx.pos().getX(), ctx.pos().getY(), ctx.pos().getZ()));
		        }
		        // Uh oh, someone done goofed their format string... :P
		        catch (Exception e)
		        {
			        str.append(StringUtils.translate(BLOCK_KEY+".exception"));
		        }

		        pre = " / ";
	        }

	        if (InfoToggle.CHUNK_POS.getBooleanValue())
	        {
		        str.append(pre).append(this.qt(CHUNK_KEY, ctx.chunkPos().x, ctx.pos().getY() >> 4, ctx.chunkPos().z));
		        pre = " / ";
	        }

	        if (InfoToggle.REGION_FILE.getBooleanValue())
	        {
		        str.append(pre).append(this.qt(REGION_KEY, ctx.pos().getX() >> 9, ctx.pos().getZ() >> 9));
	        }

	        list.add(this.of(str.toString()));
			this.succeeded = true;

	        return list;
        }

		return null;
    }
}
