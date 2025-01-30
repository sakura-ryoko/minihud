package fi.dy.masa.minihud.info;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.data.EntitiesDataManager;
import fi.dy.masa.minihud.data.HudDataManager;
import fi.dy.masa.minihud.util.DataStorage;

public abstract class InfoLine
{
    protected static final String REMAINING_KEY = Reference.MOD_ID+".info_line.remaining";
    private final InfoToggle type;

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

    public @Nullable Entry parse(@Nonnull Context ctx)
    {
        return null;
    }

    public @Nullable Entry parseNbt(@Nonnull World world, @Nonnull EntityType<?> entityType, @Nonnull NbtCompound nbt)
    {
        return null;
    }

    public @Nullable Entry parseNbt(@Nonnull World world, @Nonnull BlockEntityType<?> beType, @Nonnull NbtCompound nbt)
    {
        return null;
    }

    public @Nullable Entry parseEnt(@Nonnull World world, @Nonnull Entity ent)
    {
        return null;
    }

    public @Nullable Entry parseBlockEnt(@Nonnull World world, @Nonnull BlockEntity be)
    {
        return null;
    }

    public @Nullable Entry parseBlock(@Nonnull World world, @Nonnull Block block)
    {
        return null;
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

    public record Context(@Nonnull World world, @Nullable Entity ent, @Nullable BlockEntity be, @Nullable Block block, NbtCompound nbt)
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

        public boolean hasBlock()
        {
            return this.block != null && this.block instanceof Block;
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
