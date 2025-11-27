package fi.dy.masa.minihud.util;

import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import net.minecraft.SharedConstants;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.StringIdentifiable;

import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.minihud.config.RendererToggle;

public enum DebugRenderType implements StringIdentifiable
{
	DEBUG_ENABLED             ("debug_enabled",         "MC_DEBUG_ENABLED", RendererToggle.DEBUG_DATA_MAIN_TOGGLE),
	PATHFINDING               ("pathfinding",           "MC_DEBUG_PATHFINDING", RendererToggle.DEBUG_PATH_FINDING),
	NEIGHBOR_UPDATES          ("neighbor_updates",      "MC_DEBUG_NEIGHBORSUPDATE", RendererToggle.DEBUG_NEIGHBOR_UPDATES),
	REDSTONE_WIRE_UPDATE_ORDER("redstone_update_order", "MC_DEBUG_EXPERIMENTAL_REDSTONEWIRE_UPDATE_ORDER", RendererToggle.DEBUG_REDSTONE_UPDATE_ORDER),
	STRUCTURES                ("structures",            "MC_DEBUG_STRUCTURES", RendererToggle.DEBUG_STRUCTURES),
	GAME_EVENT_LISTENERS      ("game_event_listeners",  "MC_DEBUG_GAME_EVENT_LISTENERS", RendererToggle.DEBUG_GAME_EVENT),
	GOAL_SELECTOR             ("goal_selector",         "MC_DEBUG_GOAL_SELECTOR", RendererToggle.DEBUG_GOAL_SELECTOR),
	VILLAGE_SECTIONS          ("village_sections",      "MC_DEBUG_VILLAGE_SECTIONS", RendererToggle.DEBUG_VILLAGE_SECTIONS),
	BRAIN                     ("brain",                 "MC_DEBUG_BRAIN", RendererToggle.DEBUG_BRAIN),
	POI                       ("poi",                   "MC_DEBUG_POI", RendererToggle.DEBUG_POI),
	BEES                      ("bees",                  "MC_DEBUG_BEES", RendererToggle.DEBUG_BEEDATA),
	RAIDS                     ("raids",                 "MC_DEBUG_RAIDS", RendererToggle.DEBUG_RAID_CENTER),
	BREEZE                    ("breeze",                "MC_DEBUG_BREEZE_MOB", RendererToggle.DEBUG_BREEZE_JUMP),
	ENTITY_BLOCK_INTERSECTION ("entity_block_intersect","MC_DEBUG_ENTITY_BLOCK_INTERSECTION", RendererToggle.DEBUG_ENTITY_BLOCK_INTERSECTION),
	;

	public static final StringIdentifiable.EnumCodec<DebugRenderType> CODEC = StringIdentifiable.createCodec(DebugRenderType::values);
	public static final PacketCodec<ByteBuf, DebugRenderType> PACKET_CODEC = PacketCodecs.STRING.xmap(DebugRenderType::fromStringStatic, DebugRenderType::asString);
	public static final ImmutableList<@NotNull DebugRenderType> VALUES = ImmutableList.copyOf(values());

	private final String name;
	private final String property;
	private final RendererToggle callback;

	DebugRenderType(String name, String property, RendererToggle callback)
	{
		this.name = name;
		this.property = property;
		this.callback = callback;
	}

	@Override
	public String asString()
	{
		return this.getName();
	}

	public String getName()
	{
		return this.name;
	}

	public String getProperty()
	{
		return this.property;
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
			case "pathfinding" -> SharedConstants.PATHFINDING = toggle;
			case "neighbor_updates" -> SharedConstants.NEIGHBORSUPDATE = toggle;
			case "redstone_update_order" -> SharedConstants.EXPERIMENTAL_REDSTONEWIRE_UPDATE_ORDER = toggle;
			case "structures" -> SharedConstants.STRUCTURES = toggle;
			case "game_event_listeners" -> SharedConstants.GAME_EVENT_LISTENERS = toggle;
			case "goal_selector" -> SharedConstants.GOAL_SELECTOR = toggle;
			case "village_sections" -> SharedConstants.VILLAGE_SECTIONS = toggle;
			case "brain" -> SharedConstants.BRAIN = toggle;
			case "poi" -> SharedConstants.POI = toggle;
			case "bees" -> SharedConstants.BEES = toggle;
			case "raids" -> SharedConstants.RAIDS = toggle;
			case "breeze" -> SharedConstants.BREEZE_MOB = toggle;
			case "entity_block_intersect" -> SharedConstants.ENTITY_BLOCK_INTERSECTION = toggle;
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

	public static @Nullable DebugRenderType fromPropertyStatic(String property)
	{
		for (DebugRenderType val : VALUES)
		{
			if (val.property.equalsIgnoreCase(property))
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
