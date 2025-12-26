package fi.dy.masa.minihud.info.generic;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

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
    public List<Entry> parse(@Nonnull InfoLineContext ctx)
    {
        List<Entry> list = new ArrayList<>();

        if (!this.getData().hasIntegratedServer())
        {
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

            if (!Configs.Generic.HUD_DATA_SYNC.getBooleanValue())
            {
                list.add(this.translate(SERVUX_KEY + ".hud_sync.not_enabled"));
            }
            else if (!this.getHudData().hasServuxServer())
            {
                list.add(this.translate(SERVUX_KEY + ".hud_sync.not_connected"));
            }
            else if (this.getHudData().hasServuxServer())
            {
                list.add(this.translate(SERVUX_KEY+".hud_sync",
//                                    this.getHudData().getSpawnChunkRadius(),
                                        this.getHudData().getWorldSpawnAsString(),
                                        this.getHudData().hasStoredWorldSeed()
                                        ? this.qt(YES_KEY)
                                        : this.qt(NO_KEY)
                ));
            }

            if (!Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue() &&
                !Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue())
            {
                list.add(this.translate(SERVUX_KEY + ".entity_sync.not_enabled"));
            }
            else if (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue() &&
                     !this.getHudData().hasServuxServer())
            {
                list.add(this.translate(SERVUX_KEY + ".entity_sync.not_connected"));
            }
            else if (this.getEntData().hasServuxServer())
            {
                list.add(this.translate(SERVUX_KEY+".entity_sync",
                                        this.getEntData().getBlockEntityCacheCount(),
                                        this.getEntData().getPendingBlockEntitiesCount(),
                                        this.getEntData().getEntityCacheCount(),
                                        this.getEntData().getPendingEntitiesCount()
                ));
            }
            else if (this.getEntData().hasBackupStatus())
            {
                    list.add(this.translate(SERVUX_KEY + ".entity_sync.backup",
                                            this.getEntData().getBlockEntityCacheCount(),
                                            this.getEntData().getPendingBlockEntitiesCount(),
                                            this.getEntData().getEntityCacheCount(),
                                            this.getEntData().getPendingEntitiesCount()
                    ));
            }
            else if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue() &&
                     !this.getEntData().hasOperatorStatus())
            {
                list.add(this.translate(SERVUX_KEY + ".entity_sync.not_operator"));
            }

            if (!RendererToggle.OVERLAY_STRUCTURE_MAIN_TOGGLE.getBooleanValue())
            {
                list.add(this.translate(SERVUX_KEY + ".structures.not_enabled"));
            }
            else if (!this.getData().hasServuxServer())
            {
                list.add(this.translate(SERVUX_KEY + ".structures.not_connected"));
            }
            else if (this.getData().hasServuxServer())
            {
                list.add(this.translate(SERVUX_KEY+".structures.servux",
                                        this.getData().getStrucutreCount(),
                                        this.getData().getStructureDataMaxRange(),
                                        this.getData().getServerRenderDistance()
                ));
            }
        }
        else if (this.getData().hasIntegratedServer())
        {
            list.add(this.translate(SERVUX_KEY + ".hud_sync.integrated",
//                                    this.getHudData().getSpawnChunkRadius(),
                                    this.getHudData().getWorldSpawnAsString(),
                                    this.getHudData().hasStoredWorldSeed()
                                    ? this.qt(YES_KEY)
                                    : this.qt(NO_KEY)
            ));

//            list.add(this.translate(SERVUX_KEY+".entity_sync.integrated",
//                                    this.getEntData().getBlockEntityCacheCount(),
//                                    this.getEntData().getPendingBlockEntitiesCount(),
//                                    this.getEntData().getEntityCacheCount(),
//                                    this.getEntData().getPendingEntitiesCount()
//            ));

            if (RendererToggle.OVERLAY_STRUCTURE_MAIN_TOGGLE.getBooleanValue())
            {
                list.add(this.translate(SERVUX_KEY + ".structures.integrated",
                                        this.getData().getStrucutreCount(),
                                        this.getData().getStructureDataMaxRange(),
                                        this.getData().getServerRenderDistance()
                ));
            }
        }

        return list;
    }
}
