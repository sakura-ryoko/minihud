package fi.dy.masa.minihud.info.block;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;

public class InfoLineLookingAtBlock extends InfoLine
{
    private static final String BLOCK_KEY = Reference.MOD_ID+".info_line.looking_at_block";

    public InfoLineLookingAtBlock(InfoToggle type)
    {
        super(type);
    }

    public InfoLineLookingAtBlock()
    {
        super(InfoToggle.LOOKING_AT_BLOCK);
    }

    @Override
    public boolean succeededType() { return this.succeeded; }

    @Override
    public List<Entry> parse(@NotNull InfoLineContext ctx)
    {
        if (ctx.world() == null) return null;

        if (InfoToggle.LOOKING_AT_BLOCK_CHUNK.getBooleanValue())
        {
            return null;
        }

        return ctx.pos() != null ? this.parseBlockPos(ctx.world(), ctx.pos()) : null;
    }

    @Override
    public List<Entry> parseBlockPos(@Nonnull Level world, @Nonnull BlockPos pos)
    {
        List<Entry> list = new ArrayList<>();

        list.add(this.translate(BLOCK_KEY, pos.getX(), pos.getY(), pos.getZ()));
        this.succeeded = true;

        return list;
    }
}
