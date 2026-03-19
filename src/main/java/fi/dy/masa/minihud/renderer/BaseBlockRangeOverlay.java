package fi.dy.masa.minihud.renderer;

import fi.dy.masa.malilib.util.WorldUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.minihud.data.EntitiesDataManager;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import fi.dy.masa.malilib.config.IConfigBoolean;
import org.apache.commons.lang3.tuple.Pair;

public abstract class BaseBlockRangeOverlay<T extends BlockEntity> extends OverlayRendererBase
{
//    private final AnsiLogger LOGGER = new AnsiLogger(BaseBlockRangeOverlay.class, true, true);
    protected final IConfigBoolean renderToggleConfig;
    protected final LongOpenHashSet blockPositions;
    protected final BlockEntityType<T> blockEntityType;
    protected final Class<T> blockEntityClass;
    protected Level world;
    protected boolean needsUpdate;
    protected boolean hasData;
    protected int updateDistance = 48;

    protected BaseBlockRangeOverlay(IConfigBoolean renderToggleConfig,
                                    BlockEntityType<T> blockEntityType,
                                    Class<T> blockEntityClass)
    {
        this.renderToggleConfig = renderToggleConfig;
        this.blockEntityType = blockEntityType;
        this.blockEntityClass = blockEntityClass;
        this.blockPositions = new LongOpenHashSet();
        this.world = null;
        this.hasData = false;
    }

    public void setNeedsUpdate()
    {
        if (!this.renderToggleConfig.getBooleanValue())
        {
            this.clear();
            return;
        }

        this.needsUpdate = true;
        //this.needsFullRebuild = true;
    }

    public void onBlockStatusChange(BlockPos pos)
    {
        if (this.renderToggleConfig.getBooleanValue())
        {
            synchronized (this.blockPositions)
            {
                this.blockPositions.add(pos.asLong());
                this.needsUpdate = true;
            }
        }
    }

    @Override
    public boolean shouldRender(Minecraft mc)
    {
        return this.renderToggleConfig.getBooleanValue();
    }

    @Override
    public boolean needsUpdate(Entity cameraEntity, Minecraft mc)
    {
        return this.needsUpdate || this.lastUpdatePos == null ||
               Math.abs(cameraEntity.getX() - this.lastUpdatePos.getX()) > this.updateDistance ||
               Math.abs(cameraEntity.getZ() - this.lastUpdatePos.getZ()) > this.updateDistance ||
               Math.abs(cameraEntity.getY() - this.lastUpdatePos.getY()) > this.updateDistance;
    }

    @Override
    public void update(Vec3 cameraPos, Entity entity, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null) return;

        this.world = WorldUtils.getBestWorld(mc);
        this.hasData = this.fetchAllTargetBlockEntityPositions(mc.level, entity.blockPosition(), mc);
        this.world = this.world == null ? entity.level() : this.world;

//        LOGGER.debug("update(): hasData: {} // positions: {}", this.hasData, this.blockPositions.size());

        if (this.hasData())
        {
            this.updateBlockRanges(this.world, cameraPos, mc, profiler);
            // This batches all detected locations in a single render, in theory
            this.render(cameraPos, mc, profiler);
        }

        this.needsUpdate = false;
    }

    @Override
    public boolean hasData()
    {
        return this.hasData && !this.blockPositions.isEmpty();
    }

    @Override
    public void render(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
//        LOGGER.debug("render(): hasData: {} // positions: {}", this.hasData, this.blockPositions.size());
        this.renderBlockRange(this.world, cameraPos, mc, profiler);
    }

    private void clear()
    {
//        LOGGER.debug("clear(): hasData: {} // positions: {}", this.hasData, this.blockPositions.size());
        synchronized (this.blockPositions)
        {
            this.blockPositions.clear();
        }
    }

    @Override
    public void reset()
    {
//        LOGGER.debug("reset(): hasData: {} // positions: {}", this.hasData, this.blockPositions.size());
        super.reset();
        this.resetBlockRange();
        this.clear();
        this.hasData = false;
    }

    protected boolean fetchAllTargetBlockEntityPositions(ClientLevel world, BlockPos centerPos, Minecraft mc)
    {
        ClientChunkCache chunkManager = world.getChunkSource();
        int centerCX = centerPos.getX() >> 4;
        int centerCZ = centerPos.getZ() >> 4;
        int chunkRadius = mc.options.renderDistance().get();

//        LOGGER.debug("fetchAllTargetBlockEntityPositions(): hasData: {} // positions: {}", this.hasData, this.blockPositions.size());
        LongOpenHashSet positions = new LongOpenHashSet();

        for (int cz = centerCZ - chunkRadius; cz <= centerCZ + chunkRadius; ++cz)
        {
            for (int cx = centerCX - chunkRadius; cx <= centerCX + chunkRadius; ++cx)
            {
                LevelChunk chunk = chunkManager.getChunk(cx, cz, ChunkStatus.FULL, false);

                if (chunk != null)
                {
                    for (BlockEntity be : chunk.getBlockEntities().values())
                    {
                        if (be.getType() == this.blockEntityType)
                        {
                            positions.add(be.getBlockPos().asLong());
                        }
                    }
                }
            }
        }

        LongOpenHashSet removed = new LongOpenHashSet();

        synchronized (this.blockPositions)
        {
            this.blockPositions.longStream()
                .filter(pos -> !positions.contains(pos))
                .forEach(removed::add);

            this.blockPositions.clear();
            this.blockPositions.addAll(positions);
        }

        if (!removed.isEmpty())
        {
            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

            for (Long pos : removed)
            {
                mutablePos.set(pos);
                this.expireBlockRange(mutablePos.immutable());
            }
        }

        return !this.blockPositions.isEmpty();
    }

    protected void updateBlockRanges(Level world, Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        LongIterator it = this.blockPositions.iterator();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        double max = (mc.options.renderDistance().get() + 2) * 16;
        max = max * max;

        profiler.push("render_block_ranges");
//        LOGGER.debug("updateBlockRanges(): hasData: {} // positions: {}", this.hasData, this.blockPositions.size());

        while (it.hasNext())
        {
            mutablePos.set(it.nextLong());
            Pair<BlockEntity, CompoundData> pair = this.fetchBlockEntityData(world, mutablePos);
            BlockEntity be;

            if (pair != null)
            {
                be = pair.getLeft();
            }
            else
            {
                be = world.getBlockEntity(mutablePos);
            }

            if (be == null || !this.blockEntityClass.isAssignableFrom(be.getClass()))
            {
                it.remove();
                continue;
            }

            double distSq = (cameraPos.x - mutablePos.getX()) * (cameraPos.x - mutablePos.getX()) +
                            (cameraPos.z - mutablePos.getZ()) * (cameraPos.z - mutablePos.getZ());

            if (distSq > max)
            {
                this.expireBlockRange(mutablePos.immutable());
                continue;
            }

            T castBe = this.blockEntityClass.cast(be);
            this.updateBlockRange(world, mutablePos.immutable(), castBe, cameraPos, mc, profiler);
        }

        profiler.pop();
    }

    protected Pair<BlockEntity, CompoundData> fetchBlockEntityData(Level world, BlockPos pos)
    {
        BlockState state = world.getBlockState(pos);
        Pair<BlockEntity, CompoundData> pair;

        if (state.getBlock() instanceof EntityBlock)
        {
            pair = EntitiesDataManager.getInstance().requestBlockEntity(this.world, pos);

            if (pair != null && (pair.getLeft() == null || !this.blockEntityClass.isAssignableFrom(pair.getLeft().getClass())))
            {
                pair = null;
            }

            return pair;
        }

        return null;
    }

    protected abstract void updateBlockRange(Level world, BlockPos pos, T be, Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler);

    protected abstract void renderBlockRange(Level world, Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler);

    protected abstract void expireBlockRange(BlockPos pos);

    protected abstract void resetBlockRange();
}
