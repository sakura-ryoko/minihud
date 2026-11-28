package fi.dy.masa.minihud.util;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public abstract class LightLevelScanComponentBase
{
    protected final int centerX, centerY, centerZ;
    protected final boolean aggressiveOpt;

    public LightLevelScanComponentBase(int centerX, int centerY, int centerZ, boolean aggressiveOpt)
    {
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.aggressiveOpt = aggressiveOpt;
    }

    public interface IChunkPosPredicate
    {
        IChunkPosPredicate ALWAYS_FALSE = new IChunkPosPredicate()
        {
            @Override
            public boolean testChunk(int chunkX, int chunkZ)
            {
                return false;
            }

            @Override
            public IChunkPosPredicate and(IChunkPosPredicate next)
            {
                return ALWAYS_FALSE;
            }
        };

        boolean testChunk(int chunkX, int chunkZ);

        default IChunkPosPredicate chainChunkPredicate(@Nullable IChunkPosPredicate prev)
        {
            return prev == null ? this : prev.and(this);
        }

        default IChunkPosPredicate and(IChunkPosPredicate next)
        {
            return next == ALWAYS_FALSE ? ALWAYS_FALSE : (cx, cz) -> this.testChunk(cx, cz) && next.testChunk(cx, cz);
        }
    }

    public interface IBlockPosPredicate
    {
        IBlockPosPredicate ALWAYS_FALSE = new IBlockPosPredicate()
        {
            @Override
            public boolean testBlock(int x, int y, int z)
            {
                return false;
            }

            @Override
            public IBlockPosPredicate and(IBlockPosPredicate next)
            {
                return ALWAYS_FALSE;
            }
        };

        boolean testBlock(int x, int y, int z);

        default IBlockPosPredicate chainBlockPredicate(int chunkX, int chunkZ, @Nullable IBlockPosPredicate prev)
        {
            return prev == null ? this : prev.and(this);
        }

        default IBlockPosPredicate and(IBlockPosPredicate next)
        {
            return next == ALWAYS_FALSE ? ALWAYS_FALSE : (x, y, z) -> this.testBlock(x, y, z) && next.testBlock(x, y, z);
        }
    }

    public interface IFullLayerPredicate
    {
        IFullLayerPredicate ALWAYS_FALSE = new IFullLayerPredicate()
        {
            @Override
            public boolean testFullLayer(int chunkX, int chunkZ, int y)
            {
                return false;
            }

            @Override
            public IFullLayerPredicate and(IFullLayerPredicate next)
            {
                return ALWAYS_FALSE;
            }
        };

        boolean testFullLayer(int chunkX, int chunkZ, int y);

        default IFullLayerPredicate chainLayerPredicate(int chunkX, int chunkZ, @Nullable IFullLayerPredicate prev)
        {
            return prev == null ? this : prev.and(this);
        }

        default IFullLayerPredicate and(IFullLayerPredicate next)
        {
            return next == ALWAYS_FALSE ? ALWAYS_FALSE : (chunkX, chunkZ, y) -> this.testFullLayer(chunkX, chunkZ, y) && next.testFullLayer(chunkX, chunkZ, y);
        }
    }

    public interface IChunkPosConsumer
    {
        void accept(int chunkX, int chunkZ);
    }

    public interface IBlockConsumer
    {
        void accept(int x, int y, int z, BlockPos pos, BlockState state);
    }
}
