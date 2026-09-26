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

public record InfoLineContext(@Nullable Level world,
                              @Nullable Entity ent,
                              @Nullable BlockEntity be,
                              @Nullable BlockPos pos,
                              @Nullable BlockState state,
                              @Nullable ChunkPos chunkPos,
                              @Nullable CompoundData data)
{
	public boolean hasWorld()
	{
		return this.world != null && this.world instanceof Level;
	}

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

	public static class Builder
	{
		private @Nullable Level world = null;
		private @Nullable Entity ent = null;
		private @Nullable BlockEntity be = null;
		private @Nullable BlockPos pos = null;
		private @Nullable BlockState state = null;
		private @Nullable ChunkPos chunkPos = null;
		private @Nullable CompoundData data = null;

		public Builder world(@Nullable Level world)
		{
			this.world = world;
			return this;
		}

		public Builder ent(@Nullable Entity ent)
		{
			this.ent = ent;
			return this;
		}

		public Builder be(@Nullable BlockEntity be)
		{
			this.be = be;
			return this;
		}

		public Builder pos(@Nullable BlockPos pos)
		{
			this.pos = pos;
			return this;
		}

		public Builder state(@Nullable BlockState state)
		{
			this.state = state;
			return this;
		}

		public Builder chunkPos(@Nullable ChunkPos chunkPos)
		{
			this.chunkPos = chunkPos;
			return this;
		}

		public Builder data(@Nullable CompoundData data)
		{
			this.data = data;
			return this;
		}

		public InfoLineContext build()
		{
			return new InfoLineContext(this.world, this.ent, this.be, this.pos, this.state, this.chunkPos, this.data);
		}
	}
}
