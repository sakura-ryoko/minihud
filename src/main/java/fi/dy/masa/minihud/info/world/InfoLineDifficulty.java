package fi.dy.masa.minihud.info.world;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.core.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
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
    public List<Entry> parseBlockPos(@Nonnull Level world, @Nonnull BlockPos pos)
    {
        List<Entry> list = new ArrayList<>();
        long chunkInhabitedTime = 0L;
        float moonPhaseFactor = 0.0F;
        ChunkPos chunkPos = new ChunkPos(pos);
//            WorldChunk serverChunk = this.getChunk(chunkPos);
        LevelChunk serverChunk = InfoLineChunkCache.INSTANCE.getChunk(chunkPos);

        if (serverChunk != null)
        {
			MoonPhase moonPhase = this.mc().gameRenderer.getLevelRenderState().skyRenderState.moonPhase;
	        moonPhaseFactor = DimensionType.MOON_BRIGHTNESS_PER_PHASE[moonPhase.index()];
//            moonPhaseFactor = world.getMoonSize();
	        // That was harder....
            chunkInhabitedTime = serverChunk.getInhabitedTime();
        }

        DifficultyInstance diff = new DifficultyInstance(world.getDifficulty(), world.getDayTime(), chunkInhabitedTime, moonPhaseFactor);

        list.add(this.translate(DIFF_KEY,
                                diff.getEffectiveDifficulty(), diff.getSpecialMultiplier(), world.getDayTime() / 24000L)
        );

        return list;
    }
}
