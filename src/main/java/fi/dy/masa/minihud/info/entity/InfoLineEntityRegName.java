package fi.dy.masa.minihud.info.entity;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import fi.dy.masa.malilib.util.nbt.NbtEntityUtils;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

public class InfoLineEntityRegName extends InfoLine
{
    private static final String ENTITY_KEY = Reference.MOD_ID+".info_line.entity_reg_name";

    public InfoLineEntityRegName(InfoToggle type)
    {
        super(type);
    }

    public InfoLineEntityRegName()
    {
        super(InfoToggle.ENTITY_REG_NAME);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@NotNull InfoLineContext ctx)
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
    public List<Entry> parseNbt(@NotNull World world, @NotNull EntityType<?> entityType, @NotNull NbtCompound nbt)
    {
        List<Entry> list = new ArrayList<>();
        Identifier regName = EntityType.getId(entityType);

        list.add(this.translate(ENTITY_KEY, regName));

        return list;
    }

    @Override
    public List<Entry> parseEnt(@NotNull World world, @NotNull Entity ent)
    {
        List<Entry> list = new ArrayList<>();

        Identifier regName = EntityType.getId(ent.getType());

        list.add(this.translate(ENTITY_KEY, regName));

        return list;
    }
}
