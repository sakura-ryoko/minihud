package fi.dy.masa.minihud.info.entity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import fi.dy.masa.malilib.util.data.DataEntityUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
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

        if (ctx.hasLiving() && ctx.hasData())
        {
            EntityType<?> entityType = DataEntityUtils.getEntityType(ctx.data());
            if (entityType == null) return null;

            return this.parseData(ctx.world(), entityType, ctx.data());
        }

        return ctx.ent() != null ? this.parseEnt(ctx.world(), ctx.ent()) : null;
    }

    @Override
    public List<Entry> parseData(@NotNull Level world, @NotNull EntityType<?> entityType, @NotNull CompoundData data)
    {
        List<Entry> list = new ArrayList<>();
        Identifier regName = EntityType.getKey(entityType);

        list.add(this.translate(ENTITY_KEY, regName));

        return list;
    }

    @Override
    public List<Entry> parseEnt(@NotNull Level world, @NotNull Entity ent)
    {
        List<Entry> list = new ArrayList<>();

        Identifier regName = EntityType.getKey(ent.getType());

        list.add(this.translate(ENTITY_KEY, regName));

        return list;
    }
}
