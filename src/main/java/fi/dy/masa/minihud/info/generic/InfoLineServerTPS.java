package fi.dy.masa.minihud.info.generic;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.time.TickUtils;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

public class InfoLineServerTPS extends InfoLine
{
    private static final String TPS_KEY = Reference.MOD_ID+".info_line.server_tps";

    public InfoLineServerTPS(InfoToggle type)
    {
        super(type);
    }

    public InfoLineServerTPS()
    {
        this(InfoToggle.SERVER_TPS);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@Nonnull InfoLineContext ctx)
    {
        List<Entry> list = new ArrayList<>();

        if (TickUtils.isValid())
        {
            // TickUtils constantly refreshes the Measures MSPT to be able to take over in the event the server stops;
            // and it is required in order to calculate the isSprinting() correctly.
            final double tps = TickUtils.hasDirectData() ? TickUtils.getDirectTPS() : TickUtils.getMeasuredTPS();
            final double mspt = TickUtils.hasDirectData() ? TickUtils.getDirectMSPT() : TickUtils.getMeasuredMSPT();
//            double actualTps = TickUtils.getActualTPS();
            boolean isSprinting = TickUtils.isSprinting();
            boolean isFrozen = TickUtils.isFrozen();

            StringBuilder builder = new StringBuilder();
            String rst = GuiBase.TXT_RST;
            String preTps = tps >= 20.0D ? GuiBase.TXT_GREEN : GuiBase.TXT_RED;
            String preMspt;
            String append = isSprinting ? this.qt(TPS_KEY+".sprinting") : (isFrozen ? this.qt(TPS_KEY+".frozen") : "");

            if ((this.getHudData().hasServuxServer() && TickUtils.hasServuxData()) ||
                (this.getData().hasCarpetServer() && TickUtils.hasDirectData()) ||
                (this.getData().isSinglePlayer() && this.getData().hasIntegratedServer())
            )
            {
                if      (mspt <= 40) { preMspt = GuiBase.TXT_GREEN; }
                else if (mspt <= 45) { preMspt = GuiBase.TXT_YELLOW; }
                else if (mspt <= 50) { preMspt = GuiBase.TXT_GOLD; }
                else                 { preMspt = GuiBase.TXT_RED; }

                builder.append(this.qt(TPS_KEY, preTps, tps, rst, preMspt, mspt, rst));

                if (!append.isEmpty())
                {
                    builder.append(append);
                }

                list.add(this.of(builder.toString()));
            }
            else
            {
                if (mspt <= 51) { preMspt = GuiBase.TXT_GREEN; }
                else            { preMspt = GuiBase.TXT_RED; }

                builder.append(this.qt(TPS_KEY+".est", preTps, tps, rst, preMspt, mspt, rst));

                if (!append.isEmpty())
                {
                    builder.append(append);
                }

                list.add(this.of(builder.toString()));
            }
        }
        else
        {
            list.add(this.translate(TPS_KEY+".invalid"));
        }

        return list;
    }
}
