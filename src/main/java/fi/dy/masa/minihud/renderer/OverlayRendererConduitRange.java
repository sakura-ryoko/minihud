package fi.dy.masa.minihud.renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.phys.Vec3;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.position.PositionUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.renderer.shapes.SideQuad;
import fi.dy.masa.minihud.util.ConduitExtra;
import fi.dy.masa.minihud.util.ShapeRenderType;
import fi.dy.masa.minihud.util.shape.SphereUtils;

public class OverlayRendererConduitRange extends BaseBlockRangeOverlay<ConduitBlockEntity>
{
    public static final OverlayRendererConduitRange INSTANCE = new OverlayRendererConduitRange();
//    private final AnsiLogger LOGGER = new AnsiLogger(OverlayRendererConduitRange.class, true, true);

    private final ShapeRenderType renderType;
    private final LayerRange layerRange;
    private final Direction.Axis quadAxis;
    private boolean combineQuads;
    private Color4f colorLines;

    private final List<Entry> conduits;

    public OverlayRendererConduitRange()
    {
        super(RendererToggle.OVERLAY_CONDUIT_RANGE, BlockEntityType.CONDUIT, ConduitBlockEntity.class);
        this.quadAxis = Direction.UP.getAxis();
        this.renderType = ShapeRenderType.OUTER_EDGE;
        this.layerRange = new LayerRange(null);
        this.conduits = new ArrayList<>();
        this.useCulling = false;
    }

    @Override
    public String getName()
    {
        return "ConduitRange";
    }

    @Override
    protected void updateBlockRange(Level world, BlockPos pos, ConduitBlockEntity be, Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        if (!be.isActive())
        {
            return;
        }

        this.colorLines = Configs.Colors.CONDUIT_RANGE_OUTLINES.getColor();
        this.combineQuads = Configs.Generic.CONDUIT_RANGE_OVERLAY_COMBINE_QUADS.getBooleanValue();
        this.renderThrough = Configs.Generic.CONDUIT_RANGE_OVERLAY_RENDER_THROUGH.getBooleanValue();

//        LOGGER.debug("updateBlockRange(): pos [{}], count [{}]", pos.toShortString(), this.conduits.size());

        final int range = ((ConduitExtra) be).minihud$getStoredActivatingBlockCount() / 7 * 16;

        if (this.checkIfNeedsUpdate(pos, range))
        {
            this.addOrReplaceEntry(this.calculateEach(pos, range));
        }
    }

    private boolean checkIfNeedsUpdate(BlockPos pos, int range)
    {
        AtomicBoolean matched = new AtomicBoolean(false);

        this.conduits.forEach(entry ->
                              {
                                  if (entry.pos.equals(pos) && entry.range == range)
                                  {
                                      matched.set(true);
                                  }
                              });

	    return !matched.get();
    }

    // This is an expensive task, so we need to limit how
    // often it gets called; say hello 'checkIfNeedsUpdate()'.
    private Entry calculateEach(BlockPos pos, int range)
    {
        Entry entry = new Entry(pos, range);

        Consumer<BlockPos.MutableBlockPos> positionCollector = (p) -> entry.addPosition(p.asLong());
        entry.setTest(this.getPositionTest(pos, entry.range));
        SphereUtils.collectSpherePositions(positionCollector, entry.getTest(), pos, entry.range);

        if (this.combineQuads)
        {
            entry.setQuads(SphereUtils.buildSphereShellToQuads(entry.getPositions(), this.quadAxis, entry.getTest(), this.renderType, this.layerRange));
        }

        return entry;
    }

    private void addOrReplaceEntry(Entry entry)
    {
        AtomicBoolean replaced = new AtomicBoolean(false);

        this.conduits.forEach(
                (e) ->
                {
                    if (e.pos.compareTo(entry.pos) == 0)
                    {
                        e.clear();
                        e.range = entry.range;
                        e.positions.addAll(entry.getPositions());
                        e.setTest(entry.getTest());
                        e.setQuads(entry.getQuads());
                        replaced.set(true);
                    }
                }
        );

        if (!replaced.get())
        {
            this.conduits.add(entry);
        }
    }

    @Override
    protected void renderBlockRange(Level world, Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        boolean outlines = Configs.Generic.CONDUIT_RANGE_OVERLAY_RENDER_OUTLINES.getBooleanValue();

//        LOGGER.debug("renderBlockRange(): count [{}]", this.conduits.size());

        this.allocateBuffers(outlines);
        this.renderQuads(cameraPos, mc, profiler);

        if (outlines)
        {
            this.renderOutlines(cameraPos, mc, profiler);
        }
    }

    @Override
    protected void expireBlockRange(BlockPos pos)
    {
        for (Entry entry : this.conduits)
        {
            if (entry.pos.equals(pos))
            {
                entry.clear();
                this.conduits.remove(entry);
            }
        }
    }

    @Override
    protected void resetBlockRange()
    {
        this.conduits.forEach(Entry::clear);
        this.conduits.clear();
    }

    private void renderQuads(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null || mc.player == null)
        {
            return;
        }

        Color4f color = Configs.Colors.CONDUIT_RANGE_OVERLAY_COLOR.getColor();

        profiler.push("conduit_quads");
        RenderObjectVbo ctx = this.renderObjects.getFirst();
        BufferBuilder builder = ctx.start(() -> "minihud:conduit/quads", this.renderThrough ? MaLiLibPipelines.MINIHUD_SHAPE_NO_DEPTH_OFFSET : MaLiLibPipelines.MINIHUD_SHAPE_OFFSET_NO_CULL);

        this.conduits.forEach(
                (entry) ->
                {
//                    LOGGER.debug("renderQuads(): pos [{}], count [{}]", entry.pos.toShortString(), this.conduits.size());

                    if (this.combineQuads)
                    {
                        RenderUtils.renderQuads(entry.getQuads(), color, 0, cameraPos, builder);
                    }
                    else
                    {
                        RenderUtils.renderCircleBlockPositions(entry.getPositions(), PositionUtils.ALL_DIRECTIONS,
                                                               entry.getTest(), this.renderType,
                                                               this.layerRange, color, 0,
                                                               cameraPos, builder);
                    }
                }
        );

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
            MiniHUD.LOGGER.error("OverlayRendererConduitRange#renderQuads(): Exception; {}", err.getMessage());
        }

        profiler.pop();
    }

    private void renderOutlines(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null || mc.player == null || !Configs.Generic.CONDUIT_RANGE_OVERLAY_RENDER_OUTLINES.getBooleanValue())
        {
            return;
        }

        profiler.push("conduit_outlines");
        RenderObjectVbo ctx = this.renderObjects.get(1);
        BufferBuilder builder = ctx.start(() -> "minihud:conduit/outlines", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH);

        this.conduits.forEach(
                (entry) ->
                {
//                    LOGGER.debug("renderOutlines(): pos [{}], count [{}]", entry.pos.toShortString(), this.conduits.size());

                    if (this.combineQuads)
                    {
                        RenderUtils.renderQuadLines(entry.getQuads(), this.colorLines, 0, cameraPos, this.glLineWidth, builder);
                    }
                    else
                    {
                        RenderUtils.renderCircleBlockOutlines(entry.getPositions(), PositionUtils.ALL_DIRECTIONS,
                                                              entry.getTest(), this.renderType,
                                                              this.layerRange, this.colorLines, 0,
                                                              cameraPos, this.glLineWidth, builder);
                    }
                }
        );

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
            MiniHUD.LOGGER.error("OverlayRendererConduitRange#renderBlockRange(): Exception; {}", err.getMessage());
        }

        profiler.pop();
    }

    @Override
    public void reset()
    {
        super.reset();
        this.resetBlockRange();
    }

    protected static SphereUtils.RingPositionTest getPositionTest(BlockPos centerPos, int range)
    {
        Vec3 center = new Vec3(centerPos.getX() + 0.5, centerPos.getY() + 0.5, centerPos.getZ() + 0.5);
        double squareRange = range * range;

        return (x, y, z, dir) -> SphereUtils.isPositionInsideOrClosestToRadiusOnBlockRing(
                x, y, z, center, squareRange, Direction.EAST);
    }

    public static class Entry
    {
        public BlockPos pos;
        public int range;

        private final LongOpenHashSet positions;
        @Nullable
        private SphereUtils.RingPositionTest test;
        private final List<SideQuad> quads;

        Entry(BlockPos pos, int range)
        {
            this.pos = pos;
            this.range = range;
            this.positions = new LongOpenHashSet();
            this.test = null;
            this.quads = new ArrayList<>();
        }

        public void addPosition(long pos)
        {
            this.positions.add(pos);
        }

        public LongOpenHashSet getPositions()
        {
            return this.positions;
        }

        public void setTest(@Nullable SphereUtils.RingPositionTest test)
        {
            this.test = test;
        }

        @Nullable
        public SphereUtils.RingPositionTest getTest()
        {
            return this.test;
        }

        public void setQuads(List<SideQuad> quads)
        {
            this.quads.clear();
            this.quads.addAll(quads);
        }

        public List<SideQuad> getQuads()
        {
            return this.quads;
        }

        public void clear()
        {
            this.positions.clear();
            this.quads.clear();
            this.test = null;
        }
    }
}
