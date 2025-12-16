package fi.dy.masa.minihud.util;

import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.StructureTerrainAdaptation;
import net.minecraft.world.gen.structure.Structure;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;

import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;

public class StructureData
{
	private final StructureType type;
    private final IntBoundingBox mainBox;
    private final ImmutableList<@NotNull IntBoundingBox> componentBoxes;
    private long refreshTime;
	@Nullable
	private final StructureStart vanilla;

	private StructureData(StructureType type, ImmutableList<@NotNull IntBoundingBox> componentBoxes,
	                      long refreshTime, boolean shouldExpandBox)
	{
		this.type = type;
		this.vanilla = null;
//		this.mainBox = encompass(componentBoxes, this.shouldExpandBox(structure));
        this.mainBox = encompass(componentBoxes, shouldExpandBox);
		this.componentBoxes = componentBoxes;
		this.refreshTime = refreshTime;
	}

    private StructureData(StructureType type, ImmutableList<@NotNull IntBoundingBox> componentBoxes,
                          StructureStart structureStart)
    {
	    this.type = type;
	    this.vanilla = structureStart;
	    this.mainBox = IntBoundingBox.fromVanillaBox(structureStart.getBoundingBox());
	    this.componentBoxes = componentBoxes;
    }

    public StructureType getStructureType()
    {
        return this.type;
    }

	@Nullable
	public StructureStart toVanilla() { return this.vanilla; }

	public boolean shouldExpandBox(Structure structure)
	{
		return structure.getTerrainAdaptation() != StructureTerrainAdaptation.NONE;
	}

    public IntBoundingBox getBoundingBox()
    {
        return this.mainBox;
    }

    public ImmutableList<@NotNull IntBoundingBox> getComponents()
    {
        return this.componentBoxes;
    }

    public long getRefreshTime()
    {
        return this.refreshTime;
    }

    public static StructureData fromStructureStart(StructureType type, StructureStart structure)
    {
        ImmutableList.Builder<@NotNull IntBoundingBox> builder = ImmutableList.builder();
        List<StructurePiece> components = structure.getChildren();

        for (StructurePiece component : components)
        {
            builder.add(IntBoundingBox.fromVanillaBox(component.getBoundingBox()));
        }

        return new StructureData(type, builder.build(), structure);
    }

    @Nullable
    public static StructureData fromStructureStartTag(NbtCompound tag, long currentTime)
    {
        if (tag.contains("id") &&
            tag.contains("Children"))
        {
			String id = tag.getString("id", "?");
            StructureType type = StructureType.fromStructureId(id);

            if (type == StructureType.UNKNOWN && Configs.Generic.DEBUG_MESSAGES.getBooleanValue())
            {
                MiniHUD.LOGGER.warn("StructureData.fromStructureStartTag(): Unknown structure type '{}'", id);
            }

            try
            {
//                Structure structure = DataStorage.getInstance().getWorldRegistryManager().getOrThrow(RegistryKeys.STRUCTURE).get(Identifier.tryParse(id));
//                final int ref = tag.getInt("references", 0);
//                ChunkPos pos = new ChunkPos(tag.getInt("ChunkX", 0), tag.getInt("ChunkZ", 0));
                ImmutableList.Builder<@NotNull IntBoundingBox> builder = ImmutableList.builder();
                NbtList pieces = tag.getListOrEmpty("Children");
                boolean shouldExpandBox = tag.getBoolean("ExpandBox", false);
                final int count = pieces.size();

                for (int i = 0; i < count; ++i)
                {
                    NbtCompound pieceTag = pieces.getCompoundOrEmpty(i);
                    builder.add(IntBoundingBox.fromArray(pieceTag.getIntArray("BB").orElseThrow()));
                }

                return new StructureData(type, builder.build(), currentTime, shouldExpandBox);
            }
            catch (Exception e)
            {
                MiniHUD.LOGGER.warn("StructureData.fromStructureStartTag(): Failed to parse structure [{}] data; {}", id, e.getLocalizedMessage());
            }
        }

        return null;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.componentBoxes == null) ? 0 : this.componentBoxes.hashCode());
        result = prime * result + ((this.mainBox == null) ? 0 : this.mainBox.hashCode());
        result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj == null || this.getClass() != obj.getClass())
        {
            return false;
        }

        StructureData other = (StructureData) obj;

        if (this.componentBoxes == null)
        {
            if (other.componentBoxes != null)
            {
                return false;
            }
        }
        else if (! this.componentBoxes.equals(other.componentBoxes))
        {
            return false;
        }

        if (this.mainBox == null)
        {
            if (other.mainBox != null)
            {
                return false;
            }
        }
        else if (! this.mainBox.equals(other.mainBox))
        {
            return false;
        }

        return this.type == other.type;
    }

    public static IntBoundingBox encompass(Iterable<IntBoundingBox> boxes, boolean expandBox)
    {
        Iterator<IntBoundingBox> iterator = boxes.iterator();

        if (iterator.hasNext())
        {
            IntBoundingBox box = iterator.next();
            int minX = box.minX;
            int minY = box.minY;
            int minZ = box.minZ;
            int maxX = box.maxX;
            int maxY = box.maxY;
            int maxZ = box.maxZ;

            while (iterator.hasNext())
            {
                box = iterator.next();
                minX = Math.min(minX, box.minX);
                minY = Math.min(minY, box.minY);
                minZ = Math.min(minZ, box.minZ);
                maxX = Math.max(maxX, box.maxX);
                maxY = Math.max(maxY, box.maxY);
                maxZ = Math.max(maxZ, box.maxZ);
            }

            IntBoundingBox bb = new IntBoundingBox(minX, minY, minZ, maxX, maxY, maxZ);

			// Vanilla says to expand it if != StructureTerrainAdaptation.NONE
			if (expandBox)
			{
				bb.expand(12);
			}

			return bb;
        }

        return new IntBoundingBox(0, 0, 0, 0, 0, 0);
    }
}
