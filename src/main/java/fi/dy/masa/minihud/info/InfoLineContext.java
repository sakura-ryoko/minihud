package fi.dy.masa.minihud.info;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import fi.dy.masa.malilib.util.data.tag.CompoundData;

public record InfoLineContext(@Nullable Level world, @Nullable Entity ent, @Nullable BlockEntity be, @Nullable BlockPos pos, @Nullable BlockState state, ChunkPos chunkPos, CompoundData data)
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

	public boolean hasData()
	{
		return this.data != null && !this.data.isEmpty();
	}
}
