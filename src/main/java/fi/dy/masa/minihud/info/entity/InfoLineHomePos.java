package fi.dy.masa.minihud.info.entity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import fi.dy.masa.malilib.util.nbt.NbtEntityUtils;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;

public class InfoLineHomePos extends InfoLine
{
    private static final String HOME_KEY = Reference.MOD_ID+".info_line.home_pos";

    public InfoLineHomePos(InfoToggle type)
    {
        super(type);
    }

    public InfoLineHomePos()
    {
        super(InfoToggle.ENTITY_HOME_POS);
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
        Pair<BlockPos, Integer> pair = NbtEntityUtils.getHomePosFromNbt(nbt);

        if (pair.getLeft() != BlockPos.ZERO && pair.getRight() != -1)
        {
            list.add(this.translate(HOME_KEY,
                                    pair.getLeft().toShortString(),
                                    pair.getRight()
            ));
        }

        return list;
    }

    @Override
    public List<Entry> parseEnt(@NotNull Level world, @NotNull Entity ent)
    {
        List<Entry> list = new ArrayList<>();

        if (ent instanceof Mob mob && mob.hasHome())
        {
            list.add(this.translate(HOME_KEY,
                                    mob.getHomePosition().toShortString(),
                                    mob.getHomeRadius()
            ));
        }

        return list;
    }
}
