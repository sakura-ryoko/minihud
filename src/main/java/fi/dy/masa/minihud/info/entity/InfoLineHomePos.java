package fi.dy.masa.minihud.info.entity;

import java.util.List;
import org.jetbrains.annotations.NotNull;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

// todo 1.21.8+
public class InfoLineHomePos extends InfoLine
{
    private static final String HOME_KEY = Reference.MOD_ID+".info_line.home_pos";

    public InfoLineHomePos(InfoToggle type)
    {
        super(type);
    }

//    public InfoLineHomePos()
//    {
//        super(InfoToggle.ENTITY_HOME_POS);
//    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@NotNull InfoLineContext ctx)
    {
//        if (ctx.world() == null) return null;
//
//        if (ctx.hasLiving() && ctx.hasNbt())
//        {
//            EntityType<?> entityType = NbtEntityUtils.getEntityTypeFromNbt(ctx.nbt());
//            if (entityType == null) return null;
//
//            return this.parseNbt(ctx.world(), entityType, ctx.nbt());
//        }
//
//        return ctx.ent() != null ? this.parseEnt(ctx.world(), ctx.ent()) : null;
        return null;
    }

//    @Override
//    public List<Entry> parseNbt(@NotNull World world, @NotNull EntityType<?> entityType, @NotNull NbtCompound nbt)
//    {
//        List<Entry> list = new ArrayList<>();
//        Pair<BlockPos, Integer> pair = NbtEntityUtils.getHomePosFromNbt(nbt);
//
//        if (pair.getLeft() != BlockPos.ORIGIN && pair.getRight() != -1)
//        {
//            list.add(this.translate(HOME_KEY,
//                                    pair.getLeft().toShortString(),
//                                    pair.getRight()
//            ));
//        }
//
//        return list;
//    }
//
//    @Override
//    public List<Entry> parseEnt(@NotNull World world, @NotNull Entity ent)
//    {
//        List<Entry> list = new ArrayList<>();
//
//        if (ent instanceof MobEntity mob && mob.hasPositionTarget())
//        {
//            list.add(this.translate(HOME_KEY,
//                                    mob.getPositionTarget().toShortString(),
//                                    mob.getPositionTargetRange()
//            ));
//        }
//
//        return list;
//    }
}
