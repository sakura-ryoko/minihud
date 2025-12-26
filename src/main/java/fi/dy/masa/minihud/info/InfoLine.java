package fi.dy.masa.minihud.info;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.WorldUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.data.EntitiesDataManager;
import fi.dy.masa.minihud.data.HudDataManager;
import fi.dy.masa.minihud.util.DataStorage;

public abstract class InfoLine
{
    protected static final String REMAINING_KEY = Reference.MOD_ID+".info_line.remaining";
    private final InfoToggle type;
    protected boolean succeeded = false;

    public InfoLine(InfoToggle type)
    {
        this.type = type;
    }

    public InfoToggle getType()
    {
        return this.type;
    }

    public HudDataManager getHudData() { return HudDataManager.getInstance(); }

    public EntitiesDataManager getEntData() { return EntitiesDataManager.getInstance(); }

    public DataStorage getData() { return DataStorage.getInstance(); }

    public InfoLineChunkCache getChunkCache() { return InfoLineChunkCache.INSTANCE; }

    public Minecraft mc() { return Minecraft.getInstance(); }

    public Level getBestWorld()
    {
        return WorldUtils.getBestWorld(this.mc());
    }

    public Level getClientWorld()
    {
        return this.mc().level;
    }

    public List<Entry> parse(@Nonnull InfoLineContext ctx)
    {
        return null;
    }

    public List<Entry> parseData(@Nonnull Level world, @Nonnull EntityType<?> entityType, @Nonnull CompoundData data)
    {
        return null;
    }

    public List<Entry> parseData(@Nonnull Level world, @Nonnull BlockEntityType<?> beType, @Nonnull CompoundData data)
    {
        return null;
    }

    public List<Entry> parseEnt(@Nonnull Level world, @Nonnull Entity ent)
    {
        return null;
    }

    public List<Entry> parseBlockEnt(@Nonnull Level world, @Nonnull BlockEntity be)
    {
        return null;
    }

    public List<Entry> parseBlockPos(@Nonnull Level world, @Nonnull BlockPos pos)
    {
        return null;
    }

    public List<Entry> parseBlockState(@Nonnull Level world, @Nonnull BlockState state)
    {
        return null;
    }

	public List<Entry> parseChunkPos(@Nonnull Level world, @Nonnull ChunkPos pos)
	{
		return null;
	}

	public List<Entry> parseWorld(@Nonnull Level world)
    {
        return null;
    }

    public @Nullable Entry of(@Nonnull String str)
    {
        return new Entry(str);
    }

    public @Nullable Entry format(@Nonnull String str, Object... args)
    {
        return new Entry(str, args);
    }

    public @Nullable Entry translate(@Nonnull String str, Object... args)
    {
        Entry ent = new Entry(StringUtils.translate(str, args));
        ent.setTranslated();
        return ent;
    }

    protected String qt(@Nonnull String str, Object... args)
    {
        return StringUtils.translate(str, args);
    }

    public abstract boolean succeededType();

    public record Entry(@Nonnull String format, @Nullable Object... args)
    {
        private static boolean translated = false;

        void setTranslated()
        {
            translated = true;
        }

        public boolean isEmpty()
        {
            return this.format.isEmpty();
        }

        public boolean hasArgs()
        {
            return this.args != null && this.args.length > 0;
        }

        public boolean isTranslated()
        {
            return translated;
        }
    }
}
