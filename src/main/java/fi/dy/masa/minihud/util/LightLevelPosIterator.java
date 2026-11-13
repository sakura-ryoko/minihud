package fi.dy.masa.minihud.util;

import java.util.Arrays;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;

import fi.dy.masa.malilib.util.WorldUtils;
import fi.dy.masa.minihud.mixin.world.IMixinPalettedContainer;
import fi.dy.masa.minihud.util.LightLevelScanComponentBase.*;

public abstract class LightLevelPosIterator
{
    /**
     * The original and the stable iterator which was used before LightLevelPosIterator is a thing.
     */
    public static final LightLevelPosIterator DEFAULT = new Default();
    /**
     * The faster iterator that efficiently skips large chunks of solid blocks and air.
     * 90+% faster than the default one.
     * However, it uses fake pos to determine blocks in the palette, which can possibly cause inaccurate result.
     */
    public static final LightLevelPosIterator PALETTE_LOOKUP = new PaletteLookup();

    protected final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

    public abstract void iterate(Level world, LevelChunk chunk, int chunkX, int chunkZ, boolean underWater, boolean aggressiveOpt, LightLevelPosLimiter.List limiters, IBlockConsumer consumer);

    private static class Default extends LightLevelPosIterator
    {
        @Override
        public void iterate(Level world, LevelChunk chunk, int chunkX, int chunkZ, boolean underWater, boolean aggressiveOpt, LightLevelPosLimiter.List limiters, IBlockConsumer consumer)
        {
            final int xzMask = limiters.getXZMask(chunkX, chunkZ);
            if (xzMask == -1)
            {
                return;
            }

            final int worldMaxY = WorldUtils.getHighestSectionYOffset(chunk) + 16;
            final int startX = (chunkX << 4) | LightLevelPosLimiter.getMinX(xzMask);
            final int endX   = (chunkX << 4) | LightLevelPosLimiter.getMaxX(xzMask);
            final int startZ = (chunkZ << 4) | LightLevelPosLimiter.getMinZ(xzMask);
            final int endZ   = (chunkZ << 4) | LightLevelPosLimiter.getMaxZ(xzMask);
            final int startY = limiters.getMinY(chunkX, chunkZ, world.getMinY());
            final int endY = limiters.getMaxY(chunkX, chunkZ, worldMaxY);
            int checkedSectionY = 1; // anything but multiply of 16
            BlockState lastSkip = null;

            final IBlockPosPredicate posPredicate = limiters.chainBlockPredicate(chunkX, chunkZ);

            for (int y = startY; y <= endY; ++y)
            {
                if (y > startY)
                {
                    // If there are no blocks in the section below this layer, then we can skip it
                    LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y - 1));

                    if (section.hasOnlyAir())
                    {
                        //y += 16 - (y & 0xF);
                        // jump to the next section if y - 1 is the same section.
                        // essentially Math.ceil(y / 16.0) * 16.
                        // the code above fails when y & 0xF == 0.
                        y = ((y - 1) | 0xF) + 1;
                        continue;
                    }
                }

                if (aggressiveOpt && checkedSectionY != (y & ~0xF) && y < worldMaxY)
                {
                    checkedSectionY = y & ~0xF;
                    LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
                    // fake pos, may cause inaccurate result.
                    this.mutablePos.set(chunkX << 4, checkedSectionY, chunkZ << 4);
                    // If there are all conductors (blocks spawning), then we can skip it
                    boolean hasNonBlocking = section.getStates().maybeHas(s -> s.isRedstoneConductor(world, mutablePos) == false && (underWater || s.getFluidState().isEmpty()));
                    if (hasNonBlocking == false)
                    {
                        y |= 0xF;
                        continue;
                    }
                }

                boolean partial = limiters.testFullLayer(chunkX, chunkZ, y) == false;

                for (int z = startZ; z <= endZ; ++z)
                {
                    for (int x = startX; x <= endX; ++x)
                    {
                        if (LightLevelPosLimiter.checkXZ(xzMask, x, z) == false ||
                                partial && posPredicate.testBlock(x, y, z) == false)
                        {
                            continue;
                        }

                        this.mutablePos.set(x, y, z);
                        BlockState state = chunk.getBlockState(this.mutablePos);
                        if (aggressiveOpt)
                        {
                            // compares with state at another pos, may cause inaccurate result.
                            if (lastSkip == state)
                            {
                                continue;
                            }

                            if (state.isRedstoneConductor(world, this.mutablePos))
                            {
                                lastSkip = state;
                                continue;
                            }
                        }

                        consumer.accept(x, y, z, this.mutablePos, state);
                    }
                }
            }
        }
    }

    private static class PaletteLookup extends LightLevelPosIterator
    {
        @Override
        public void iterate(Level world, LevelChunk chunk, int chunkX, int chunkZ, boolean underWater, boolean aggressiveOpt, LightLevelPosLimiter.List limiters, IBlockConsumer consumer)
        {
            final int xzMask = limiters.getXZMask(chunkX, chunkZ);
            if (xzMask == -1)
            {
                return;
            }

            final int startX = chunkX << 4;
            final int startZ = chunkZ << 4;
            final int worldMinY = world.getMinY();
            final int worldMaxY = WorldUtils.getHighestSectionYOffset(chunk) + 16;
            final int startY = limiters.getMinY(chunkX, chunkZ, worldMinY);
            final int endY = limiters.getMaxY(chunkX, chunkZ, worldMaxY);
            final LevelChunkSection[] sections = chunk.getSections();
            final IBlockPosPredicate posPredicate = limiters.chainBlockPredicate(chunkX, chunkZ);

            final boolean[] hasState = new boolean[State.values().length];
            // 256 last section's top layer and 4096 section states.
            final State[] states = new State[256 + 4096];
            State[] paletteArr = new State[32];
            // determines if last states should be copied to the start
            int lastSectionIndex = -2;
            final int[] index = new int[1];

            int sectionIndex = chunk.getSectionIndex(startY);
            final int endSectionIndex = chunk.getSectionIndex(endY - 1);

            if (startY > worldMinY && (startY & 15) == 0)
            {
                // Doesn't seem worth it to load a section just for 256 blocks, give it a floor and leave it to others.
                Arrays.fill(states, 0, 256, State.BLOCKED);
            }

            while (sectionIndex <= endSectionIndex)
            {
                LevelChunkSection section = sections[sectionIndex];
                int fromIndex = 256;
                int toIndex = 256 + 4096;
                if (section.hasOnlyAir())
                {
                    if (sectionIndex - 1 == lastSectionIndex)
                    {
                        // has previous data, scan one layer
                        System.arraycopy(states, 4096, states, 0, 256);
                        Arrays.fill(states, 256, 256 + 256, State.AIR);
                        toIndex = 256 + 256;
                    }
                    else
                    {
                        // skip entirely
                        sectionIndex++;
                        continue;
                    }
                }
                else
                {
                    PalettedContainer.Data<BlockState> data = ((IMixinPalettedContainer) section.getStates()).minihud_getData();
                    // init, resize if needed
                    Palette<BlockState> palette = data.palette();
                    int size = palette.getSize();
                    if (paletteArr.length < size)
                    {
                        paletteArr = new State[size];
                    }
                    Arrays.fill(hasState, false);

                    // fake pos, may cause inaccurate result.
                    this.mutablePos.set(startX, worldMinY + (sectionIndex << 4), startZ);
                    // convert palette to enum
                    for (int i = 0; i < size; i++)
                    {
                        State state = State.OTHER;
                        BlockState block = palette.valueFor(i);
                        if (block.isAir())
                        {
                            state = State.AIR;
                        }
                        else if (block.isRedstoneConductor(world, this.mutablePos) || (underWater == false && block.getFluidState().isEmpty() == false))
                        {
                            state = State.BLOCKED;
                        }
                        paletteArr[i] = state;
                        hasState[state.ordinal()] = true;
                    }

                    if (hasState[State.OTHER.ordinal()] == false && hasState[State.AIR.ordinal()] == false)
                    {
                        // only blocking
                        Arrays.fill(states, 4096, 256 + 4096, State.BLOCKED);
                        lastSectionIndex = sectionIndex;
                        sectionIndex++;
                        continue;
                    }

                    if (size < paletteArr.length)
                    {
                        // just in case
                        Arrays.fill(paletteArr, size, paletteArr.length, State.AIR);
                    }

                    if (sectionIndex - 1 == lastSectionIndex)
                    {
                        // move previous data
                        System.arraycopy(states, 4096, states, 0, 256);
                    }
                    lastSectionIndex = sectionIndex;
                    // grab states
                    final State[] finalPaletteArr = paletteArr;
                    index[0] = 256;
                    data.storage().getAll(v -> states[index[0]++] = finalPaletteArr[v]);
                    if (index[0] < 256 + 4096)
                    {
                        Arrays.fill(states, index[0], 256 + 4096, State.AIR);
                    }
                }
                // calculate range
                int sectionY = worldMinY + (sectionIndex << 4);
                fromIndex = Math.max(fromIndex, 256 + (startY - sectionY) * 256);
                toIndex   = Math.min(toIndex,   256 + (  endY - sectionY) * 256);

                for (int i = fromIndex; i < toIndex; i++)
                {
                    if (states[i] == State.BLOCKED || states[i - 256] == State.AIR)
                    {
                        // either it's blocked, or can't spawn on air
                        continue;
                    }

                    int x = i & 15;
                    int z = i >> 4 & 15;

                    if (LightLevelPosLimiter.checkXZ(xzMask, x, z))
                    {
                        x += startX;
                        int y = sectionY + (i >> 8) - 1;
                        z += startZ;
                        if (posPredicate.testBlock(x, y, z))
                        {
                            this.mutablePos.set(x, y, z);
                            consumer.accept(x, y, z, this.mutablePos, world.getBlockState(this.mutablePos));
                        }
                    }
                }

                sectionIndex++;
            }

            if (sectionIndex - 1 == lastSectionIndex && worldMinY + (sectionIndex << 4) == endY)
            {
                // scan last layer, not worth to load it
                for (int i = 4096; i < 256 + 4096; i++)
                {
                    // only check the ground since no data loaded on top
                    if (states[i] != State.AIR)
                    {
                        int x = i & 15;
                        int z = i >> 4 & 15;

                        if (LightLevelPosLimiter.checkXZ(xzMask, x, z))
                        {
                            x += startX;
                            z += startZ;
                            if (posPredicate.testBlock(x, endY, z))
                            {
                                this.mutablePos.set(x, endY, z);
                                consumer.accept(x, endY, z, this.mutablePos, world.getBlockState(this.mutablePos));
                            }
                        }
                    }
                }
            }
        }

        private enum State
        {
            AIR,
            BLOCKED,
            OTHER
        }
    }
}
