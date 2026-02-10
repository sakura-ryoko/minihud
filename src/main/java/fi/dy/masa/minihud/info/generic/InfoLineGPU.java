package fi.dy.masa.minihud.info.generic;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

@Deprecated
public class InfoLineGPU extends InfoLine
{
    private static final String GPU_KEY = Reference.MOD_ID + ".info_line.gpu";

    public InfoLineGPU(InfoToggle type)
    {
        super(type);
    }

//    public InfoLineGPU()
//    {
//        this(InfoToggle.GPU);
//    }

    @Override
    public boolean succeededType() {return false;}

    @Override
    public List<Entry> parse(@Nonnull InfoLineContext ctx)
    {
        List<Entry> list = new ArrayList<>();

        if (this.getClientWorld() == null)
        {
            return null;
        }

        // Problematic
//		final double gpu = InfoLineProfiler.INSTANCE.getGpuUtilization();
//		String gpuString = "" + (gpu > 100.0 ? GuiBase.TXT_RED + "100" : Math.round(gpu));
//        list.add(this.translate(GPU_KEY, gpuString));

        return list;
    }
}
