package fi.dy.masa.minihud.renderer;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.joml.Matrix4f;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
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
import fi.dy.masa.malilib.util.WorldUtils;

public abstract class BaseBlockRangeOverlay<T extends BlockEntity> extends OverlayRendererBase
{
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
    public void update(Vec3d cameraPos, Entity entity, MinecraftClient mc)
    {
        if (mc.world == null) return;

        this.hasData = this.fetchAllTargetBlockEntityPositions(mc.world, entity.getBlockPos(), mc);
        this.world = entity.getEntityWorld();
        //MiniHUD.printDebug("BaseBlockRangeOverlay#update(): wasEmpty: {} // beInRange: {}", this.wasEmpty, beInRange);

        this.needsUpdate = false;
    }

    @Override
    public boolean hasData()
    {
        return this.hasData && !this.blockPositions.isEmpty();
    }

    @Override
    public void render(Camera camera, Matrix4f matrix4f, Matrix4f projMatrix, MinecraftClient mc, Profiler profiler)
    {
        if (this.hasData())
        {
            Vec3d cameraPos = camera.getPos();
            this.renderBlockRanges(this.world, cameraPos, mc, profiler);
        }
    }

    private void clear()
    {
        synchronized (this.blockPositions)
        {
            this.blockPositions.clear();
        }
    }

    @Override
    public void reset()
    {
        super.reset();
        this.clear();
        this.hasData = false;
    }

    protected boolean fetchAllTargetBlockEntityPositions(ClientWorld world, BlockPos centerPos, MinecraftClient mc)
    {
        ClientChunkManager chunkManager = world.getChunkManager();
        int centerCX = centerPos.getX() >> 4;
        int centerCZ = centerPos.getZ() >> 4;
        int chunkRadius = mc.options.getViewDistance().getValue();
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

    protected void renderBlockRanges(World world, Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        LongIterator it = this.blockPositions.iterator();
        BlockPos.Mutable mutablePos = new BlockPos.Mutable();
        double max = (mc.options.getViewDistance().getValue() + 2) * 16;
        max = max * max;

        profiler.push("render_block_ranges");

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
                continue;
            }

            T castBe = this.blockEntityClass.cast(be);
            this.renderBlockRange(world, mutablePos, castBe, cameraPos, profiler);
        }

        profiler.pop();
    }

    protected int getTopYOverTerrain(World world, BlockPos pos, int range)
    {
        final int minX = pos.getX() - range;
        final int minZ = pos.getZ() - range;
        final int maxX = pos.getX() + range;
        final int maxZ = pos.getZ() + range;

        final int minCX = minX >> 4;
        final int minCZ = minZ >> 4;
        final int maxCX = maxX >> 4;
        final int maxCZ = maxZ >> 4;
        int maxY = 0;

        for (int cz = minCZ; cz <= maxCZ; ++cz)
        {
            for (int cx = minCX; cx <= maxCX; ++cx)
            {
                WorldChunk chunk = world.getChunk(cx, cz);
                int height = WorldUtils.getHighestSectionYOffset(chunk) + 15;

                if (height > maxY)
                {
                    maxY = height;
                }
            }
        }

        return maxY + 4;
    }

    protected abstract void renderBlockRange(World world, BlockPos pos, T be, Vec3d cameraPos, Profiler profiler);
}
