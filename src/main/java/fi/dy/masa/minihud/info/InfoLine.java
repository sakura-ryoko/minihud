package fi.dy.masa.minihud.info;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.WorldUtils;
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

    public MinecraftClient mc() { return MinecraftClient.getInstance(); }

    public World getBestWorld()
    {
        return WorldUtils.getBestWorld(this.mc());
    }

    public World getClientWorld()
    {
        return this.mc().world;
    }

    public List<Entry> parse(@Nonnull Context ctx)
    {
        return null;
    }

    public List<Entry> parseNbt(@Nonnull World world, @Nonnull EntityType<?> entityType, @Nonnull NbtCompound nbt)
    {
        return null;
    }

    public List<Entry> parseNbt(@Nonnull World world, @Nonnull BlockEntityType<?> beType, @Nonnull NbtCompound nbt)
    {
        return null;
    }

    public List<Entry> parseEnt(@Nonnull World world, @Nonnull Entity ent)
    {
        return null;
    }

    public List<Entry> parseBlockEnt(@Nonnull World world, @Nonnull BlockEntity be)
    {
        return null;
    }

    public List<Entry> parseBlockPos(@Nonnull World world, @Nonnull BlockPos pos)
    {
        return null;
    }

    public List<Entry> parseBlockState(@Nonnull World world, @Nonnull BlockState state)
    {
        return null;
    }

    public List<Entry> parseWorld(@Nonnull World world)
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

    public record Context(@Nullable World world, @Nullable Entity ent, @Nullable BlockEntity be, @Nullable BlockPos pos, @Nullable BlockState state, NbtCompound nbt)
    {
        public boolean hasEntity()
        {
            return this.ent != null && this.ent instanceof Entity;
        }

        public boolean hasLiving()
        {
            return this.ent != null && this.ent instanceof LivingEntity;
        }

        public @Nullable LivingEntity living()
        {
            if (this.hasLiving())
            {
                return (LivingEntity) this.ent;
            }

            return null;
        }

        public boolean hasBlockEntity()
        {
            return this.be != null && this.be instanceof BlockEntity;
        }

        public boolean hasBlockPos()
        {
            return this.pos != null;
        }

        public boolean hasBlockState()
        {
            return this.state != null && this.state instanceof BlockState;
        }

        public boolean hasNbt()
        {
            return this.nbt != null && !this.nbt.isEmpty();
        }
    }

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
