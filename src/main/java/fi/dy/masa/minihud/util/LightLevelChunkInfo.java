package fi.dy.masa.minihud.util;

import java.util.Iterator;
import java.util.function.Predicate;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntImmutableList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortImmutableList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import org.jspecify.annotations.NonNull;

import fi.dy.masa.malilib.util.position.Vec3d;
import fi.dy.masa.minihud.MiniHUD;

/**
 * Stores light level info in yzx order.
 * <p>
 * Expects new info coming in yzx order.
 * Although it's possible to be implemented to accept any order.
 */
public class LightLevelChunkInfo
{
    // Assumes light level values to be within 0 to 15.
    private static final int LIGHT_LEVEL_BITS = 4;
    private static final int LIGHT_LEVEL_MASK = (1 << LIGHT_LEVEL_BITS) - 1;
    private static final int HEIGHT_BITS = Integer.SIZE - 2 * LIGHT_LEVEL_BITS;
    private static final int HEIGHT_RESOLUTION = 1 << HEIGHT_BITS;
    private static final int HEIGHT_MASK = HEIGHT_RESOLUTION - 1;

    /**
     * Cluster them into counts, even index means gap and odd index means infos.
     * <p>
     * To read <code>{ 123, 456, 789, 1011 }</code>, start from 0, 0, 0,
     *  skip 123 blocks, read 456 entries (from infos), skip 789 blocks, read 1011 entries.
     * <p>
     * Cluster count is capped at 32767 since it's unlikely to happen and skips negative checking.
     * <p>
     * To skip 40000 blocks, just do <code>{ 32767, 0, 40000 - 32767, ... }</code>.
     */
    private ShortList clusters = new ShortArrayList(512);
    /**
     * Packed integers: block, sky, y.
     */
    private IntList infos = new IntArrayList(512);
    /**
     * For vertex buffer allocation purpose
     */
    private final short[] blockLightCounts = new short[16];

    private boolean sealed;
    public final int x, z;
    private final int worldMinY;
    private int lastX, lastY, lastZ;

    private static int packInfo(int block, int sky, float h)
    {
        return (block << (LIGHT_LEVEL_BITS + HEIGHT_BITS)) |
                ((sky & LIGHT_LEVEL_MASK) << HEIGHT_BITS) |
                (int) Math.clamp(Math.ceil((h % 1.0f) * HEIGHT_RESOLUTION), 0, HEIGHT_MASK);
    }

    private static void unpackInfo(int y, int info, LightLevelInfo to)
    {
        to.block = (byte) (info >>> (LIGHT_LEVEL_BITS + HEIGHT_BITS));
        to.sky = (byte) ((info >>> HEIGHT_BITS) & LIGHT_LEVEL_MASK);
        to.y = y + (info & HEIGHT_MASK) / (float) HEIGHT_RESOLUTION;
    }

    private static boolean checkCondition(int info, int blockLightMask)
    {
        int block = info >>> (LIGHT_LEVEL_BITS + HEIGHT_BITS);
        return (blockLightMask & (1 << block)) == 0;
    }

    private static int generateBlockLightMask(Predicate<Integer> condition)
    {
        int mask = 0;
        for (int i = 0; i < 16; i++)
        {
            if (condition.test(i))
            {
                mask |= 1 << i;
            }
        }
        return ~mask;
    }

    public LightLevelChunkInfo(int chunkX, int chunkZ, int worldMinY)
    {
        this.sealed = false;
        this.x = chunkX * 16;
        this.z = chunkZ * 16;
        this.worldMinY = worldMinY;
        this.lastX = 15;
        this.lastY = worldMinY - 1;
        this.lastZ = 15;
    }

    public void push(int x, int y, int z, float h, int block, int sky)
    {
        if (this.sealed)
        {
            throw new IllegalStateException("Trying to push into sealed data");
        }

        x -= this.x;
        z -= this.z;
        if (x < 0 || x > 15 || z < 0 || z > 15)
        {
            throw new IllegalArgumentException("Expects x and z values to be within 0 and 15. Got " + x + " and " + z);
        }
        if (block < 0 || block > 15 || sky < 0 || sky > 15)
        {
            throw new IllegalArgumentException("Expects light levels to be within 0 and 15. Got " + block + " and " + sky);
        }

        // yzx order
        int gap = (y - this.lastY) * 256 +
                  (z - this.lastZ) * 16 +
                  (x - this.lastX)
                  - 1;
        if (gap < 0)
        {
            throw new IllegalStateException("Expects new info coming in yzx order. Calculated gap: " + gap);
        }
        this.lastX = x;
        this.lastY = y;
        this.lastZ = z;

        final ShortList clusters = this.clusters;

        if (gap > 0 || clusters.isEmpty())
        {
            while (gap > 32767)
            {
                clusters.add((short) 32767);
                clusters.add((short) 0);
                gap -= 32767;
            }
            clusters.add((short) gap);
            clusters.add((short) 1);
        }
        else
        {
            int lastIndex = clusters.size() - 1;
            short value = clusters.getShort(lastIndex);
            if (value == 32767) // possible in water world
            {
                clusters.add((short) 0);
                clusters.add((short) 1);
            }
            else
            {
                clusters.set(lastIndex, (short) (value + 1));
            }
        }

        this.infos.add(packInfo(block, sky, h));
    }

    public void seal()
    {
        if (this.sealed)
        {
            return;
        }

        // These assert should be impossible but just to be sure and make iterator comfortable
        if ((this.clusters.size() % 2) == 1)
        {
            MiniHUD.LOGGER.warn("[ChunkLightLevelInfo] Detected odd numbers of elements in clusters list.");
            this.clusters.removeLast();
        }
        while (this.clusters.isEmpty() == false && this.clusters.getLast().intValue() == 0)
        {
            this.clusters.removeLast();
            this.clusters.removeLast();
        }

        int total = 0;
        for (int i = this.clusters.size() - 1; i > 0; i -= 2)
        {
            total += this.clusters.getShort(i);
        }
        if (total != this.infos.size())
        {
            MiniHUD.LOGGER.warn("[ChunkLightLevelInfo] Detected mismatch between cluster size and infos.");
            if (this.infos.size() > total)
            {
                this.infos.size(total);
            }
            else
            {
                int toRemove = total - this.infos.size();
                while (toRemove > 0)
                {
                    int value = this.clusters.removeLast().intValue();
                    if (toRemove >= value)
                    {
                        toRemove -= value;
                        this.clusters.removeLast();
                    }
                    else
                    {
                        this.clusters.add((short) (value - toRemove));
                        toRemove = 0;
                    }
                }
            }
        }

        this.clusters = new ShortImmutableList(this.clusters);
        this.infos = new IntImmutableList(this.infos);

        int[] blockLights = new int[16];
        for (int info : this.infos)
        {
            blockLights[info >>> (LIGHT_LEVEL_BITS + HEIGHT_BITS)]++;
        }
        for (int i = 0; i < 16; i++)
        {
            this.blockLightCounts[i] = (short) Math.min(blockLights[i], Short.MAX_VALUE);
        }
        sealed = true;
//        System.out.printf("clusters: %d, infos: %d\n", this.clusters.size(), this.infos.size());
    }

    public int size()
    {
        return this.infos.size();
    }

    public boolean isEmpty()
    {
        return this.infos.isEmpty();
    }

    public Iterable<LightLevelInfo> iterable(int minY, int xzMask, Predicate<Integer> condition)
    {
        if (this.sealed == false)
        {
            throw new IllegalStateException("Trying to iterate while data is not sealed");
        }

        return new Iterable<>()
        {
            @Override
            public @NonNull Iterator<LightLevelInfo> iterator()
            {
                return new InfoIterator(minY, xzMask, condition);
            }
        };
    }

    class InfoIterator implements Iterator<LightLevelInfo>
    {
        // decides coordinates.
        private int mainIndex;
        // indexes clusters, should be odd.
        private int clusterIndex;
        // value of clusters.getShort(clusterIndex).intValue().
        private int clusterSize;
        // current cluster index, should be smaller than clusterSize.
        private int clusterIndex2 = 0;
        // indexes infos.
        private int infoIndex = 0;
        // max index determined by condition.
        private int infoMaxIndex = -1;
        // filters block light level
        private final int blockLightMask;
        private final int xzMask;
        private final LightLevelInfo mutable = new LightLevelInfo();

        private InfoIterator(int minY, int xzMask, Predicate<Integer> condition)
        {
            this.blockLightMask = LightLevelChunkInfo.generateBlockLightMask(condition);
            this.xzMask = xzMask;

            if (LightLevelChunkInfo.this.isEmpty() || xzMask == -1)
            {
                return;
            }

            // find the last info index
            final IntList infos = LightLevelChunkInfo.this.infos;
            this.infoMaxIndex = infos.size() - 1;
            while (LightLevelChunkInfo.checkCondition(infos.getInt(this.infoMaxIndex), this.blockLightMask) == false)
            {
                this.infoMaxIndex--;
                if (this.infoMaxIndex < 0)
                {
                    return;
                }
            }

            // find the first info index
            final ShortList clusters = LightLevelChunkInfo.this.clusters;
            this.mainIndex = clusters.getShort(0);
            this.clusterIndex = 1;
            this.clusterSize = clusters.getShort(1);
            this.skip();

            // find the first valid info index based on minY
            if (minY != Integer.MIN_VALUE)
            {
                int target = (minY - LightLevelChunkInfo.this.worldMinY) * 256;
                while (this.mainIndex < target && this.hasNext())
                {
                    int toSkip = Math.min(target - this.mainIndex, this.clusterSize - this.clusterIndex2);
                    this.mainIndex += toSkip;
                    this.clusterIndex2 += toSkip;
                    this.infoIndex += toSkip;
                    this.skip();
                }
            }
        }

        @Override
        public boolean hasNext()
        {
            return this.infoIndex <= this.infoMaxIndex;
        }

        private void skip()
        {
            if (this.hasNext() == false)
            {
                return;
            }

            final ShortList clusters = LightLevelChunkInfo.this.clusters;
            final IntList infos = LightLevelChunkInfo.this.infos;
            while (true)
            {
                // skip infos that doesn't meet condition
                while (this.clusterIndex2 < this.clusterSize)
                {
                    if (LightLevelChunkInfo.checkCondition(infos.getInt(this.infoIndex), this.blockLightMask) && this.checkXZ())
                    {
                        // found next valid info
                        return;
                    }

                    this.mainIndex++;
                    this.clusterIndex2++;
                    this.infoIndex++;

                    if (this.hasNext() == false)
                    {
                        return;
                    }
                }

                // current cluster finished, next one.
                // OOB protected by sealing.
                this.clusterIndex2 = 0;
                int clusterSize;
                do
                {
                    this.mainIndex += clusters.getShort(this.clusterIndex + 1);
                    this.clusterIndex += 2;
                }
                while ((clusterSize = clusters.getShort(this.clusterIndex)) == 0);
                this.clusterSize = clusterSize;
            }
        }

        @Override
        public LightLevelInfo next()
        {
            if (this.hasNext() == false)
            {
                throw new IllegalStateException("No more info to be iterated");
            }

            this.mutable.x = LightLevelChunkInfo.this.x + (this.mainIndex & 15);
            int y = LightLevelChunkInfo.this.worldMinY + (this.mainIndex / 256);
            this.mutable.z = LightLevelChunkInfo.this.z + ((this.mainIndex >>> 4) & 15);
            LightLevelChunkInfo.unpackInfo(y, LightLevelChunkInfo.this.infos.getInt(this.infoIndex), this.mutable);

            this.mainIndex++;
            this.clusterIndex2++;
            this.infoIndex++;
            this.skip();

            return this.mutable;
        }

        private boolean checkXZ()
        {
            return this.xzMask == 0 || LightLevelPosLimiter.checkXZ(this.xzMask, this.mainIndex & 15, (this.mainIndex >>> 4) & 15);
        }
    }

    // eliminates ChunkPos instantiation
    public static class Map extends Long2ObjectOpenHashMap<LightLevelChunkInfo>
    {
        private static final long MASK32 = ((1L << 32L) - 1L);

        public void put(int chunkX, int chunkZ, LightLevelChunkInfo info)
        {
            long key = ((long) chunkX << 32L) | ((long) chunkZ & MASK32);
            super.put(key, info);
        }

        public boolean contains(int chunkX, int chunkZ)
        {
            long key = ((long) chunkX << 32L) | ((long) chunkZ & MASK32);
            return super.containsKey(key);
        }

        public void removeNeighbors(int chunkX, int chunkZ)
        {
            long keyLeft = (long) chunkX << 32L;
            long key0 = keyLeft | ((long) (chunkZ - 1) & MASK32);
            long key1 = keyLeft | ((long) (chunkZ    ) & MASK32);
            long key2 = keyLeft | ((long) (chunkZ + 1) & MASK32);
            super.remove(key0);
            super.remove(key1);
            super.remove(key2);
            super.remove(key0 - (1L << 32L));
            super.remove(key1 - (1L << 32L));
            super.remove(key2 - (1L << 32L));
            super.remove(key0 + (1L << 32L));
            super.remove(key1 + (1L << 32L));
            super.remove(key2 + (1L << 32L));
        }

        public int countAllMarks(Predicate<Integer> blockLightCondition)
        {
            final int mask = LightLevelChunkInfo.generateBlockLightMask(blockLightCondition);
            int total = 0;
            for (LightLevelChunkInfo info : this.values())
            {
                for (int i = 0; i < 16; i++)
                {
                    if ((mask & (1 << i)) == 0)
                    {
                        total += info.blockLightCounts[i];
                    }
                }
            }
            return total;
        }

        // Doesn't have to be exact.
        public int countMarks(Vec3d camera, int range, Predicate<Integer> blockLightCondition)
        {
            final int mask = LightLevelChunkInfo.generateBlockLightMask(blockLightCondition);
            int total = 0;
            int minCX = (int) Math.floor(camera.x - range) >> 4;
            int minCZ = (int) Math.floor(camera.z - range) >> 4;
            int maxCX = (int) Math.floor(camera.x + range) >> 4;
            int maxCZ = (int) Math.floor(camera.z + range) >> 4;
            for (Entry<LightLevelChunkInfo> info : this.long2ObjectEntrySet())
            {
                long key = info.getLongKey();
                int cx = (int) (key >> 32L);
                int cz = (int) key;
                if (cx < minCX || cx > maxCX || cz < minCZ || cz > maxCZ)
                {
                    continue;
                }

                short[] blc = info.getValue().blockLightCounts;
                for (int i = 0; i < 16; i++)
                {
                    if ((mask & (1 << i)) == 0)
                    {
                        total += blc[i];
                    }
                }
            }

            return range <= 64 ? Math.min(total, range * range * range) : total;
        }
    }
}
