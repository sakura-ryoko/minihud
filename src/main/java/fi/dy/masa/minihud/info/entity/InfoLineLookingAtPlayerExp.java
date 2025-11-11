package fi.dy.masa.minihud.info.entity;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Triple;
import fi.dy.masa.malilib.util.nbt.NbtEntityUtils;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;

public class InfoLineLookingAtPlayerExp extends InfoLine
{
    private static final String PLAYER_KEY = Reference.MOD_ID+".info_line.looking_at_player_exp";

    public InfoLineLookingAtPlayerExp(InfoToggle type)
    {
        super(type);
    }

    public InfoLineLookingAtPlayerExp()
    {
        this(InfoToggle.LOOKING_AT_PLAYER_EXP);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@Nonnull Context ctx)
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
        List<Entry> list = new ArrayList<>();

        if (entityType.equals(EntityType.PLAYER))
        {
            Triple<Integer, Integer, Float> triple = NbtEntityUtils.getPlayerExpFromNbt(nbt);

            if (triple.getLeft() > 0)
            {
                list.add(this.translate(PLAYER_KEY, triple.getLeft(), triple.getRight(), 100 * triple.getMiddle()));
            }
        }

        return list;
    }

    @Override
    public List<Entry> parseEnt(@Nonnull World world, @Nonnull Entity ent)
    {
        List<Entry> list = new ArrayList<>();

        if (ent instanceof ServerPlayerEntity player)
        {
            list.add(this.translate(PLAYER_KEY, player.experienceLevel, 100 * player.experienceProgress, player.totalExperience));
        }

        return list;
    }
}
