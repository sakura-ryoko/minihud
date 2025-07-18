package fi.dy.masa.minihud.info.generic;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;

public class InfoLineServux extends InfoLine
{
    private static final String SERVUX_KEY = Reference.MOD_ID+".info_line.servux";
    private static final String YES_KEY = Reference.MOD_ID+".info_line.slime_chunk.yes";
    private static final String NO_KEY = Reference.MOD_ID+".info_line.slime_chunk.no";

    public InfoLineServux(InfoToggle type)
    {
        super(type);
    }

    public InfoLineServux()
    {
        this(InfoToggle.SERVUX);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@Nonnull Context ctx)
    {
        List<Entry> list = new ArrayList<>();

        if (this.getEntData().hasServuxServer())
        {
            list.add(this.translate(SERVUX_KEY,
                                    this.getEntData().getServuxVersion()
            ));
        }
        else if (this.getData().hasServuxServer())
        {
            list.add(this.translate(SERVUX_KEY,
                                    this.getData().getServuxVersion()
            ));
        }
        else if (this.getHudData().hasServuxServer())
        {
            list.add(this.translate(SERVUX_KEY,
                                    this.getHudData().getServuxVersion()
            ));
        }
        else if (!this.getData().hasIntegratedServer() &&
                !this.getEntData().hasServuxServer() &&
                !this.getHudData().hasServuxServer())
        {
            list.add(this.translate(SERVUX_KEY+".not_connected"));
        }

        if (this.getEntData().hasServuxServer() ||
            this.getEntData().hasBackupStatus())
        {
            list.add(this.translate(SERVUX_KEY+".entity_sync",
                                    this.getEntData().getBlockEntityCacheCount(),
                                    this.getEntData().getPendingBlockEntitiesCount(),
                                    this.getEntData().getEntityCacheCount(),
                                    this.getEntData().getPendingEntitiesCount()
            ));
        }
        if (this.getData().hasServuxServer())
        {
            list.add(this.translate(SERVUX_KEY+".structures",
                                    this.getData().getStrucutreCount(),
                                    this.getHudData().getSpawnChunkRadius(),
                                    this.getHudData().getWorldSpawn().toShortString(),
                                    this.getHudData().isWorldSpawnKnown()
                                        ? this.qt(YES_KEY)
                                        : this.qt(NO_KEY)
            ));
        }
        else if (this.getHudData().hasServuxServer())
        {
            list.add(this.translate(SERVUX_KEY+".no_structures_hud",
                                    this.getHudData().getSpawnChunkRadius(),
                                    this.getHudData().getWorldSpawn().toShortString(),
                                    this.getHudData().isWorldSpawnKnown()
                                        ? this.qt(YES_KEY)
                                        : this.qt(NO_KEY)
            ));
        }
        else if (this.getData().hasIntegratedServer())
        {
            list.add(this.translate(SERVUX_KEY+".structures_integrated",
                                    this.getData().getStrucutreCount(),
                                    this.getHudData().getSpawnChunkRadius(),
                                    this.getHudData().getWorldSpawn().toShortString(),
                                    this.getHudData().isWorldSpawnKnown()
                                        ? this.qt(YES_KEY)
                                        : this.qt(NO_KEY)
            ));
        }

        return list;
    }
}
