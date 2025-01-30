package fi.dy.masa.minihud.info.entity;

import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.util.Util;

import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.nbt.NbtEntityUtils;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.mixin.entity.IMixinPassiveEntity;

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
    public @Nullable Entry parse(@Nonnull Context ctx)
    {
        if (Configs.Generic.INFO_LINES_USES_NBT.getBooleanValue() &&
            ctx.ent() instanceof LivingEntity living && ctx.hasNbt())
        {
            Pair<Double, Double> healthPair = NbtEntityUtils.getHealthFromNbt(ctx.nbt());
            Pair<UUID, Boolean> ownerPair = NbtEntityUtils.getTamableOwner(ctx.nbt());
            Pair<Integer, Integer> agePair = NbtEntityUtils.getAgeFromNbt(ctx.nbt());

            double health = healthPair.getLeft();
            double maxHealth = healthPair.getRight();

            // Update the Health, as it might not be timely otherwise.
            if (living.getHealth() != health)
            {
                health = living.getHealth();
            }

            String entityLine = StringUtils.translate(LOOKING_KEY+".livingentity", living.getName().getString(), health, maxHealth);

            if (ownerPair.getLeft() != Util.NIL_UUID)
            {
                LivingEntity owner = ctx.world().getPlayerByUuid(ownerPair.getLeft());

                if (owner != null)
                {
                    entityLine = entityLine + " - " + StringUtils.translate(LOOKING_KEY+".owner") + ": " + owner.getName().getLiteralString();
                }
            }
            if (agePair.getLeft() < 0)
            {
                int untilGrown = agePair.getLeft() * (-1);
                entityLine = entityLine+ " [" + StringUtils.getDurationString(untilGrown * 50L) + " " + StringUtils.translate(REMAINING_KEY) + "]";
            }

            return this.format(entityLine);
        }
        else if (ctx.ent() instanceof LivingEntity living)
        {
            String entityLine = StringUtils.translate(LOOKING_KEY+".livingentity", living.getName().getString(), living.getHealth(), living.getMaxHealth());

            if (living instanceof Tameable tamable)
            {
                LivingEntity owner = tamable.getOwner();

                if (owner != null)
                {
                    entityLine = entityLine + " - " + StringUtils.translate(LOOKING_KEY+".owner") + ": " + owner.getName().getLiteralString();
                }
            }
            if (living instanceof PassiveEntity passive)
            {
                if (passive.getBreedingAge() < 0)
                {
                    int untilGrown = ((IMixinPassiveEntity) passive).minihud_getRealBreedingAge() * (-1);
                    entityLine = entityLine+ " [" + StringUtils.getDurationString(untilGrown * 50L) + " " + StringUtils.translate(REMAINING_KEY) + "]";
                }
            }

            return this.format(entityLine);
        }
        else if (ctx.ent() instanceof Entity ent)
        {
            return this.translate(LOOKING_KEY, ent.getName().getString());
        }

        return null;
    }
}
