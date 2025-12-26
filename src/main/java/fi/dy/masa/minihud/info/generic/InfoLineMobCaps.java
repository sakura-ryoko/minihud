package fi.dy.masa.minihud.info.generic;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.data.MobCapDataHandler;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

public class InfoLineMobCaps extends InfoLine
{
    private static final String MOB_CAP_KEY = Reference.MOD_ID+".info_line.mobcap";

    public InfoLineMobCaps(InfoToggle type)
    {
        super(type);
    }

    public InfoLineMobCaps()
    {
        this(InfoToggle.MOB_CAPS);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@Nonnull InfoLineContext ctx)
    {
        List<Entry> list = new ArrayList<>();

        MobCapDataHandler mobCapData = this.getData().getMobCapData();

        // Was 100 ticks
        if (this.getData().hasIntegratedServer() && (this.getData().getIntegratedServer().getTickCount() % 25) == 0)
        {
            mobCapData.updateIntegratedServerMobCaps();
        }

        if (mobCapData.getHasValidData())
        {
            list.add(this.of(
                    mobCapData.getFormattedInfoLine()
            ));
        }

        return list;
    }
}
