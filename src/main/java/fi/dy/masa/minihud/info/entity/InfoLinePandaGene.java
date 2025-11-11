package fi.dy.masa.minihud.info.entity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import fi.dy.masa.malilib.util.nbt.NbtEntityUtils;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;

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
    public List<Entry> parse(@NotNull InfoLine.Context ctx)
    {
        if (ctx.world() == null) return null;

        if (ctx.hasLiving() && ctx.hasNbt())
        {
            EntityType<?> entityType = NbtEntityUtils.getEntityTypeFromNbt(ctx.nbt());
            if (entityType == null) return null;

            return this.parseNbt(ctx.world(), entityType, ctx.nbt());
        }

        return ctx.ent() != null ? this.parseEnt(ctx.world(), ctx.ent()) : null;
    }

    @Override
    public List<Entry> parseNbt(@NotNull Level world, @NotNull EntityType<?> entityType, @NotNull CompoundTag nbt)
    {
        List<Entry> list = new ArrayList<>();

        if (entityType.equals(EntityType.PANDA))
        {
            Pair<Panda.Gene, Panda.Gene> genes = NbtEntityUtils.getPandaGenesFromNbt(nbt);

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
