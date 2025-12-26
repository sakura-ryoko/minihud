package fi.dy.masa.minihud.renderer;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import fi.dy.masa.malilib.config.IConfigBoolean;

public abstract class BaseBlockRangeOverlay<T extends BlockEntity> extends OverlayRendererBase
{
//    private final AnsiLogger LOGGER = new AnsiLogger(BaseBlockRangeOverlay.class, true, true);
    protected final IConfigBoolean renderToggleConfig;
    protected final LongOpenHashSet blockPositions;
    protected final BlockEntityType<T> blockEntityType;
    protected final Class<T> blockEntityClass;
    protected World world;
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
    public boolean shouldRender(MinecraftClient mc)
    {
        return this.renderToggleConfig.getBooleanValue();
    }

    @Override
    public boolean needsUpdate(Entity cameraEntity, MinecraftClient mc)
    {
        return this.needsUpdate || this.lastUpdatePos == null ||
               Math.abs(cameraEntity.getX() - this.lastUpdatePos.getX()) > this.updateDistance ||
               Math.abs(cameraEntity.getZ() - this.lastUpdatePos.getZ()) > this.updateDistance ||
               Math.abs(cameraEntity.getY() - this.lastUpdatePos.getY()) > this.updateDistance;
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity, MinecraftClient mc, Profiler profiler)
    {
        if (mc.world == null) return;

        this.hasData = this.fetchAllTargetBlockEntityPositions(mc.world, entity.getBlockPos(), mc);
        this.world = entity.getEntityWorld();

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
    public void render(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
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

    protected boolean fetchAllTargetBlockEntityPositions(ClientWorld world, BlockPos centerPos, MinecraftClient mc)
    {
        ClientChunkManager chunkManager = world.getChunkManager();
        int centerCX = centerPos.getX() >> 4;
        int centerCZ = centerPos.getZ() >> 4;
        int chunkRadius = mc.options.getViewDistance().getValue();

//        LOGGER.debug("fetchAllTargetBlockEntityPositions(): hasData: {} // positions: {}", this.hasData, this.blockPositions.size());
        this.blockPositions.clear();

        for (int cz = centerCZ - chunkRadius; cz <= centerCZ + chunkRadius; ++cz)
        {
            for (int cx = centerCX - chunkRadius; cx <= centerCX + chunkRadius; ++cx)
            {
                WorldChunk chunk = chunkManager.getChunk(cx, cz, ChunkStatus.FULL, false);

                if (chunk != null)
                {
                    for (BlockEntity be : chunk.getBlockEntities().values())
                    {
                        if (be.getType() == this.blockEntityType)
                        {
                            synchronized (this.blockPositions)
                            {
                                this.blockPositions.add(be.getPos().asLong());
                                this.hasData = true;
                            }
                        }
                    }
                }
            }
        }

        return !this.blockPositions.isEmpty() && this.blockPositions.size() > 0;
    }

    protected void updateBlockRanges(World world, Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        LongIterator it = this.blockPositions.iterator();
        BlockPos.Mutable mutablePos = new BlockPos.Mutable();
        double max = (mc.options.getViewDistance().getValue() + 2) * 16;
        max = max * max;

        profiler.push("render_block_ranges");
//        LOGGER.debug("updateBlockRanges(): hasData: {} // positions: {}", this.hasData, this.blockPositions.size());

        while (it.hasNext())
        {
            mutablePos.set(it.nextLong());
            BlockEntity be = world.getBlockEntity(mutablePos);

            if (be == null || !this.blockEntityClass.isAssignableFrom(be.getClass()))
            {
                it.remove();
                continue;
            }

            double distSq = (cameraPos.x - mutablePos.getX()) * (cameraPos.x - mutablePos.getX()) +
                            (cameraPos.z - mutablePos.getZ()) * (cameraPos.z - mutablePos.getZ());

            if (distSq > max)
            {
                this.expireBlockRange(mutablePos.toImmutable());
                continue;
            }

            T castBe = this.blockEntityClass.cast(be);
            this.updateBlockRange(world, mutablePos.toImmutable(), castBe, cameraPos, mc, profiler);
        }

        profiler.pop();
    }

    protected abstract void updateBlockRange(World world, BlockPos pos, T be, Vec3d cameraPos, MinecraftClient mc, Profiler profiler);

    protected abstract void renderBlockRange(World world, Vec3d cameraPos, MinecraftClient mc, Profiler profiler);

    protected abstract void expireBlockRange(BlockPos pos);

    protected abstract void resetBlockRange();
}
