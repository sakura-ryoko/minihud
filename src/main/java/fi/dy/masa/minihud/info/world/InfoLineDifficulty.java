package fi.dy.masa.minihud.info.world;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineChunkCache;
import fi.dy.masa.minihud.info.InfoLineContext;

public class InfoLineDifficulty extends InfoLine
{
    private static final String DIFF_KEY = Reference.MOD_ID+".info_line.difficulty";

    public InfoLineDifficulty(InfoToggle type)
    {
        super(type);
    }

    public InfoLineDifficulty()
    {
        this(InfoToggle.DIFFICULTY);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@Nonnull InfoLineContext ctx)
    {
        if (this.getClientWorld() == null || ctx.pos() == null)
        {
            return null;
        }

        return this.parseBlockPos(ctx.world() == null ? this.getClientWorld() : ctx.world(), ctx.pos());
    }

    @Override
    public List<Entry> parseBlockPos(@Nonnull World world, @Nonnull BlockPos pos)
    {
        List<Entry> list = new ArrayList<>();
        long chunkInhabitedTime = 0L;
        float moonPhaseFactor = 0.0F;
        ChunkPos chunkPos = new ChunkPos(pos);
//            WorldChunk serverChunk = this.getChunk(chunkPos);
        WorldChunk serverChunk = InfoLineChunkCache.INSTANCE.getChunk(chunkPos);

        if (serverChunk != null)
        {
            moonPhaseFactor = world.getMoonSize();
            chunkInhabitedTime = serverChunk.getInhabitedTime();
        }

        LocalDifficulty diff = new LocalDifficulty(world.getDifficulty(), world.getTimeOfDay(), chunkInhabitedTime, moonPhaseFactor);

        list.add(this.translate(DIFF_KEY,
                                diff.getLocalDifficulty(), diff.getClampedLocalDifficulty(), world.getTimeOfDay() / 24000L)
        );

        return list;
    }
}
