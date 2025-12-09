package fi.dy.masa.minihud.util;

import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import net.minecraft.SharedConstants;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.minihud.config.RendererToggle;

public enum DebugRenderType implements StringRepresentable
{
	DEBUG_ENABLED             ("debug_enabled",         RendererToggle.DEBUG_DATA_MAIN_TOGGLE),
	PATHFINDING               ("pathfinding",           RendererToggle.DEBUG_PATH_FINDING),
	NEIGHBOR_UPDATES          ("neighbor_updates",      RendererToggle.DEBUG_NEIGHBOR_UPDATES),
	REDSTONE_WIRE_UPDATE_ORDER("redstone_update_order", RendererToggle.DEBUG_REDSTONE_UPDATE_ORDER),
	STRUCTURES                ("structures",            RendererToggle.DEBUG_STRUCTURES),
	GAME_EVENT_LISTENERS      ("game_event_listeners",  RendererToggle.DEBUG_GAME_EVENT),
	GOAL_SELECTOR             ("goal_selector",         RendererToggle.DEBUG_GOAL_SELECTOR),
	VILLAGE_SECTIONS          ("village_sections",      RendererToggle.DEBUG_VILLAGE_SECTIONS),
	BRAIN                     ("brain",                 RendererToggle.DEBUG_BRAIN),
	POI                       ("poi",                   RendererToggle.DEBUG_POI),
	BEES                      ("bees",                  RendererToggle.DEBUG_BEEDATA),
	RAIDS                     ("raids",                 RendererToggle.DEBUG_RAID_CENTER),
	BREEZE                    ("breeze",                RendererToggle.DEBUG_BREEZE_JUMP),
	ENTITY_BLOCK_INTERSECTION ("entity_block_intersect",RendererToggle.DEBUG_ENTITY_BLOCK_INTERSECTION),
	// 1.21.10-Only
//	WATER                     ("water",                 RendererToggle.DEBUG_WATER),
//	HEIGHTMAP                 ("heightmap",             RendererToggle.DEBUG_HEIGHTMAP),
//	COLLISION_BOXES           ("collision_boxes",       RendererToggle.DEBUG_COLLISION_BOXES),
//	SUPPORTING_BLOCK          ("supporting_block",      RendererToggle.DEBUG_SUPPORTING_BLOCK),
//	LIGHT                     ("light",                 RendererToggle.DEBUG_LIGHT),
//	CHUNK_LOADING             ("chunk_loading",         RendererToggle.DEBUG_CHUNK_LOADING),
//	SKYLIGHT_SECTIONS         ("skylight_sections",     RendererToggle.DEBUG_SKYLIGHT_SECTIONS),
	;

	public static final StringRepresentable.EnumCodec<DebugRenderType> CODEC = StringRepresentable.fromEnum(DebugRenderType::values);
	public static final StreamCodec<ByteBuf, DebugRenderType> PACKET_CODEC = ByteBufCodecs.STRING_UTF8.map(DebugRenderType::fromStringStatic, DebugRenderType::getSerializedName);
	public static final ImmutableList<@NotNull DebugRenderType> VALUES = ImmutableList.copyOf(values());

	private final String name;
	private final RendererToggle callback;

	DebugRenderType(String name, RendererToggle callback)
	{
		this.name = name;
		this.callback = callback;
	}

	@Override
	public @NonNull String getSerializedName()
	{
		return this.getName();
	}

	public String getName()
	{
		return this.name;
	}

	public RendererToggle getCallback()
	{
		return this.callback;
	}

	@ApiStatus.Internal
	public void toggleSharedConstant(boolean toggle)
	{
		switch (this.name.toLowerCase())
		{
			case "debug_enabled" -> SharedConstants.DEBUG_ENABLED = toggle;
			case "pathfinding" -> SharedConstants.DEBUG_PATHFINDING = toggle;
			case "neighbor_updates" -> SharedConstants.DEBUG_NEIGHBORSUPDATE = toggle;
			case "redstone_update_order" -> SharedConstants.DEBUG_EXPERIMENTAL_REDSTONEWIRE_UPDATE_ORDER = toggle;
			case "structures" -> SharedConstants.DEBUG_STRUCTURES = toggle;
			case "game_event_listeners" -> SharedConstants.DEBUG_GAME_EVENT_LISTENERS = toggle;
			case "goal_selector" -> SharedConstants.DEBUG_GOAL_SELECTOR = toggle;
			case "village_sections" -> SharedConstants.DEBUG_VILLAGE_SECTIONS = toggle;
			case "brain" -> SharedConstants.DEBUG_BRAIN = toggle;
			case "poi" -> SharedConstants.DEBUG_POI = toggle;
			case "bees" -> SharedConstants.DEBUG_BEES = toggle;
			case "raids" -> SharedConstants.DEBUG_RAIDS = toggle;
			case "breeze" -> SharedConstants.DEBUG_BREEZE_MOB = toggle;
			case "entity_block_intersect" -> SharedConstants.DEBUG_ENTITY_BLOCK_INTERSECTION = toggle;
			// 1.21.10-Only
//			case "water" -> SharedConstants.WATER = toggle;
//			case "heightmap" -> SharedConstants.HEIGHTMAP = toggle;
//			case "collision_boxes" -> SharedConstants.COLLISION = toggle;
//			case "supporting_block" -> SharedConstants.SUPPORT_BLOCKS = toggle;
//			case "light" -> SharedConstants.LIGHT = toggle;
////			case "block_outline" -> SharedConstants.BLOCK_ = toggle;
//			case "chunk_loading" -> SharedConstants.CHUNKS = toggle;
//			case "skylight_sections" -> SharedConstants.SKY_LIGHT_SECTIONS = toggle;
		}
	}

	public @Nullable DebugRenderType fromString(String str)
	{
		return fromStringStatic(str);
	}

	public static @Nullable DebugRenderType fromStringStatic(String name)
	{
		for (DebugRenderType val : VALUES)
		{
			if (val.name.equalsIgnoreCase(name))
			{
				return val;
			}
		}

		return null;
	}

	public static @Nullable DebugRenderType fromCallbackStatic(IConfigBoolean config)
	{
		for (DebugRenderType val : VALUES)
		{
			if (val.callback.getName().equalsIgnoreCase(config.getName()))
			{
				return val;
			}
		}

		return null;
	}
}
