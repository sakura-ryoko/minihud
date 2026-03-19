package fi.dy.masa.minihud.renderer;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import fi.dy.masa.malilib.util.data.DataBlockUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBeamOwner;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
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
import org.apache.commons.lang3.tuple.Pair;

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
        this.updateDistance = 16;
    }

    @Override
    public String getName()
    {
        return "BeaconRange";
    }

    @Override
    protected void updateBlockRange(Level world, BlockPos pos, BeaconBlockEntity be, Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        IMixinBeaconBlockEntity beaconBE = (IMixinBeaconBlockEntity) be;

        if (this.checkBeaconActivation(mc, world, pos, beaconBE))
        {
            this.positions.put(pos, beaconBE.minihud_getLevel());
        }
        else
        {
            this.positions.remove(pos);
        }
    }

    private boolean checkBeaconActivation(Minecraft mc, Level world, BlockPos pos, IMixinBeaconBlockEntity be)
    {
        final int level = be.minihud_getLevel();
        List<BeaconBeamOwner.Section> segments = be.minihud_getBeamEmitter();
        Holder<MobEffect> primary;
//        Holder<MobEffect> primary = be.minihud_getPrimary();
//        Holder<MobEffect> secondary = be.minihud_getSecondary();
//        LOGGER.debug("beacon - pos [{}], level [{}], pri [{}], sec [{}], segment count: [{}]", pos, level,
//                          primary != null ? primary.value().getDisplayName().getString() : "<NULL>",
//                          secondary != null ? secondary.value().getDisplayName().getString() : "<NULL>",
//                          segments.size()
//        );

        if (level < 1 || level > 4 || segments.isEmpty())
        {
            return false;
        }

        if (Configs.Generic.BEACON_RANGE_OVERLAY_CHECK_PRIMARY_EFFECT.getBooleanValue())
        {
            Pair<BlockEntity, CompoundData> pair = this.fetchBlockEntityData(world, pos);

            if (pair != null)
            {
                primary = DataBlockUtils.getBeaconEffects(pair.getRight()).getLeft();
            }
            else
            {
                primary = be.minihud_getPrimary();      // Fallback Check Only
            }

            if (primary != null)
            {
                ResourceKey<MobEffect> key = primary.unwrapKey().orElse(null);

                return key != null && BeaconBlockEntity.BEACON_EFFECTS
                        .stream()
//                        .limit(level) ??
                        .flatMap(Collection::stream)
                        .anyMatch(it -> it.is(key));
            }

            return false;
        }
        else
        {
            return true;
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
