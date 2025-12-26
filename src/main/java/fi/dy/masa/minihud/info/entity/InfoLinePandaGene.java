package fi.dy.masa.minihud.info.entity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import fi.dy.masa.malilib.util.data.DataEntityUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

public class InfoLinePandaGene extends InfoLine
{
    private static final String PANDA_KEY = Reference.MOD_ID+".info_line.panda_gene";

    public InfoLinePandaGene(InfoToggle type)
    {
        super(type);
    }

    public InfoLinePandaGene()
    {
        super(InfoToggle.PANDA_GENE);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@NotNull InfoLineContext ctx)
    {
        if (ctx.world() == null) return null;

        if (ctx.hasLiving() && ctx.hasData())
        {
            EntityType<?> entityType = DataEntityUtils.getEntityType(ctx.data());
            if (entityType == null) return null;

            return this.parseData(ctx.world(), entityType, ctx.data());
        }

        return ctx.ent() != null ? this.parseEnt(ctx.world(), ctx.ent()) : null;
    }

    @Override
    public List<Entry> parseData(@NotNull Level world, @NotNull EntityType<?> entityType, @NotNull CompoundData data)
    {
        List<Entry> list = new ArrayList<>();

        if (entityType.equals(EntityType.PANDA))
        {
            Pair<Panda.Gene, Panda.Gene> genes = DataEntityUtils.getPandaGenes(data);

            if (genes.getLeft() != null && genes.getRight() != null)
            {
                list.add(this.translate(PANDA_KEY+".main_gene",
                                        this.qt(PANDA_KEY+".gene." + genes.getLeft().getSerializedName()),
                                        genes.getLeft().isRecessive() ? this.qt(PANDA_KEY+".recessive_gene") : this.qt(PANDA_KEY+".dominant_gene")
                ));
                list.add(this.translate(PANDA_KEY+".hidden_gene",
                                        this.qt(PANDA_KEY+".gene." + genes.getRight().getSerializedName()),
                                        genes.getRight().isRecessive() ? this.qt(PANDA_KEY+".recessive_gene") : this.qt(PANDA_KEY+".dominant_gene")
                ));
            }
        }

        return list;
    }

    @Override
    public List<Entry> parseEnt(@NotNull Level world, @NotNull Entity ent)
    {
        List<Entry> list = new ArrayList<>();

        if (ent instanceof Panda panda)
        {
            list.add(this.translate(PANDA_KEY+".main_gene",
                                    this.qt(PANDA_KEY+".gene." + panda.getMainGene().getSerializedName()),
                                    panda.getMainGene().isRecessive() ? this.qt(PANDA_KEY+".recessive_gene") : this.qt(PANDA_KEY+".dominant_gene")
            ));
            list.add(this.translate(PANDA_KEY+".hidden_gene",
                                    this.qt(PANDA_KEY+".gene." + panda.getHiddenGene().getSerializedName()),
                                    panda.getHiddenGene().isRecessive() ? this.qt(PANDA_KEY+".recessive_gene") : this.qt(PANDA_KEY+".dominant_gene")
            ));
        }

        return list;
    }
}
