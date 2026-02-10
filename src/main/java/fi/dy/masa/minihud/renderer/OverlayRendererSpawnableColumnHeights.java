package fi.dy.masa.minihud.renderer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;

public class OverlayRendererSpawnableColumnHeights extends OverlayRendererBase
{
    public static final OverlayRendererSpawnableColumnHeights INSTANCE = new OverlayRendererSpawnableColumnHeights();

    private final Set<Long> DIRTY_CHUNKS = new HashSet<>();
    private final BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();
    private long lastCheckTime;
    private final List<AABB> boxes;

    protected OverlayRendererSpawnableColumnHeights()
    {
        this.boxes = new ArrayList<>();
        this.renderThrough = false;
        this.useCulling = false;
    }

    @Override
    public String getName()
    {
        return "SpawnableColumnHeights";
    }

    public void markChunkChanged(int cx, int cz)
    {
        if (RendererToggle.OVERLAY_SPAWNABLE_COLUMN_HEIGHTS.getBooleanValue())
        {
            synchronized (this.DIRTY_CHUNKS)
            {
                this.DIRTY_CHUNKS.add(ChunkPos.asLong(cx, cz));
            }
        }
    }

    @Override
    public boolean shouldRender(Minecraft mc)
    {
        return RendererToggle.OVERLAY_SPAWNABLE_COLUMN_HEIGHTS.getBooleanValue();
    }

    @Override
    public boolean needsUpdate(Entity entity, Minecraft mc)
    {
        int ex = (int) Math.floor(entity.getX());
        int ez = (int) Math.floor(entity.getZ());
        int lx = this.lastUpdatePos.getX();
        int lz = this.lastUpdatePos.getZ();

        if (Math.abs(lx - ex) > 8 || Math.abs(lz - ez) > 8)
        {
            return true;
        }

        if (System.currentTimeMillis() - this.lastCheckTime > 1000)
        {
            final int radius = Mth.clamp(Configs.Generic.SPAWNABLE_COLUMNS_OVERLAY_RADIUS.getIntegerValue(), 0, 128);
            final int xStart = (((int) entity.getX() - radius) >> 4);
            final int zStart = (((int) entity.getZ() - radius) >> 4);
            final int xEnd = (((int) entity.getX() + radius) >> 4);
            final int zEnd = (((int) entity.getZ() + radius) >> 4);

            synchronized (this.DIRTY_CHUNKS)
            {
                for (int cx = xStart; cx <= xEnd; ++cx)
                {
                    for (int cz = zStart; cz <= zEnd; ++cz)
                    {
                        if (this.DIRTY_CHUNKS.contains(ChunkPos.asLong(cx, cz)))
                        {
                            return true;
                        }
                    }
                }
            }

            this.lastCheckTime = System.currentTimeMillis();
        }

        return false;
    }

    @Override
    public void update(Vec3 cameraPos, Entity entity, Minecraft mc, ProfilerFiller profiler)
    {
        this.calculateChunks(cameraPos, entity, mc);
        this.lastCheckTime = System.currentTimeMillis();

        synchronized (this.DIRTY_CHUNKS)
        {
            this.DIRTY_CHUNKS.clear();
        }

        if (this.hasData())
        {
            this.render(cameraPos, mc, profiler);
        }
    }

    @Override
    public boolean hasData()
    {
        return !this.boxes.isEmpty();
    }

    @Override
    public void render(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        this.allocateBuffers();
        this.renderQuads(cameraPos, mc, profiler);
        this.renderOutlines(cameraPos, mc, profiler);
    }

    private void renderQuads(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null || mc.player == null)
        {
            return;
        }

        final Color4f color = Configs.Colors.SPAWNABLE_COLUMNS_OVERLAY_COLOR.getColor();

        profiler.push("column_quads");
        RenderObjectVbo ctx = this.renderObjects.getFirst();
        BufferBuilder builder = ctx.start(() -> "minihud:spawnable_column/quads", MaLiLibPipelines.POSITION_COLOR_MASA_LEQUAL_DEPTH);

        for (AABB bb : this.boxes)
        {
            fi.dy.masa.malilib.render.RenderUtils.drawBoxHorizontalSidesBatchedQuads((float) bb.minX, (float) bb.minY, (float) bb.minZ, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ, color, builder);
            fi.dy.masa.malilib.render.RenderUtils.drawBoxTopBatchedQuads((float) bb.minX, (float) bb.minZ, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ, color, builder);
        }

        try
        {
            MeshData meshData = builder.build();

            if (meshData != null)
            {
                ctx.upload(meshData, this.shouldResort);

                if (this.shouldResort)
                {
                    ctx.startResorting(meshData, ctx.createVertexSorter(cameraPos));
                }

                meshData.close();
            }
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererSpawnableColumnHeights#renderQuads(): Exception; {}", err.getMessage());
        }

        profiler.pop();
    }

    private void renderOutlines(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null || mc.player == null)
        {
            return;
        }

        final Color4f color = Color4f.fromColor(Configs.Colors.SPAWNABLE_COLUMNS_OVERLAY_COLOR.getColor(), 0xFF);
        profiler.push("column_outlines");
        RenderObjectVbo ctx = this.renderObjects.get(1);
        BufferBuilder builder = ctx.start(() -> "minihud:spawnable_column/outlines", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH);

        for (AABB bb : this.boxes)
        {
            fi.dy.masa.malilib.render.RenderUtils.drawBoxAllEdgesBatchedLines((float) bb.minX, (float) bb.minY, (float) bb.minZ, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ, color, this.glLineWidth, builder);
        }

        try
        {
            MeshData meshData = builder.build();

            if (meshData != null)
            {
                ctx.upload(meshData, false);
                meshData.close();
            }
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererSpawnableColumnHeights#renderOutlines(): Exception; {}", err.getMessage());
        }

        profiler.pop();
    }

    private void calculateChunks(Vec3 cameraPos, Entity entity, Minecraft mc)
    {
        if (mc.level == null) return;

        final int radius = Mth.clamp(Configs.Generic.SPAWNABLE_COLUMNS_OVERLAY_RADIUS.getIntegerValue(), 0, 128);
        final int xStart = (int) entity.getX() - radius;
        final int zStart = (int) entity.getZ() - radius;
        final int xEnd = (int) entity.getX() + radius;
        final int zEnd = (int) entity.getZ() + radius;
        final Level world = mc.level;

        this.boxes.clear();

        for (int x = xStart; x <= xEnd; ++x)
        {
            final double minX = x + 0.25 - cameraPos.x;
            final double maxX = minX + 0.5;

            for (int z = zStart; z <= zEnd; ++z)
            {
                // See WorldEntitySpawner.getRandomChunkPosition()
                final int height = world.getChunkAt(this.posMutable.set(x, 0, z)).getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 1;
                final double minY = height - cameraPos.y;
                final double maxY = minY + 0.09375;
                final double minZ = z + 0.25 - cameraPos.z;
                final double maxZ = minZ + 0.5;

                this.boxes.add(new AABB(minX, minY, minZ, maxX, maxY, maxZ));
            }
        }
    }

    @Override
    public void reset()
    {
        super.reset();
        this.boxes.clear();
        this.lastCheckTime = -1;
        synchronized (this.DIRTY_CHUNKS)
        {
            this.DIRTY_CHUNKS.clear();
        }
    }
}
