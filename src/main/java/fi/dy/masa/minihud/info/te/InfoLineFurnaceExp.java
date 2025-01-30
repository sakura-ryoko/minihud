package fi.dy.masa.minihud.info.te;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import fi.dy.masa.malilib.util.nbt.NbtBlockUtils;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
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
    public @Nullable Entry parse(@Nonnull Context ctx)
    {
        if (Configs.Generic.INFO_LINES_USES_NBT.getBooleanValue() &&
            ctx.hasNbt())
        {
            BlockEntityType<?> beType = NbtBlockUtils.getBlockEntityTypeFromNbt(ctx.nbt());

            if (beType == null) return null;

            return this.parseNbt(ctx.world(), beType, ctx.nbt());
        }

        return ctx.be() != null ? this.parseBlockEnt(ctx.world(), ctx.be()) : null;
    }

    @Override
    public @Nullable Entry parseNbt(@Nonnull World world, @Nonnull BlockEntityType<?> beType, @Nonnull NbtCompound nbt)
    {
        if (beType.equals(BlockEntityType.FURNACE) ||
            beType.equals(BlockEntityType.BLAST_FURNACE) ||
            beType.equals(BlockEntityType.SMOKER))
        {
            if (world instanceof ServerWorld serverWorld)
            {
                int exp = MiscUtils.getFurnaceXpAmount(serverWorld, nbt);

                if (exp > 0)
                {
                    return this.translate(FURNACE_KEY, exp);
                }
            }
            else if (this.getHudData().hasServuxServer() && this.getHudData().hasRecipes())
            {
                int exp = MiscUtils.getFurnaceXpAmount(nbt);

                if (exp > 0)
                {
                    return this.translate(FURNACE_KEY, exp);
                }
            }
        }

        return null;
    }

    @Override
    public @Nullable Entry parseBlockEnt(@Nonnull World world, @Nonnull BlockEntity be)
    {
        if (be instanceof AbstractFurnaceBlockEntity furnace)
        {
            if (world instanceof ServerWorld serverWorld)
            {
                int exp = MiscUtils.getFurnaceXpAmount(serverWorld, furnace);

                if (exp > 0)
                {
                    return this.translate(FURNACE_KEY, exp);
                }
            }
            else if (this.getHudData().hasServuxServer() && this.getHudData().hasRecipes())
            {
                int exp = MiscUtils.getFurnaceXpAmount(furnace);

                if (exp > 0)
                {
                    return this.translate(FURNACE_KEY, exp);
                }
            }
        }

        return null;
    }
}
