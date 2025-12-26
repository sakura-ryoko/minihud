package fi.dy.masa.minihud.info.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import fi.dy.masa.malilib.util.data.DataEntityUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
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

        if (ctx.hasLiving() && ctx.hasData())
        {
            EntityType<?> entityType = DataEntityUtils.getEntityType(ctx.data());
            if (entityType == null) return null;

            return this.parseData(ctx.world(), entityType, ctx.data());
        }

        return ctx.ent() != null ? this.parseEnt(ctx.world(), ctx.ent()) : null;
    }

    @Override
    public List<Entry> parseData(@Nonnull Level world, @Nonnull EntityType<?> entityType, @Nonnull CompoundData data)
    {
        Map<Holder<MobEffect>, MobEffectInstance> effects = DataEntityUtils.getActiveStatusEffects(data, world.registryAccess());
        List<Entry> list = new ArrayList<>();

        if (effects == null || effects.isEmpty())
        {
            return list;
        }

        for (Holder<MobEffect> effectType : effects.keySet())
        {
            MobEffectInstance effect = effects.get(effectType);

            if (effect.isInfiniteDuration() || effect.getDuration() > 0)
            {
                list.add(this.translate(EFFECTS_KEY,
                                        effectType.value().getDisplayName().getString(),
                                        effect.getAmplifier() > 0 ? this.qt(EFFECTS_KEY+".amplifier", effect.getAmplifier() + 1) : "",
                                        effect.isInfiniteDuration() ? this.qt(EFFECTS_KEY+".infinite") :
                                        MiscUtils.formatDuration((effect.getDuration() / 20) * 1000L),
                                        this.qt(REMAINING_KEY)
                ));
            }
        }

        return list;
    }

    @Override
    public List<Entry> parseEnt(@Nonnull Level world, @Nonnull Entity ent)
    {
        List<Entry> list = new ArrayList<>();

        if (ent instanceof LivingEntity living)
        {
            Collection<MobEffectInstance> effects = living.getActiveEffects();

            for (MobEffectInstance effect : effects)
            {
                if (effect.isInfiniteDuration() || effect.getDuration() > 0)
                {
                    list.add(this.translate(EFFECTS_KEY,
                                            effect.getEffect().value().getDisplayName().getString(),
                                            effect.getAmplifier() > 0 ? this.qt(EFFECTS_KEY + ".amplifier", effect.getAmplifier() + 1) : "",
                                            effect.isInfiniteDuration() ? this.qt(EFFECTS_KEY + ".infinite") :
                                            MiscUtils.formatDuration((effect.getDuration() / 20) * 1000L),
                                            this.qt(REMAINING_KEY)
                    ));
                }
            }
        }

        return list;
    }
}
