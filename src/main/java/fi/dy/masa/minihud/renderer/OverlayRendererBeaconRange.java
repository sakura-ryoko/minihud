package fi.dy.masa.minihud.renderer;

import org.joml.Matrix4fStack;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.ShaderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;

import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.render.RenderContext;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.mixin.block.IMixinBeaconBlockEntity;

public class OverlayRendererBeaconRange extends BaseBlockEntityRangeOverlay<BeaconBlockEntity>
{
    public static final OverlayRendererBeaconRange INSTANCE = new OverlayRendererBeaconRange();

    public OverlayRendererBeaconRange()
    {
        super(RendererToggle.OVERLAY_BEACON_RANGE, BlockEntityType.BEACON, BeaconBlockEntity.class);
    }

    @Override
    public String getName()
    {
        return "BeaconRange";
    }

    @Override
    protected void renderBlockRange(World world, BlockPos pos, BeaconBlockEntity be, Vec3d cameraPos, Profiler profiler)
    {
        int level = ((IMixinBeaconBlockEntity) be).minihud_getLevel();

        if (level >= 1 && level <= 4)
        {
            this.renderQuads(world, pos, level, cameraPos, getColorForLevel(level), profiler);
            this.renderOutlines(world, pos, level, cameraPos, getColorForLevel(level), profiler);
        }
    }

    private void renderQuads(World world, BlockPos pos, int level, Vec3d cameraPos, Color4f color, Profiler profiler)
    {
        double x = pos.getX() - cameraPos.x;
        double y = pos.getY() - cameraPos.y;
        double z = pos.getZ() - cameraPos.z;

        int range = level * 10 + 10;
        double minX = x - range;
        double minY = y - range;
        double minZ = z - range;
        double maxX = x + range + 1;
        double maxY = this.getTopYOverTerrain(world, pos, range);
        double maxZ = z + range + 1;

        profiler.push("beacon_quads");
        RenderContext ctx = new RenderContext(() -> "Beacon Quads", MaLiLibPipelines.POSITION_COLOR_SIMPLE, GlUsage.STATIC_WRITE);
        BufferBuilder builder = ctx.getBuilder();
        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        Vec3d updatePos = this.getUpdatePosition();

        this.preRender();
        matrix4fstack.pushMatrix();
        matrix4fstack.translate((float) (updatePos.x - cameraPos.x), (float) (updatePos.y - cameraPos.y), (float) (updatePos.z - cameraPos.z));

        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllSidesBatchedQuads((float) minX, (float) minY, (float) minZ, (float) maxX, (float) maxY, (float) maxZ, color, builder);

        try
        {
            ctx.drawColor(builder.endNullable());
            ctx.close();
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererBeaconRange#renderQuads(): Exception; {}", err.getMessage());
        }

        this.postRender();
        matrix4fstack.popMatrix();
        profiler.pop();
    }

    private void renderOutlines(World world, BlockPos pos, int level, Vec3d cameraPos, Color4f color, Profiler profiler)
    {
        double x = pos.getX() - cameraPos.x;
        double y = pos.getY() - cameraPos.y;
        double z = pos.getZ() - cameraPos.z;

        int range = level * 10 + 10;
        double minX = x - range;
        double minY = y - range;
        double minZ = z - range;
        double maxX = x + range + 1;
        double maxY = this.getTopYOverTerrain(world, pos, range);
        double maxZ = z + range + 1;

        profiler.push("beacon_outlines");
        RenderContext ctx = new RenderContext(() -> "Beacon Outlines", ShaderPipelines.LINES, GlUsage.STATIC_WRITE);
        BufferBuilder builder = ctx.getBuilder();
        MatrixStack matrices = new MatrixStack();
        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        Vec3d updatePos = this.getUpdatePosition();

        this.preRender();
        matrices.push();
        matrix4fstack.pushMatrix();
        matrix4fstack.translate((float) (updatePos.x - cameraPos.x), (float) (updatePos.y - cameraPos.y), (float) (updatePos.z - cameraPos.z));

        MatrixStack.Entry e = matrices.peek();

        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllEdgesBatchedLines((float) minX, (float) minY, (float) minZ, (float) maxX, (float) maxY, (float) maxZ, Color4f.fromColor(color.intValue, 1f), builder, e);

        try
        {
            ctx.drawColor(builder.endNullable());
            ctx.close();
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererBeaconRange#renderOutlines(): Exception; {}", err.getMessage());
        }

        this.postRender();
        matrix4fstack.popMatrix();
        matrices.pop();
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
