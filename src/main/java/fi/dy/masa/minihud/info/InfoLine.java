package fi.dy.masa.minihud.info;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class InfoLine
{
    public @Nullable Text parse(@Nonnull Context ctx)
    {
        return null;
    }

    public record Context(@Nonnull World world, @Nullable Entity ent, @Nullable BlockEntity be, @Nullable Block block, NbtCompound nbt)
    {
        public boolean hasLivingEntity()
        {
            return this.ent != null && this.ent instanceof LivingEntity;
        }

        public boolean hasBlockEntity()
        {
            return this.be != null && this.be instanceof BlockEntity;
        }

        public boolean hasBlock()
        {
            return this.block != null && this.block instanceof Block;
        }

        public boolean hasNbt()
        {
            return this.nbt != null && !this.nbt.isEmpty();
        }
    }

    public record Text(@Nonnull String format, @Nullable Object... args)
    {
    }
}
