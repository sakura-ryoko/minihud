package fi.dy.masa.minihud.info.entity;

import java.util.Collection;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;

import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.nbt.NbtEntityUtils;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;

public class InfoLineLookingAtEffects extends InfoLine
{
    private static final String EFFECTS_KEY = Reference.MOD_ID+".info_line.looking_at_effects";

    public InfoLineLookingAtEffects(InfoToggle type)
    {
        super(type);
    }

    public InfoLineLookingAtEffects()
    {
        this(InfoToggle.LOOKING_AT_EFFECTS);
    }

    @Override
    public @Nullable Entry parse(@Nonnull InfoLine.Context ctx)
    {
        if (Configs.Generic.INFO_LINES_USES_NBT.getBooleanValue() &&
            ctx.hasLiving() && ctx.hasNbt())
        {
            EntityType<?> entityType = NbtEntityUtils.getEntityTypeFromNbt(ctx.nbt());
            if (entityType == null) return null;

            return this.parseNbt(ctx.world(), entityType, ctx.nbt());
        }

        return ctx.ent() != null ? this.parseEnt(ctx.world(), ctx.ent()) : null;
    }

    @Override
    public @Nullable Entry parseNbt(@Nonnull World world, @Nonnull EntityType<?> entityType, @Nonnull NbtCompound nbt)
    {
        Map<RegistryEntry<StatusEffect>, StatusEffectInstance> effects = NbtEntityUtils.getActiveStatusEffectsFromNbt(nbt, world.getRegistryManager());

        if (effects == null || effects.isEmpty())
        {
            return null;
        }

        for (RegistryEntry<StatusEffect> effectType : effects.keySet())
        {
            StatusEffectInstance effect = effects.get(effectType);

            if (effect.isInfinite() || effect.getDuration() > 0)
            {
                return this.translate(EFFECTS_KEY,
                                      effectType.value().getName().getString(),
                                      effect.getAmplifier() > 0 ? StringUtils.translate(EFFECTS_KEY+".amplifier", effect.getAmplifier() + 1) : "",
                                      effect.isInfinite() ? StringUtils.translate(EFFECTS_KEY+".infinite") :
                                      StringUtils.getDurationString((effect.getDuration() / 20) * 1000L),
                                      StringUtils.translate(REMAINING_KEY)
                );
            }
        }

        return null;
    }

    @Override
    public @Nullable Entry parseEnt(@Nonnull World world, @Nonnull Entity ent)
    {
        if (ent instanceof LivingEntity living)
        {
            Collection<StatusEffectInstance> effects = living.getStatusEffects();

            for (StatusEffectInstance effect : effects)
            {
                if (effect.isInfinite() || effect.getDuration() > 0)
                {
                    return this.translate(EFFECTS_KEY,
                                          effect.getEffectType().value().getName().getString(),
                                          effect.getAmplifier() > 0 ? StringUtils.translate(EFFECTS_KEY + ".amplifier", effect.getAmplifier() + 1) : "",
                                          effect.isInfinite() ? StringUtils.translate(EFFECTS_KEY + ".infinite") :
                                          StringUtils.getDurationString((effect.getDuration() / 20) * 1000L),
                                          StringUtils.translate(REMAINING_KEY)
                    );
                }
            }
        }

        return null;
    }
}
