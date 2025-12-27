package fi.dy.masa.minihud.info.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;

import fi.dy.masa.malilib.util.nbt.NbtEntityUtils;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;
import fi.dy.masa.minihud.util.MiscUtils;

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
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@Nonnull InfoLineContext ctx)
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
    public List<Entry> parseNbt(@Nonnull World world, @Nonnull EntityType<?> entityType, @Nonnull NbtCompound nbt)
    {
        Map<RegistryEntry<StatusEffect>, StatusEffectInstance> effects = NbtEntityUtils.getActiveStatusEffectsFromNbt(nbt);
        List<Entry> list = new ArrayList<>();

        if (effects == null || effects.isEmpty())
        {
            return list;
        }

        for (RegistryEntry<StatusEffect> effectType : effects.keySet())
        {
            StatusEffectInstance effect = effects.get(effectType);

            if (effect.isInfinite() || effect.getDuration() > 0)
            {
                list.add(this.translate(EFFECTS_KEY,
                                        effectType.value().getName().getString(),
                                        effect.getAmplifier() > 0 ? this.qt(EFFECTS_KEY+".amplifier", effect.getAmplifier() + 1) : "",
                                        effect.isInfinite() ? this.qt(EFFECTS_KEY+".infinite") :
                                        MiscUtils.formatDuration((effect.getDuration() / 20) * 1000L),
                                        this.qt(REMAINING_KEY)
                ));
            }
        }

        return list;
    }

    @Override
    public List<Entry> parseEnt(@Nonnull World world, @Nonnull Entity ent)
    {
        List<Entry> list = new ArrayList<>();

        if (ent instanceof LivingEntity living)
        {
            Collection<StatusEffectInstance> effects = living.getStatusEffects();

            for (StatusEffectInstance effect : effects)
            {
                if (effect.isInfinite() || effect.getDuration() > 0)
                {
                    list.add(this.translate(EFFECTS_KEY,
                                            effect.getEffectType().value().getName().getString(),
                                            effect.getAmplifier() > 0 ? this.qt(EFFECTS_KEY + ".amplifier", effect.getAmplifier() + 1) : "",
                                            effect.isInfinite() ? this.qt(EFFECTS_KEY + ".infinite") :
                                            MiscUtils.formatDuration((effect.getDuration() / 20) * 1000L),
                                            this.qt(REMAINING_KEY)
                    ));
                }
            }
        }

        return list;
    }
}
