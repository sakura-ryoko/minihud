package fi.dy.masa.minihud.info.generic;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

public class InfoLineParticleCount extends InfoLine
{
    private static final String PARTICLE_KEY = Reference.MOD_ID+".info_line.particle_count";

    public InfoLineParticleCount(InfoToggle type)
    {
        super(type);
    }

    public InfoLineParticleCount()
    {
        this(InfoToggle.PARTICLE_COUNT);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@Nonnull InfoLineContext ctx)
    {
        List<Entry> list = new ArrayList<>();

        if (this.getClientWorld() == null)
        {
            return null;
        }

        list.add(this.translate(PARTICLE_KEY, this.mc().particleEngine.countParticles()));

        return list;
    }
}
