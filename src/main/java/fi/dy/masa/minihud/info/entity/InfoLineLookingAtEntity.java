package fi.dy.masa.minihud.info.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraft.util.Util;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import fi.dy.masa.malilib.util.data.DataEntityUtils;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;
import fi.dy.masa.minihud.mixin.entity.IMixinPassiveEntity;
import fi.dy.masa.minihud.util.MiscUtils;

public class InfoLineLookingAtEntity extends InfoLine
{
    private static final String LOOKING_KEY = Reference.MOD_ID+".info_line.looking_at_entity";

    public InfoLineLookingAtEntity(InfoToggle type)
    {
        super(type);
    }

    public InfoLineLookingAtEntity()
    {
        this(InfoToggle.LOOKING_AT_ENTITY);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@Nonnull InfoLineContext ctx)
    {
        if (ctx.world() == null) return null;
        List<Entry> list = new ArrayList<>();

        if (ctx.ent() instanceof LivingEntity living && ctx.hasData())
        {
            Pair<Double, Double> healthPair = DataEntityUtils.getHealth(ctx.data());
            Pair<UUID, Boolean> ownerPair = DataEntityUtils.getTamableOwner(ctx.data());
            Pair<Integer, Integer> agePair = DataEntityUtils.getAge(ctx.data());

            double health = healthPair.getLeft();
            double maxHealth = healthPair.getRight();

            // Update the Health, as it might not be timely otherwise.
            if (living.getHealth() != health)
            {
                health = living.getHealth();
            }

            String entityLine = this.qt(LOOKING_KEY+".livingentity", living.getName().getString(), health, maxHealth);

            if (ownerPair.getLeft() != Util.NIL_UUID)
            {
                LivingEntity owner = ctx.world().getPlayerByUUID(ownerPair.getLeft());

                if (owner != null)
                {
                    entityLine = entityLine + " - " + this.qt(LOOKING_KEY+".owner") + ": " + owner.getName().tryCollapseToString();
                }
            }
            if (agePair.getLeft() < 0)
            {
                int untilGrown = agePair.getLeft() * (-1);
                entityLine = entityLine+ " [" + MiscUtils.formatDuration(untilGrown * 50L) + " " + this.qt(REMAINING_KEY) + "]";
            }

            list.add(this.format(entityLine));
        }
        else if (ctx.ent() instanceof LivingEntity living)
        {
            String entityLine = this.qt(LOOKING_KEY+".livingentity", living.getName().getString(), living.getHealth(), living.getMaxHealth());

            if (living instanceof OwnableEntity tamable)
            {
                LivingEntity owner = tamable.getOwner();

                if (owner != null)
                {
                    entityLine = entityLine + " - " + this.qt(LOOKING_KEY+".owner") + ": " + owner.getName().tryCollapseToString();
                }
            }
            if (living instanceof AgeableMob passive)
            {
                if (passive.getAge() < 0)
                {
                    int untilGrown = ((IMixinPassiveEntity) passive).minihud_getRealBreedingAge() * (-1);
                    entityLine = entityLine+ " [" + MiscUtils.formatDuration(untilGrown * 50L) + " " + this.qt(REMAINING_KEY) + "]";
                }
            }

            list.add(this.format(entityLine));
        }
        else if (ctx.ent() instanceof Entity ent)
        {
            list.add(this.translate(LOOKING_KEY, ent.getName().getString()));
        }

        return list;
    }
}
