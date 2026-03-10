package fi.dy.masa.minihud.renderer;

import java.util.HashMap;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBeamOwner;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.mixin.block.IMixinBeaconBlockEntity;

public class OverlayRendererBeaconRange extends BaseBlockRangeOverlay<BeaconBlockEntity>
{
    public static final OverlayRendererBeaconRange INSTANCE = new OverlayRendererBeaconRange();
//    private final AnsiLogger LOGGER = new AnsiLogger(OverlayRendererBeaconRange.class, true, true);
    private final HashMap<BlockPos, Integer> positions;

    public OverlayRendererBeaconRange()
    {
        super(RendererToggle.OVERLAY_BEACON_RANGE, BlockEntityType.BEACON, BeaconBlockEntity.class);
        this.useCulling = false;
        this.positions = new HashMap<>();
        this.useCulling = false;
        this.updateDistance = 24;
    }

    @Override
    public String getName()
    {
        return "BeaconRange";
    }

    @Override
    protected void updateBlockRange(Level world, BlockPos pos, BeaconBlockEntity be, Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        List<BeaconBeamOwner.Section> segments = ((IMixinBeaconBlockEntity) be).minihud_getBeamEmitter();
        Holder<MobEffect> primary = ((IMixinBeaconBlockEntity) be).minihud_getPrimary();
//        RegistryEntry<StatusEffect> secondary = ((IMixinBeaconBlockEntity) be).minihud_getSecondary();
        final int level = ((IMixinBeaconBlockEntity) be).minihud_getLevel();

//        System.out.printf("beacon - pos [%s], level [%d], pri [%s], sec [%s], segment count: [%d]\n", pos, level,
//                          primary != null ? primary.value().getName().getString() : "<NULL>",
//                          secondary != null ? secondary.value().getName().getString() : "<NULL>",
//                          segments.size()
//        );

        if (segments.isEmpty() || level == 0)
        {
            this.positions.remove(pos);
        }
        else if (level >= 1 && level <= 4 && primary != null)
        {
            this.positions.put(pos, level);
        }
        else
        {
            this.positions.remove(pos);
        }
    }

    @Override
    protected void renderBlockRange(Level world, Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        this.renderThrough = false;

        if (!this.positions.isEmpty())
        {
            this.allocateBuffers(true);
            this.renderQuads(world, cameraPos, mc, profiler);
            this.renderOutlines(world, cameraPos, mc, profiler);
        }
        else
        {
            this.clearBuffers();
        }
    }

    @Override
    protected void expireBlockRange(BlockPos pos)
    {
	    this.positions.remove(pos);
    }

    @Override
    protected void resetBlockRange()
    {
        this.positions.clear();
    }

    private void renderQuads(Level world, Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null || mc.player == null)
        {
            return;
        }

        final double camX = cameraPos.x;
        final double camY = cameraPos.y;
        final double camZ = cameraPos.z;

        profiler.push("beacon_quads");
        RenderObjectVbo ctx = this.renderObjects.getFirst();
        BufferBuilder builder = ctx.start(() -> "minihud:beacon/quads", this.renderThrough ? MaLiLibPipelines.POSITION_COLOR_MASA_NO_DEPTH_NO_CULL : MaLiLibPipelines.MINIHUD_SHAPE_OFFSET_NO_CULL);
//        MatrixStack matrices = new MatrixStack();

//        matrices.push();

        this.positions.forEach(
                (pos, level) ->
        {
            final double x = pos.getX() - camX;
            final double y = pos.getY() - camY;
            final double z = pos.getZ() - camZ;

            final Color4f color = getColorForLevel(level);
            final int range = level * 10 + 10;
            final double minX = x - range;
            final double minY = y - range;
            final double minZ = z - range;
            final double maxX = x + range + 1;
            final double maxY = this.getTopYOverTerrain(world, pos, range);
            final double maxZ = z + range + 1;

            fi.dy.masa.malilib.render.RenderUtils.drawBoxAllSidesBatchedQuads((float) minX, (float) minY, (float) minZ, (float) maxX, (float) maxY, (float) maxZ, color, builder);
        });

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
            MiniHUD.LOGGER.error("OverlayRendererBeaconRange#renderQuads(): Exception; {}", err.getMessage());
        }

        profiler.pop();
    }

    private void renderOutlines(Level world, Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null || mc.player == null)
        {
            return;
        }

        final double camX = cameraPos.x;
        final double camY = cameraPos.y;
        final double camZ = cameraPos.z;

        profiler.push("beacon_outlines");
        RenderObjectVbo ctx = this.renderObjects.get(1);
        BufferBuilder builder = ctx.start(() -> "minihud:beacon/outlines", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH);

        this.positions.forEach(
                (pos, level) ->
        {
            final double x = pos.getX() - camX;
            final double y = pos.getY() - camY;
            final double z = pos.getZ() - camZ;

            final Color4f color = Color4f.fromColor(getColorForLevel(level), 0xFF);
            final int range = level * 10 + 10;
            final double minX = x - range;
            final double minY = y - range;
            final double minZ = z - range;
            final double maxX = x + range + 1;
            final double maxY = this.getTopYOverTerrain(world, pos, range);
            final double maxZ = z + range + 1;

            fi.dy.masa.malilib.render.RenderUtils.drawBoxAllEdgesBatchedLines((float) minX, (float) minY, (float) minZ, (float) maxX, (float) maxY, (float) maxZ, color, this.glLineWidth, builder);
        });

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
            MiniHUD.LOGGER.error("OverlayRendererBeaconRange#renderOutlines(): Exception; {}", err.getMessage());
        }

//        matrices.pop();
        profiler.pop();
    }

    public static Color4f getColorForLevel(int level)
    {
        return switch (level)
        {
            case 1 -> Configs.Colors.BEACON_RANGE_LVL1_OVERLAY_COLOR.getColor();
            case 2 -> Configs.Colors.BEACON_RANGE_LVL2_OVERLAY_COLOR.getColor();
            case 3 -> Configs.Colors.BEACON_RANGE_LVL3_OVERLAY_COLOR.getColor();
            default -> Configs.Colors.BEACON_RANGE_LVL4_OVERLAY_COLOR.getColor();
        };
    }
}
