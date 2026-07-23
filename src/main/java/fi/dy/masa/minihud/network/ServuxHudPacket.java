package fi.dy.masa.minihud.network;

import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import io.netty.buffer.Unpooled;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import fi.dy.masa.malilib.network.IClientPayloadData;
import fi.dy.masa.malilib.util.data.tag.BaseData;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.util.DataByteBufUtils;
import fi.dy.masa.minihud.MiniHUD;

public class ServuxHudPacket implements IClientPayloadData
{
	private Type packetType;
	private CompoundData nbt;
	private FriendlyByteBuf buffer;
	public static final int PROTOCOL_VERSION = 3;

	private ServuxHudPacket(Type type)
	{
		this.packetType = type;
		this.nbt = new CompoundData();
		this.clearPacket();
	}

	public static ServuxHudPacket MetadataRequest(@Nullable CompoundData nbt)
	{
		var packet = new ServuxHudPacket(Type.PACKET_C2S_METADATA_REQUEST);
		if (nbt != null)
		{
			packet.nbt.combine(nbt);
		}
		return packet;
	}

	public static ServuxHudPacket MetadataResponse(@Nullable CompoundData nbt)
	{
		var packet = new ServuxHudPacket(Type.PACKET_S2C_METADATA);
		if (nbt != null)
		{
			packet.nbt.combine(nbt);
		}
		return packet;
	}

	public static ServuxHudPacket UnregisterReply(@Nullable CompoundData nbt)
	{
		var packet = new ServuxHudPacket(Type.PACKET_C2S_UNREGISTER_REPLY);
		if (nbt != null)
		{
			packet.nbt.combine(nbt);
		}
		return packet;
	}

	public static ServuxHudPacket SpawnRequest(@Nullable CompoundData nbt)
	{
		var packet = new ServuxHudPacket(Type.PACKET_C2S_SPAWN_DATA_REQUEST);
		if (nbt != null)
		{
			packet.nbt.combine(nbt);
		}
		return packet;
	}

	public static ServuxHudPacket SpawnResponse(@Nullable CompoundData nbt)
	{
		var packet = new ServuxHudPacket(Type.PACKET_S2C_SPAWN_DATA);
		if (nbt != null)
		{
			packet.nbt.combine(nbt);
		}
		return packet;
	}

	public static ServuxHudPacket DataLoggerRequest(@Nullable CompoundData nbt)
	{
		var packet = new ServuxHudPacket(Type.PACKET_C2S_DATA_LOGGER_REQUEST);
		if (nbt != null)
		{
			packet.nbt.combine(nbt);
		}
		return packet;
	}

	public static ServuxHudPacket DataLoggerTick(@Nullable CompoundData nbt)
	{
		var packet = new ServuxHudPacket(Type.PACKET_S2C_DATA_LOGGER_TICK);
		if (nbt != null)
		{
			packet.nbt.combine(nbt);
		}
		return packet;
	}

	public static ServuxHudPacket WeatherTick(@Nullable CompoundData nbt)
	{
		var packet = new ServuxHudPacket(Type.PACKET_S2C_WEATHER_TICK);
		if (nbt != null)
		{
			packet.nbt.combine(nbt);
		}
		return packet;
	}

	public static ServuxHudPacket RecipeManagerRequest(@Nullable CompoundData nbt)
	{
		var packet = new ServuxHudPacket(Type.PACKET_C2S_RECIPE_MANAGER_REQUEST);
		if (nbt != null)
		{
			packet.nbt.combine(nbt);
		}
		return packet;
	}

	// Nbt Packet, using Packet Splitter
	public static ServuxHudPacket ResponseS2CStart(@Nonnull CompoundData nbt)
	{
		var packet = new ServuxHudPacket(Type.PACKET_S2C_NBT_RESPONSE_START);
		packet.nbt.combine(nbt);
		return packet;
	}

	public static ServuxHudPacket ResponseS2CData(@Nonnull FriendlyByteBuf buffer)
	{
		var packet = new ServuxHudPacket(Type.PACKET_S2C_NBT_RESPONSE_DATA);
		packet.buffer = new FriendlyByteBuf(buffer.copy());
		packet.nbt = new CompoundData();
		return packet;
	}

	private void clearPacket()
	{
		if (this.buffer != null)
		{
			this.buffer.clear();
			this.buffer = new FriendlyByteBuf(Unpooled.buffer());
		}
	}

	@Override
	public int getVersion()
	{
		return PROTOCOL_VERSION;
	}

	@Override
	public int getPacketType()
	{
		return this.packetType.get();
	}

	@Override
	public int getTotalSize()
	{
		int total = 2;

		if (this.nbt != null && !this.nbt.isEmpty())
		{
			total += this.nbt.sizeInBytes();
		}
		if (this.buffer != null)
		{
			total += this.buffer.readableBytes();
		}

		return total;
	}

	public Type getType()
	{
		return this.packetType;
	}

	public CompoundData getCompound()
	{
		return this.nbt;
	}

	public FriendlyByteBuf getBuffer()
	{
		return this.buffer;
	}

	public boolean hasBuffer() {return this.buffer != null && this.buffer.isReadable();}

	public boolean hasNbt() {return this.nbt != null && !this.nbt.isEmpty();}

	@Override
	public boolean isEmpty()
	{
		return !this.hasBuffer() && !this.hasNbt();
	}

	@Override
	public void toPacket(FriendlyByteBuf output)
	{
		output.writeVarInt(this.packetType.get());

		switch (this.packetType)
		{
			case PACKET_S2C_NBT_RESPONSE_DATA ->
			{
				// Write Packet Buffer (Slice)
				try
				{
					output.writeBytes(this.buffer.copy());
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxHudPacket#toPacket: error writing buffer data to packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_C2S_METADATA_REQUEST, PACKET_S2C_METADATA, PACKET_C2S_SPAWN_DATA_REQUEST, PACKET_S2C_SPAWN_DATA, PACKET_S2C_WEATHER_TICK, PACKET_C2S_RECIPE_MANAGER_REQUEST, PACKET_C2S_DATA_LOGGER_REQUEST, PACKET_S2C_DATA_LOGGER_TICK, PACKET_C2S_UNREGISTER_REPLY ->
			{
				// Write NBT
				try
				{
//                    output.writeNbt(this.nbt);
					DataByteBufUtils.toByteBuf(output, this.nbt, "");
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxHudPacket#toPacket: error writing NBT to packet: [{}]", e.getLocalizedMessage());
				}
			}
			default -> MiniHUD.LOGGER.error("ServuxHudPacket#toPacket: Unknown packet type!");
		}
	}

	@Nullable
	public static ServuxHudPacket fromPacket(FriendlyByteBuf input)
	{
		int i = input.readVarInt();
		Type type = getType(i);

		if (type == null)
		{
			// Invalid Type
			MiniHUD.LOGGER.warn("ServuxHudPacket#fromPacket: invalid packet type received");
			return null;
		}
		switch (type)
		{
			case PACKET_S2C_NBT_RESPONSE_DATA ->
			{
				// Read Packet Buffer Slice
				try
				{
					return ServuxHudPacket.ResponseS2CData(new FriendlyByteBuf(input.readBytes(input.readableBytes())));
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxHudPacket#fromPacket: error reading S2C Bulk Response Buffer from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_C2S_METADATA_REQUEST ->
			{
				// Read Nbt
				try
				{
					Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(input);
//                    return ServuxHudPacket.MetadataRequest(input.readNbt());
					if (opt.isPresent())
					{
						return ServuxHudPacket.MetadataRequest((CompoundData) opt.get());
					}
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxHudPacket#fromPacket: error reading Metadata Request from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_S2C_METADATA ->
			{
				// Read Nbt
				try
				{
					Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(input);
//                    return ServuxHudPacket.MetadataResponse(input.readNbt());
					if (opt.isPresent())
					{
						return ServuxHudPacket.MetadataResponse((CompoundData) opt.get());
					}
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxHudPacket#fromPacket: error reading Metadata Response from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_C2S_SPAWN_DATA_REQUEST ->
			{
				// Read Nbt
				try
				{
					Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(input);
//                    return ServuxHudPacket.SpawnRequest(input.readNbt());
					if (opt.isPresent())
					{
						return ServuxHudPacket.SpawnRequest((CompoundData) opt.get());
					}
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxHudPacket#fromPacket: error reading Spawn Data Request from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_S2C_SPAWN_DATA ->
			{
				// Read Nbt
				try
				{
					Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(input);
//                    return ServuxHudPacket.SpawnResponse(input.readNbt());
					if (opt.isPresent())
					{
						return ServuxHudPacket.SpawnResponse((CompoundData) opt.get());
					}
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxHudPacket#fromPacket: error reading Spawn Data Response from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_C2S_DATA_LOGGER_REQUEST ->
			{
				// Read Nbt
				try
				{
					Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(input);
//                    return ServuxHudPacket.DataLoggerRequest(input.readNbt());
					if (opt.isPresent())
					{
						return ServuxHudPacket.DataLoggerRequest((CompoundData) opt.get());
					}
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxHudPacket#fromPacket: error reading Data Logger Request from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_S2C_DATA_LOGGER_TICK ->
			{
				// Read Nbt
				try
				{
					Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(input);
//                    return ServuxHudPacket.DataLoggerTick(input.readNbt());
					if (opt.isPresent())
					{
						return ServuxHudPacket.DataLoggerTick((CompoundData) opt.get());
					}
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxHudPacket#fromPacket: error reading Data Logger Tick from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_S2C_WEATHER_TICK ->
			{
				// Read Nbt
				try
				{
					Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(input);
//                    return ServuxHudPacket.WeatherTick(input.readNbt());
					if (opt.isPresent())
					{
						return ServuxHudPacket.WeatherTick((CompoundData) opt.get());
					}
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxHudPacket#fromPacket: error reading Weather Tick from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_C2S_RECIPE_MANAGER_REQUEST ->
			{
				// Read Nbt
				try
				{
					Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(input);
//                    return ServuxHudPacket.RecipeManagerRequest(input.readNbt());
					if (opt.isPresent())
					{
						return ServuxHudPacket.RecipeManagerRequest((CompoundData) opt.get());
					}
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxHudPacket#fromPacket: error reading Recipe Request from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_C2S_UNREGISTER_REPLY ->
			{
				// Read Nbt
				try
				{
					Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(input);
//                    return ServuxHudPacket.UnregisterReply(input.readNbt());
					if (opt.isPresent())
					{
						return ServuxHudPacket.UnregisterReply((CompoundData) opt.get());
					}
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxHudPacket#fromPacket: error reading Unregister Reply from packet: [{}]", e.getLocalizedMessage());
				}
			}
			default -> MiniHUD.LOGGER.error("ServuxHudPacket#fromPacket: Unknown packet type!");
		}

		return null;
	}

	@Override
	public void clear()
	{
		if (this.nbt != null && !this.nbt.isEmpty())
		{
			this.nbt = new CompoundData();
		}
		this.clearPacket();
		this.packetType = null;
	}

	@Nullable
	public static Type getType(int input)
	{
		for (Type type : Type.values())
		{
			if (type.get() == input)
			{
				return type;
			}
		}

		return null;
	}

	public enum Type
	{
		PACKET_S2C_METADATA(1),
		PACKET_C2S_METADATA_REQUEST(2),
		PACKET_S2C_SPAWN_DATA(3),
		PACKET_C2S_SPAWN_DATA_REQUEST(4),
		PACKET_S2C_WEATHER_TICK(5),
		PACKET_C2S_RECIPE_MANAGER_REQUEST(6),
		PACKET_S2C_DATA_LOGGER_TICK(7),
		PACKET_C2S_DATA_LOGGER_REQUEST(8),
		PACKET_C2S_UNREGISTER_REPLY(9),
		// For Packet Splitter (Oversize Packets, S2C)
		PACKET_S2C_NBT_RESPONSE_START(10),
		PACKET_S2C_NBT_RESPONSE_DATA(11);

		private final int type;

		Type(int type)
		{
			this.type = type;
		}

		int get() {return this.type;}
	}

	public record Payload(ServuxHudPacket data) implements CustomPacketPayload
	{
		public static final CustomPacketPayload.Type<Payload> ID = new CustomPacketPayload.Type<>(ServuxHudHandler.CHANNEL_ID);
		public static final StreamCodec<FriendlyByteBuf, Payload> CODEC = CustomPacketPayload.codec(Payload::write, Payload::new);

		public Payload(FriendlyByteBuf input)
		{
			this(fromPacket(input));
		}

		private void write(FriendlyByteBuf output)
		{
			data.toPacket(output);
		}

		@Override
		public @Nonnull CustomPacketPayload.Type<? extends CustomPacketPayload> type()
		{
			return ID;
		}
	}
}
