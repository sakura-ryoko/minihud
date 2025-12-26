package fi.dy.masa.minihud.info.te;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import fi.dy.masa.malilib.util.data.DataBlockUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;
import fi.dy.masa.minihud.util.MiscUtils;

public class InfoLineFurnaceExp extends InfoLine
{
    private static final String FURNACE_KEY = Reference.MOD_ID+".info_line.furnace_xp";

    public InfoLineFurnaceExp(InfoToggle type)
    {
        super(type);
    }

    public InfoLineFurnaceExp()
    {
        this(InfoToggle.FURNACE_XP);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@Nonnull InfoLineContext ctx)
    {
        if (ctx.world() == null) return null;

        if (ctx.hasData())
        {
            BlockEntityType<?> beType = DataBlockUtils.getBlockEntityType(ctx.data());

            if (beType == null) return null;

            return this.parseData(ctx.world(), beType, ctx.data());
        }

        return ctx.be() != null ? this.parseBlockEnt(ctx.world(), ctx.be()) : null;
    }

    @Override
    public List<Entry> parseData(@Nonnull Level world, @Nonnull BlockEntityType<?> beType, @Nonnull CompoundData data)
    {
        List<Entry> list = new ArrayList<>();

        if (beType.equals(BlockEntityType.FURNACE) ||
            beType.equals(BlockEntityType.BLAST_FURNACE) ||
            beType.equals(BlockEntityType.SMOKER))
        {
            if (world instanceof ServerLevel serverWorld)
            {
                int exp = MiscUtils.getFurnaceXpAmount(serverWorld, data);

                if (exp > 0)
                {
                    list.add(this.translate(FURNACE_KEY, exp));
                }
            }
            else if (this.getHudData().hasServuxServer() && this.getHudData().hasRecipes())
            {
                int exp = MiscUtils.getFurnaceXpAmount(data);

                if (exp > 0)
                {
                    list.add(this.translate(FURNACE_KEY, exp));
                }
            }
        }

        return list;
    }

    @Override
    public List<Entry> parseBlockEnt(@Nonnull Level world, @Nonnull BlockEntity be)
    {
        List<Entry> list = new ArrayList<>();

        if (be instanceof AbstractFurnaceBlockEntity furnace)
        {
            if (world instanceof ServerLevel serverWorld)
            {
                int exp = MiscUtils.getFurnaceXpAmount(serverWorld, furnace);

                if (exp > 0)
                {
                    list.add(this.translate(FURNACE_KEY, exp));
                }
            }
            else if (this.getHudData().hasServuxServer() && this.getHudData().hasRecipes())
            {
                int exp = MiscUtils.getFurnaceXpAmount(furnace);

                if (exp > 0)
                {
                    list.add(this.translate(FURNACE_KEY, exp));
                }
            }
        }

        return list;
    }
}
