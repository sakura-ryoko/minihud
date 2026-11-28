package fi.dy.masa.minihud.util;

import javax.annotation.Nullable;
import java.util.ArrayList;

import net.minecraft.world.level.Level;

import fi.dy.masa.minihud.data.HudDataManager;
import fi.dy.masa.minihud.renderer.shapes.ShapeSpawnSphere;
import fi.dy.masa.minihud.util.LightLevelScanComponentBase.*;

// Invocation order: testChunk, getMinY, getMaxY, testFullLayer*, xzMask, testBlock.
// Latter can ignore conditions that previous filters already ruled out, except testFullLayer.
// xzMask and testBlock may still be called even if testFullLayer returns true,
//  because rebuilding the predicates each layer seems unnecessary,
//  and testFullLayer may get ignored by iterator that is fast enough.
public abstract class LightLevelPosLimiter extends LightLevelScanComponentBase implements IChunkPosPredicate, IFullLayerPredicate, IBlockPosPredicate
{
    private long cacheKey = -1;
    protected int minYCache, maxYCache, xzMaskCache;

    /**
     * x and z in a chunk has 16 possible values each, and integer has 32 bits, how convenient!
     * @return an xzMask for quickly stacking together and checking x and z coordinates in a chunk.
     */
    @SuppressWarnings("CommentedOutCode")
    public static int createXZMask(int minX, int minZ, int maxX, int maxZ)
    {
        int mask = 0;
        // base logic:
//        for (int i = 0; i < 16; ++i)
//        {
//            if (i < minX || i > maxX)
//            {
//                mask |= 1 << 16 << i;
//            }
//            if (i < minZ || i > maxZ)
//            {
//                mask |= 1 << i;
//            }
//        }
        if (minX > 0)
        {
            if (minX > 15)
            {
                return -1;
            }
            mask |= ~(-1 << minX) << 16;
        }
        if (maxX < 15)
        {
            if (maxX < 0)
            {
                return -1;
            }
            mask |= (-2 << maxX) << 16;
        }
        if (minZ > 0)
        {
            if (minZ > 15)
            {
                return -1;
            }
            mask |= ~(-1 << minZ) & 0xFFFF;
        }
        if (maxZ < 15)
        {
            if (maxZ < 0)
            {
                return -1;
            }
            mask |= (-2 << maxZ) & 0xFFFF;
        }

        if ((mask >> 16) == -1 || (mask << 16 >> 16) == -1) // either x or z are all invalid
        {
            return -1;
        }

        return mask;
    }

    public static int createXZMask(int chunkX, int chunkZ, int minX, int minZ, int maxX, int maxZ)
    {
        int x = chunkX << 4;
        int z = chunkZ << 4;
        return createXZMask(minX - x, minZ - z, maxX - x, maxZ - z);
    }

    public static boolean checkXZ(int xzMask, int x, int z)
    {
        return (xzMask & ((1 << 16 << (x & 0xF)) | (1 << (z & 0xF)))) == 0;
    }

    public static int getMinX(int xzMask)
    {
        return Integer.numberOfTrailingZeros((~xzMask >>> 16) | (1 << 16));
    }

    public static int getMinZ(int xzMask)
    {
        return Integer.numberOfTrailingZeros(~xzMask | (1 << 16));
    }

    public static int getMaxX(int xzMask)
    {
        return 15 - Integer.numberOfLeadingZeros(~xzMask | (1 << 15));
    }

    public static int getMaxZ(int xzMask)
    {
        return 15 - Integer.numberOfLeadingZeros((~xzMask << 16) | (1 << 15));
    }


    public LightLevelPosLimiter(int centerX, int centerY, int centerZ, boolean aggressiveOpt)
    {
        super(centerX, centerY, centerZ, aggressiveOpt);
    }

    protected boolean shouldUpdateCache(int chunkX, int chunkZ)
    {
        long key = ((long) chunkX << 32L) | ((long) chunkZ & ((1L << 32L) - 1L));
        if (this.cacheKey == key)
        {
            return false;
        }
        this.cacheKey = key;
        return true;
    }

    /**
     * Should update minYCache, maxYCache, xzMaskCache when called, or override the respective getter methods.
     */
    protected abstract void updateCache(int chunkX, int chunkZ);

    public int getXZMask(int chunkX, int chunkZ)
    {
        this.updateCache(chunkX, chunkZ);
        return this.xzMaskCache;
    }

    public int getMinY(int chunkX, int chunkZ)
    {
        this.updateCache(chunkX, chunkZ);
        return this.minYCache;
    }

    public int getMaxY(int chunkX, int chunkZ)
    {
        this.updateCache(chunkX, chunkZ);
        return this.maxYCache;
    }

    // if all 256 blocks are guaranteed to be true
    @Override
    public boolean testFullLayer(int chunkX, int chunkZ, int y)
    {
        return true;
    }

    public boolean isSame(LightLevelPosLimiter last)
    {
        return this.getClass() == last.getClass();
    }

    public static class SpawnSphere extends LightLevelPosLimiter
    {
        private final java.util.List<ShapeSpawnSphere> spawnSpheres;
        private java.util.List<ShapeSpawnSphere> filteredSpawnSpheres = java.util.List.of();

        public SpawnSphere(int centerX, int centerY, int centerZ, boolean aggressiveOpt, java.util.List<ShapeSpawnSphere> spawnSpheres)
        {
            super(centerX, centerY, centerZ, aggressiveOpt);
            if (spawnSpheres.isEmpty())
            {
                throw new IllegalArgumentException("spawnSpheres must not be empty");
            }
            this.spawnSpheres = spawnSpheres;
        }

        @Override
        public boolean testFullLayer(int chunkX, int chunkZ, int y)
        {
            return this.aggressiveOpt && this.filteredSpawnSpheres.stream()
                    .anyMatch(s -> s.containsChunkLayer(chunkX, chunkZ, y));
        }

        @Override
        public boolean testChunk(int chunkX, int chunkZ)
        {
            this.updateCache(chunkX, chunkZ);
            return this.filteredSpawnSpheres.isEmpty() == false;
        }

        @Override
        public boolean testBlock(int x, int y, int z)
        {
            this.updateCache(x >> 4, z >> 4);
            return this.filteredSpawnSpheres.stream().anyMatch(s -> s.contains(x, y, z));
        }

        @Override
        protected void updateCache(int chunkX, int chunkZ)
        {
            if (this.shouldUpdateCache(chunkX, chunkZ) == false)
            {
                return;
            }

            this.filteredSpawnSpheres = this.spawnSpheres.stream().filter(s -> s.mayIntersectWithChunk(chunkX, chunkZ)).toList();

            int minY = Integer.MAX_VALUE;
            int maxY = Integer.MIN_VALUE;
            int xzMask = -1;
            for (ShapeSpawnSphere s : this.filteredSpawnSpheres)
            {
                minY = Math.min(minY, s.getChunkIntersectMinY(chunkX, chunkZ));
                maxY = Math.max(maxY, s.getChunkIntersectMaxY(chunkX, chunkZ));
                xzMask &= s.getChunkIntersectXZMask(chunkX, chunkZ);
            }
            this.minYCache = minY;
            this.maxYCache = maxY;
            this.xzMaskCache = xzMask;
        }

        @Override
        public IFullLayerPredicate chainLayerPredicate(int chunkX, int chunkZ, @Nullable IFullLayerPredicate prev)
        {
            if (this.testChunk(chunkX, chunkZ) == false)
            {
                return IFullLayerPredicate.ALWAYS_FALSE;
            }

            IFullLayerPredicate orChain = null;
            for (ShapeSpawnSphere s : this.filteredSpawnSpheres)
            {
                IFullLayerPredicate last = orChain;
                orChain = last == null ? s::containsChunkLayer : (cx, cz, y) -> last.testFullLayer(cx, cz, y) || s.containsChunkLayer(cx, cz, y);
            }

            assert orChain != null;
            return orChain.chainLayerPredicate(chunkX, chunkZ, prev);
        }

        @Override
        public IBlockPosPredicate chainBlockPredicate(int chunkX, int chunkZ, @Nullable IBlockPosPredicate prev)
        {
            if (this.testChunk(chunkX, chunkZ) == false)
            {
                return IBlockPosPredicate.ALWAYS_FALSE;
            }

            IBlockPosPredicate orChain = null;
            for (ShapeSpawnSphere s : this.filteredSpawnSpheres)
            {
                IBlockPosPredicate last = orChain;
                orChain = last == null ? s::contains : (x, y, z) -> last.testBlock(x, y, z) || s.contains(x, y, z);
            }

            assert orChain != null;
            return orChain.chainBlockPredicate(chunkX, chunkZ, prev);
        }

        @Override
        public boolean isSame(LightLevelPosLimiter last)
        {
            return last.getClass() == SpawnSphere.class &&
                    this.spawnSpheres.equals(((SpawnSphere) last).spawnSpheres);
        }
    }

    public static class SlimeChunk extends LightLevelPosLimiter
    {
        private final boolean isSeedKnown;
        private final long seed;

        public SlimeChunk(Level world, int centerX, int centerY, int centerZ, boolean aggressiveOpt)
        {
            super(centerX, centerY, centerZ, aggressiveOpt);
            HudDataManager data = HudDataManager.getInstance();
            this.isSeedKnown = data.isWorldSeedKnown(world);
            this.seed = data.getWorldSeed(world);
        }

        @Override
        public boolean testBlock(int x, int y, int z)
        {
            return y < 40;
        }

        @Override
        public boolean testChunk(int chunkX, int chunkZ)
        {
            return this.isSeedKnown && MiscUtils.canSlimeSpawnInChunk(chunkX, chunkZ, this.seed);
        }

        @Override
        public IBlockPosPredicate chainBlockPredicate(int chunkX, int chunkZ, @Nullable IBlockPosPredicate prev)
        {
            return prev; // maxY did the job already
        }

        @Override
        public IChunkPosPredicate chainChunkPredicate(@Nullable IChunkPosPredicate prev)
        {
            return this.isSeedKnown ? super.chainChunkPredicate(prev) : IChunkPosPredicate.ALWAYS_FALSE;
        }

        @Override
        protected void updateCache(int chunkX, int chunkZ) {}

        @Override
        public int getXZMask(int chunkX, int chunkZ)
        {
            return 0;
        }

        @Override
        public int getMinY(int chunkX, int chunkZ)
        {
            return Integer.MIN_VALUE;
        }

        @Override
        public int getMaxY(int chunkX, int chunkZ)
        {
            return 39;
        }

        @Override
        public boolean isSame(LightLevelPosLimiter last)
        {
            if (last.getClass() != SlimeChunk.class)
            {
                return false;
            }

            SlimeChunk l = (SlimeChunk) last;
            if (this.isSeedKnown != l.isSeedKnown)
            {
                return false;
            }

            return this.isSeedKnown == false || this.seed == l.seed;
        }
    }

    public static class List extends ArrayList<LightLevelPosLimiter>
    {
        private final IChunkPosPredicate cacheLookup;
        public List(IChunkPosPredicate cacheLookup)
        {
            this.cacheLookup = cacheLookup;
            super(2); // there's only 2 unique limiter currently
        }

        public IChunkPosPredicate chainChunkPredicate()
        {
            IChunkPosPredicate predicate = (chunkX, chunkZ) -> this.cacheLookup.testChunk(chunkX, chunkZ) == false;
            for (LightLevelPosLimiter l : this)
            {
                predicate = l.chainChunkPredicate(predicate);
            }
            return predicate;
        }

        public IBlockPosPredicate chainBlockPredicate(int chunkX, int chunkZ)
        {
            IBlockPosPredicate predicate = null;
            for (LightLevelPosLimiter l : this)
            {
                predicate = l.chainBlockPredicate(chunkX, chunkZ, predicate);
            }
            return predicate != null ? predicate : (_, _, _) -> true;
        }

        public int getMinY(int chunkX, int chunkZ, int worldMinY)
        {
            int minY = worldMinY;
            for (LightLevelPosLimiter l : this)
            {
                int m = l.getMinY(chunkX, chunkZ);
                if (m > minY)
                {
                    minY = m;
                }
            }
            return minY;
        }

        public int getMaxY(int chunkX, int chunkZ, int worldMaxY)
        {
            int maxY = worldMaxY;
            for (LightLevelPosLimiter l : this)
            {
                int m = l.getMaxY(chunkX, chunkZ);
                if (m < maxY)
                {
                    maxY = m;
                }
            }
            return maxY;
        }

        public int getXZMask(int chunkX, int chunkZ)
        {
            int mask = 0;
            for (LightLevelPosLimiter l : this)
            {
                mask |= l.getXZMask(chunkX, chunkZ);
            }
            return mask;
        }

        public boolean testFullLayer(int chunkX, int chunkZ, int y)
        {
            for (LightLevelPosLimiter l : this)
            {
                if (l.testFullLayer(chunkX, chunkZ, y) == false)
                {
                    return false;
                }
            }
            return true;
        }

        public boolean isSame(List last)
        {
            if (this.size() != last.size())
            {
                return false;
            }

            for (int i = this.size() - 1; i >= 0; --i)
            {
                if (this.get(i).isSame(last.get(i)) == false)
                {
                    return false;
                }
            }

            return true;
        }
    }
}
