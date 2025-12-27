package fi.dy.masa.minihud.info.generic;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import fi.dy.masa.malilib.gui.GuiBase;
//import fi.dy.masa.malilib.util.time.TickUtils;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.data.ServuxTickData;
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

        // todo 1.21.8+
//        if (TickUtils.isValid())
//        {
//            // TickUtils constantly refreshes the Measures MSPT to be able to take over in the event the server stops;
//            // and it is required in order to calculate the isSprinting() correctly.
//            final double tps = TickUtils.hasDirectData() ? TickUtils.getDirectTPS() : TickUtils.getMeasuredTPS();
//            final double mspt = TickUtils.hasDirectData() ? TickUtils.getDirectMSPT() : TickUtils.getMeasuredMSPT();
////            double actualTps = TickUtils.getActualTPS();
//            boolean isSprinting = TickUtils.isSprinting();
//            boolean isFrozen = TickUtils.isFrozen();
//
//            StringBuilder builder = new StringBuilder();
//            String rst = GuiBase.TXT_RST;
//            String preTps = tps >= 20.0D ? GuiBase.TXT_GREEN : GuiBase.TXT_RED;
//            String preMspt;
//            String append = isSprinting ? this.qt(TPS_KEY+".sprinting") : (isFrozen ? this.qt(TPS_KEY+".frozen") : "");
//
//            if ((this.getHudData().hasServuxServer() && TickUtils.hasServuxData()) ||
//                (this.getData().hasCarpetServer() && TickUtils.hasDirectData()) ||
//                (this.getData().isSinglePlayer() && this.getData().hasIntegratedServer())
//            )
//            {
//                if      (mspt <= 40) { preMspt = GuiBase.TXT_GREEN; }
//                else if (mspt <= 45) { preMspt = GuiBase.TXT_YELLOW; }
//                else if (mspt <= 50) { preMspt = GuiBase.TXT_GOLD; }
//                else                 { preMspt = GuiBase.TXT_RED; }
//
//                builder.append(this.qt(TPS_KEY, preTps, tps, rst, preMspt, mspt, rst));
//
//                if (!append.isEmpty())
//                {
//                    builder.append(append);
//                }
//
//                list.add(this.of(builder.toString()));
//            }
//            else
//            {
//                if (mspt <= 51) { preMspt = GuiBase.TXT_GREEN; }
//                else            { preMspt = GuiBase.TXT_RED; }
//
//                builder.append(this.qt(TPS_KEY+".est", preTps, tps, rst, preMspt, mspt, rst));
//
//                if (!append.isEmpty())
//                {
//                    builder.append(append);
//                }
//
//                list.add(this.of(builder.toString()));
//            }
//        }

        if (this.getData().hasIntegratedServer() && (this.getData().getIntegratedServer().getTicks() % 10) == 0)
        {
            this.getData().updateIntegratedServerTPS();
        }

        if (this.getData().hasTPSData())
        {
            double tps = this.getData().getServerTPS();
            double mspt = this.getData().getServerMSPT();
            String rst = GuiBase.TXT_RST;
            String preTps = tps >= 20.0D ? GuiBase.TXT_GREEN : GuiBase.TXT_RED;
            String preMspt;

            // Get the direct data dump from Servux (Without using TickUtils...)
            if (this.getData().hasServuxTickData() && this.getData().getServuxTickData() != null)
            {
                StringBuilder builder = new StringBuilder();
                ServuxTickData tickData = this.getData().getServuxTickData();
                String append = tickData.sprinting() ? this.qt(TPS_KEY+".sprinting") : (tickData.frozen() ? this.qt(TPS_KEY+".frozen") : "");

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
            // Carpet server and integrated server have actual meaningful MSPT data available
            else if (this.getData().hasCarpetServer() || this.getData().isSinglePlayer())
            {
                if      (mspt <= 40) { preMspt = GuiBase.TXT_GREEN; }
                else if (mspt <= 45) { preMspt = GuiBase.TXT_YELLOW; }
                else if (mspt <= 50) { preMspt = GuiBase.TXT_GOLD; }
                else                 { preMspt = GuiBase.TXT_RED; }

                list.add(this.translate(TPS_KEY, preTps, tps, rst, preMspt, mspt, rst));
            }
            else
            {
                if (mspt <= 51) { preMspt = GuiBase.TXT_GREEN; }
                else            { preMspt = GuiBase.TXT_RED; }

                list.add(this.translate(TPS_KEY+".est", preTps, tps, rst, preMspt, mspt, rst));
            }
        }
        else
        {
            list.add(this.translate(TPS_KEY+".invalid"));
        }

        return list;
    }
}
