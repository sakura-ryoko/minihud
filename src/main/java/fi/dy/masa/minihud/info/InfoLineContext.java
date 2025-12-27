package fi.dy.masa.minihud.info;

import javax.annotation.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

public record InfoLineContext(@Nullable World world, @Nullable Entity ent, @Nullable BlockEntity be, @Nullable BlockPos pos, @Nullable BlockState state, ChunkPos chunkPos, NbtCompound nbt)
{
	public boolean hasEntity()
	{
		return this.ent != null && this.ent instanceof Entity;
	}

	public boolean hasLiving()
	{
		return this.ent != null && this.ent instanceof LivingEntity;
	}

	public @Nullable LivingEntity living()
	{
		if (this.hasLiving())
		{
			return (LivingEntity) this.ent;
		}

		return null;
	}

	public boolean hasBlockEntity()
	{
		return this.be != null && this.be instanceof BlockEntity;
	}

	public boolean hasBlockPos()
	{
		return this.pos != null;
	}

	public boolean hasBlockState()
	{
		return this.state != null && this.state instanceof BlockState;
	}

	public boolean hasChunkPos()
	{
		return this.chunkPos != null;
	}

	public boolean hasNbt()
	{
		return this.nbt != null && !this.nbt.isEmpty();
	}
}
