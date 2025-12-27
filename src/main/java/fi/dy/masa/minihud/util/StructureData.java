package fi.dy.masa.minihud.util;

import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;

import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;

public class StructureData
{
    private static final int expandBoxAmount = 12;
	private final StructureType type;
    private final ImmutableList<@NotNull IntBoundingBox> componentBoxes;
    private IntBoundingBox mainBox;
    private long refreshTime;
	@Nullable
	private final StructureStart vanilla;
    private final boolean expandBox;

	private StructureData(StructureType type, ImmutableList<@NotNull IntBoundingBox> componentBoxes,
	                      long refreshTime, boolean shouldExpandBox)
	{
		this.type = type;
		this.vanilla = null;
        this.mainBox = encompass(componentBoxes, shouldExpandBox);
		this.componentBoxes = componentBoxes;
		this.refreshTime = refreshTime;
        this.expandBox = shouldExpandBox;
	}

    private StructureData(StructureType type, ImmutableList<@NotNull IntBoundingBox> componentBoxes,
                          StructureStart structureStart)
    {
	    this.type = type;
	    this.vanilla = structureStart;
	    this.mainBox = IntBoundingBox.fromVanillaBox(structureStart.getBoundingBox());
	    this.componentBoxes = componentBoxes;
        this.expandBox = this.shouldExpandBox(structureStart.getStructure());
        this.fixMainBox(componentBoxes, this.mainBox, this.expandBox);
    }

    public StructureType getStructureType()
    {
        return this.type;
    }

	@Nullable
	public StructureStart toVanilla() { return this.vanilla; }

	public boolean shouldExpandBox(Structure structure)
	{
		return structure.terrainAdaptation() != TerrainAdjustment.NONE;
	}

    /**
     * This is needed when your generating structures for the
     * first time, and Vanilla reports it at the wrong Y level.
     * This only is in effect when getting data from the Integrated Server.
     */
    public void fixMainBox(ImmutableList<@NotNull IntBoundingBox> componentBoxes, IntBoundingBox mainBox, boolean shouldExpand)
    {
        final IntBoundingBox box = encompass(componentBoxes, shouldExpand);

        // Fix when the Vanilla Box is the right size, but in the wrong place,
        // such as floating 30 blocks higher than it should.
        // This happens because most structures pre-gen at Y = 90
        // before the terrain is generated.
        // Then the structure is adapted to
        // the Terrain / Heightmap afterward.
        if (!mainBox.intersects(box))
        {
            int xDistA = mainBox.maxX() - mainBox.minX();
            int yDistA = mainBox.maxY() - mainBox.minY();
            int zDistA = mainBox.maxZ() - mainBox.minZ();
            int xDistB = box.maxX() - box.minX();
            int yDistB = box.maxY() - box.minY();
            int zDistB = box.maxZ() - box.minZ();

            // We use Math.min() here because chances are,
            // `mainBox` is well above the encompassBox.
            IntBoundingBox fixed = new IntBoundingBox(
                    Math.min(box.minX(), mainBox.minX()),
                    Math.min(box.minY(), mainBox.minY()),
                    Math.min(box.minZ(), mainBox.minZ()),
                    Math.min(box.maxX(), mainBox.maxX()),
                    Math.min(box.maxY(), mainBox.maxY()),
                    Math.min(box.maxZ(), mainBox.maxZ())
            );

            if (xDistA != xDistB || yDistA != yDistB || zDistA != zDistB)
            {
                // Mainly used for minor corrections; such as a (y - 4).
                fixed = fixed.expand(xDistA - xDistB,
                                     yDistA - yDistB,
                                     zDistA - zDistB
                );
            }

            if (shouldExpand)
            {
                fixed = fixed.expand(expandBoxAmount);
            }

            this.mainBox = fixed;
        }
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
        List<StructurePiece> components = structure.getPieces();

        for (StructurePiece component : components)
        {
            builder.add(IntBoundingBox.fromVanillaBox(component.getBoundingBox()));
        }

        return new StructureData(type, builder.build(), structure);
    }

    @Nullable
    public static StructureData fromStructureStartTag(CompoundTag tag, long currentTime)
    {
        if (tag.contains("id") &&
            tag.contains("Children"))
        {
            String id = tag.getStringOr("id", "?");
            StructureType type = StructureType.fromStructureId(id);

            if (type == StructureType.UNKNOWN && Configs.Generic.DEBUG_MESSAGES.getBooleanValue())
            {
                MiniHUD.LOGGER.warn("StructureData.fromStructureStartTag(): Unknown structure type '{}'", id);
            }

            try
            {
//                Structure structure = DataStorage.getInstance().getWorldRegistryManager().lookupOrThrow(Registries.STRUCTURE).getValue(Identifier.tryParse(id));
//                Structure structure = WorldUtils.getBestWorld(Minecraft.getInstance()).registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(Identifier.tryParse(id));

//                if (structure == null) return null;
//			    final int ref = tag.getInt("references", 0);
//			    ChunkPos pos = new ChunkPos(tag.getInt("ChunkX", 0), tag.getInt("ChunkZ", 0));
                ImmutableList.Builder<@NotNull IntBoundingBox> builder = ImmutableList.builder();
                ListTag pieces = tag.getListOrEmpty("Children");
                boolean shouldExpandBox = tag.getBooleanOr("ExpandBox", false);
                final int count = pieces.size();

                for (int i = 0; i < count; ++i)
                {
                    CompoundTag pieceTag = pieces.getCompoundOrEmpty(i);
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
            int minX = box.minX();
            int minY = box.minY();
            int minZ = box.minZ();
            int maxX = box.maxX();
            int maxY = box.maxY();
            int maxZ = box.maxZ();

            while (iterator.hasNext())
            {
                box = iterator.next();
                minX = Math.min(minX, box.minX());
                minY = Math.min(minY, box.minY());
                minZ = Math.min(minZ, box.minZ());
                maxX = Math.max(maxX, box.maxX());
                maxY = Math.max(maxY, box.maxY());
                maxZ = Math.max(maxZ, box.maxZ());
            }

            IntBoundingBox bb = new IntBoundingBox(minX, minY, minZ, maxX, maxY, maxZ);

			// Vanilla says to expand it if != StructureTerrainAdaptation.NONE
			if (expandBox)
			{
				bb.expand(expandBoxAmount);
			}

			return bb;
        }

        return new IntBoundingBox(0, 0, 0, 0, 0, 0);
    }
}
