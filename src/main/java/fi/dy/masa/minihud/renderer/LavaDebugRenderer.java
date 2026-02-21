package fi.dy.masa.minihud.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.minihud.config.RendererToggle;

/**
 * Lava fluid level debug renderer that mirrors the vanilla WaterDebugRenderer.
 * Uses the Gizmo system for proper billboard text and filled cuboids,
 * injected into the debug render pipeline via MixinDebugRenderer.
 */
public class LavaDebugRenderer
{
    private static final int SCAN_RADIUS = 10;

    public static void emitGizmos()
    {
        if (!RendererToggle.DEBUG_LAVA.getBooleanValue())
        {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null)
        {
            return;
        }

        BlockPos playerPos = mc.player.blockPosition();
        Level level = mc.player.level();

        emitFluidHeightCuboids(level, playerPos);
        emitFluidAmountLabels(level, playerPos);
    }

    private static void emitFluidHeightCuboids(Level level, BlockPos playerPos)
    {
        for (BlockPos pos : BlockPos.betweenClosed(
                playerPos.offset(-SCAN_RADIUS, -SCAN_RADIUS, -SCAN_RADIUS),
                playerPos.offset(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS)))
        {
            FluidState fluidState = level.getFluidState(pos);

            if (fluidState.is(FluidTags.LAVA))
            {
                double topY = (double) pos.getY() + (double) fluidState.getHeight(level, pos);

                AABB aabb = new AABB(
                        (double) pos.getX() + 0.01, (double) pos.getY() + 0.01, (double) pos.getZ() + 0.01,
                        (double) pos.getX() + 0.99, topY, (double) pos.getZ() + 0.99
                );

                // Orange at 15% alpha
                Gizmos.cuboid(aabb, GizmoStyle.fill(ARGB.colorFromFloat(0.15f, 1.0f, 0.5f, 0.0f)));
            }
        }
    }

    private static void emitFluidAmountLabels(Level level, BlockPos playerPos)
    {
        for (BlockPos pos : BlockPos.betweenClosed(
                playerPos.offset(-SCAN_RADIUS, -SCAN_RADIUS, -SCAN_RADIUS),
                playerPos.offset(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS)))
        {
            FluidState fluidState = level.getFluidState(pos);

            if (fluidState.is(FluidTags.LAVA))
            {
                String text = String.valueOf(fluidState.getAmount());
                Vec3 textPos = Vec3.atLowerCornerWithOffset(pos, 0.5, fluidState.getHeight(level, pos), 0.5);
                Gizmos.billboardText(text, textPos, TextGizmo.Style.forColorAndCentered(-16777216))
                        .setAlwaysOnTop();
            }
        }
    }
}
