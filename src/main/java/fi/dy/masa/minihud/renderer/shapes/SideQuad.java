package fi.dy.masa.minihud.renderer.shapes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;

public record SideQuad(long startPos, int width, int height, Direction side)
{
    @Override
    public @NotNull String toString()
    {
        return "SideQuad{start=" + String.format("BlockPos{x=%d,y=%d,z=%d}",
                                                 BlockPos.getX(this.startPos),
                                                 BlockPos.getY(this.startPos),
                                                 BlockPos.getZ(this.startPos)) +
                       ", width=" + this.width +
                       ", height=" + this.height +
                       ", side=" + this.side + '}';
    }
}
