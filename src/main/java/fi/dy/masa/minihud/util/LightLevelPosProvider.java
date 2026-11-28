package fi.dy.masa.minihud.util;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiConsumer;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.renderer.shapes.ShapeSpawnSphere;

/**
 * Provider generates chunk pos -> filter by limiter -> generate block pos by iterator -> filter by limiter.
 * There can be zero or multiple limiters, but provider and iterator are unique.
 */
public abstract class LightLevelPosProvider extends LightLevelScanComponentBase
{
    private final LightLevelPosLimiter.List limiters;
    private final LightLevelPosIterator iterator;
    protected boolean stop;

    public LightLevelPosProvider(int centerX, int centerY, int centerZ, boolean aggressiveOpt, LightLevelPosLimiter.List limiters, LightLevelPosIterator iterator)
    {
        super(centerX, centerY, centerZ, aggressiveOpt);
        this.limiters = limiters;
        this.iterator = iterator;
    }

    public void generate(Level world, boolean underWater, IChunkIterateBody body)
    {
        this.stop = false;

        IChunkPosPredicate limiter = this.limiters.chainChunkPredicate();

        this.generate((chunkX, chunkZ) ->
        {
            if (limiter.testChunk(chunkX, chunkZ))
            {
                body.accept(chunkX, chunkZ, (chunk, consumer) -> this.iterator.iterate(world, chunk, chunkX, chunkZ, underWater, this.aggressiveOpt, this.limiters, consumer));
            }
        });
    }

    protected abstract void generate(IChunkPosConsumer consumer);

    @SuppressWarnings("CommentedOutCode")
    protected void generateBetween(int minCX, int minCZ, int maxCX, int maxCZ, IChunkPosConsumer consumer)
    {
        // normal
//        for (int cz = minCZ; cz <= maxCZ; ++cz)
//        {
//            for (int cx = minCX; cx <= maxCX; ++cx)
//            {
//                consumer.accept(cx, cz);
//                if (this.stop)
//                {
//                    return;
//                }
//            }
//        }

        int cx = this.centerX >> 4;
        int cz = this.centerZ >> 4;

        // center (camera)
        if (cx >= minCX && cx <= maxCX && cz >= minCZ && cz <= maxCZ)
        {
            consumer.accept(cx, cz);
            if (this.stop)
            {
                return;
            }
        }

        int step = 0;
        int maxStep = 0;
        final int finalStep = Math.max(
                Math.max(Math.abs(minCX - cx), Math.abs(maxCX - cx)),
                Math.max(Math.abs(minCZ - cz), Math.abs(maxCZ - cz))
        ) * 2;

        // spiral, clockwise
        while (maxStep < finalStep)
        {
            // next ring
            cx--;
            cz--;
            maxStep += 2;
            // north
            if (cz < minCZ || cz > maxCZ || cx > maxCX) // out of range
            {
                cx += maxStep;
            }
            else
            {
                if (cx < minCX) // behind
                {
                    int skip = Math.min(minCX - cx, maxStep);
                    cx += skip;
                    step += skip;
                }
                while (step < maxStep) // walk
                {
                    consumer.accept(cx, cz);
                    if (this.stop)
                    {
                        return;
                    }
                    cx++;
                    step++;
                    if (cx > maxCX) // out of range
                    {
                        cx += maxStep - step;
                        break;
                    }
                }
                step = 0;
            }
            // east
            if (cx < minCX || cx > maxCX || cz > maxCZ) // out of range
            {
                cz += maxStep;
            }
            else
            {
                if (cz < minCZ) // behind
                {
                    int skip = Math.min(minCZ - cz, maxStep);
                    cz += skip;
                    step += skip;
                }
                while (step < maxStep) // walk
                {
                    consumer.accept(cx, cz);
                    if (this.stop)
                    {
                        return;
                    }
                    cz++;
                    step++;
                    if (cz > maxCZ) // out of range
                    {
                        cz += maxStep - step;
                        break;
                    }
                }
                step = 0;
            }
            // south
            if (cz < minCZ || cz > maxCZ || cx < minCX) // out of range
            {
                cx -= maxStep;
            }
            else
            {
                if (cx > maxCX) // behind
                {
                    int skip = Math.min(cx - maxCX, maxStep);
                    cx -= skip;
                    step += skip;
                }
                while (step < maxStep) // walk
                {
                    consumer.accept(cx, cz);
                    if (this.stop)
                    {
                        return;
                    }
                    cx--;
                    step++;
                    if (cx < minCX) // out of range
                    {
                        cx -= maxStep - step;
                        break;
                    }
                }
                step = 0;
            }
            // west
            if (cx < minCX || cx > maxCX || cz < minCZ) // out of range
            {
                cz -= maxStep;
            }
            else
            {
                if (cz > maxCZ) // behind
                {
                    int skip = Math.min(cz - maxCZ, maxStep);
                    cz -= skip;
                    step += skip;
                }
                while (step < maxStep) // walk
                {
                    consumer.accept(cx, cz);
                    if (this.stop)
                    {
                        return;
                    }
                    cz--;
                    step++;
                    if (cz < minCZ) // out of range
                    {
                        cz -= maxStep - step;
                        break;
                    }
                }
                step = 0;
            }
        }
    }

    public void stop()
    {
        this.stop = true;
    }

    // if renderer should ignore the range option
    public boolean ignoreRange()
    {
        return false;
    }

    // if condition didn't change that doesn't need a rescan
    public boolean isSame(@Nullable LightLevelPosProvider last)
    {
        return last != null && this.getClass() == last.getClass() && this.isComponentsSame(last);
    }

    protected boolean isComponentsSame(LightLevelPosProvider last)
    {
        // iterator shouldn't change the result but just to show off the performance difference
        return this.limiters.isSame(last.limiters) && this.iterator == last.iterator;
    }

    public static class Default extends LightLevelPosProvider
    {
        public Default(int centerX, int centerY, int centerZ, boolean aggressiveOpt, LightLevelPosLimiter.List limiters, LightLevelPosIterator iterator)
        {
            super(centerX, centerY, centerZ, aggressiveOpt, limiters, iterator);
        }

        @Override
        protected void generate(IChunkPosConsumer consumer)
        {
            int range = Configs.Generic.LIGHT_LEVEL_RANGE.getIntegerValue();
            int minCX = (this.centerX - range) >> 4;
            int minCZ = (this.centerZ - range) >> 4;
            int maxCX = (this.centerX + range) >> 4;
            int maxCZ = (this.centerZ + range) >> 4;
            this.generateBetween(minCX, minCZ, maxCX, maxCZ, consumer);
        }
    }

    public static class SpawnSphere extends LightLevelPosProvider
    {
        private final List<ShapeSpawnSphere> spawnSpheres;

        public SpawnSphere(int centerX, int centerY, int centerZ, boolean aggressiveOpt, List<ShapeSpawnSphere> spawnSpheres, LightLevelPosLimiter.List limiters, LightLevelPosIterator iterator)
        {
            super(centerX, centerY, centerZ, aggressiveOpt, limiters, iterator);
            if (spawnSpheres.isEmpty())
            {
                throw new IllegalArgumentException("spawnSpheres must not be empty");
            }
            this.spawnSpheres = spawnSpheres;
        }

        @SuppressWarnings("OptionalGetWithoutIsPresent")
        @Override
        protected void generate(IChunkPosConsumer consumer)
        {
            int minCX = this.spawnSpheres.stream().mapToInt(s -> (int) Math.floor(s.getEffectiveCenter().x - s.getMargin() - s.getRadius())).min().getAsInt() >> 4;
            int minCZ = this.spawnSpheres.stream().mapToInt(s -> (int) Math.floor(s.getEffectiveCenter().z - s.getMargin() - s.getRadius())).min().getAsInt() >> 4;
            int maxCX = this.spawnSpheres.stream().mapToInt(s -> (int) Math.ceil(s.getEffectiveCenter().x + s.getMargin() + s.getRadius())).max().getAsInt() >> 4;
            int maxCZ = this.spawnSpheres.stream().mapToInt(s -> (int) Math.ceil(s.getEffectiveCenter().z + s.getMargin() + s.getRadius())).max().getAsInt() >> 4;
            this.generateBetween(minCX, minCZ, maxCX, maxCZ, consumer);
        }

        @Override
        public boolean ignoreRange()
        {
            return true;
        }

        @Override
        public boolean isSame(@Nullable LightLevelPosProvider last)
        {
            return last != null && last.getClass() == SpawnSphere.class &&
                    this.spawnSpheres.equals(((SpawnSphere) last).spawnSpheres) &&
                    this.isComponentsSame(last);
        }
    }

    public interface IChunkIterateBody
    {
        void accept(int chunkX, int chunkZ, BiConsumer<LevelChunk, IBlockConsumer> iterator);
    }
}
