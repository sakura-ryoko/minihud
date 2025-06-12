package fi.dy.masa.minihud.info.block;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;

public class InfoLineLookingAtChunk extends InfoLine
{
    private static final String BLOCK_KEY = Reference.MOD_ID+".info_line.looking_at_block";
    private static final String CHUNK_KEY = Reference.MOD_ID+".info_line.looking_at_block_chunk";

    public InfoLineLookingAtChunk(InfoToggle type)
    {
        super(type);
    }

    public InfoLineLookingAtChunk()
    {
        super(InfoToggle.LOOKING_AT_BLOCK_CHUNK);
    }

    @Override
    public boolean succeededType() { return this.succeeded; }

    @Override
    public List<Entry> parse(@NotNull InfoLine.Context ctx)
    {
        return ctx.pos() != null ? this.parseBlockPos(ctx.world(), ctx.pos()) : null;
    }

    @Override
    public List<Entry> parseBlockPos(@Nonnull World world, @Nonnull BlockPos pos)
    {
        List<Entry> list = new ArrayList<>();

        String str =
                this.qt(BLOCK_KEY, pos.getX(), pos.getY(), pos.getZ())
                + " // "
                + this.qt(CHUNK_KEY,
                          pos.getX() & 0xF, pos.getY() & 0xF, pos.getZ() & 0xF,
                          pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4
                );

        list.add(this.of(str));
        this.succeeded = true;

        return list;
    }
}
