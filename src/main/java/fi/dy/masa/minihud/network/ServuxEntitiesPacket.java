package fi.dy.masa.minihud.network;

import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import io.netty.buffer.Unpooled;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import fi.dy.masa.malilib.network.IClientPayloadData;
import fi.dy.masa.malilib.util.data.tag.BaseData;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.converter.DataConverterNbt;
import fi.dy.masa.malilib.util.data.tag.util.DataByteBufUtils;
import fi.dy.masa.minihud.MiniHUD;

public class ServuxEntitiesPacket implements IClientPayloadData
{
	private Type packetType;
	private int entityId;
	private BlockPos pos;
	private CompoundData nbt;
	private FriendlyByteBuf buffer;
	public static final int PROTOCOL_VERSION = 2;

	private ServuxEntitiesPacket(Type type)
	{
		this.packetType = type;
		this.entityId = -1;
		this.pos = BlockPos.ZERO;
		this.nbt = new CompoundData();
		this.clearPacket();
	}

	public static ServuxEntitiesPacket MetadataRequest(@Nullable CompoundData nbt)
	{
		var packet = new ServuxEntitiesPacket(Type.PACKET_C2S_METADATA_REQUEST);
		if (nbt != null)
		{
			packet.nbt.combine(nbt);
		}
		return packet;
	}

	public static ServuxEntitiesPacket MetadataResponse(@Nullable CompoundData nbt)
	{
		var packet = new ServuxEntitiesPacket(Type.PACKET_S2C_METADATA);
		if (nbt != null)
		{
			packet.nbt.combine(nbt);
		}
		return packet;
	}

	public static ServuxEntitiesPacket UnregisterReply(@Nullable CompoundData nbt)
	{
		var packet = new ServuxEntitiesPacket(Type.PACKET_C2S_UNREGISTER_REPLY);
		if (nbt != null)
		{
			packet.nbt.combine(nbt);
		}
		return packet;
	}

	// Entity simple response
	public static ServuxEntitiesPacket SimpleEntityResponse(int entityId, @Nullable CompoundData nbt)
	{
		var packet = new ServuxEntitiesPacket(Type.PACKET_S2C_ENTITY_NBT_RESPONSE_SIMPLE);
		if (nbt != null)
		{
			packet.nbt.combine(nbt);
		}
		packet.entityId = entityId;
		return packet;
	}

	public static ServuxEntitiesPacket SimpleBlockResponse(BlockPos pos, @Nullable CompoundData nbt)
	{
		var packet = new ServuxEntitiesPacket(Type.PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE);
		if (nbt != null)
		{
			packet.nbt.combine(nbt);
		}
		packet.pos = pos.immutable();
		return packet;
	}

	public static ServuxEntitiesPacket BlockEntityRequest(BlockPos pos)
	{
		var packet = new ServuxEntitiesPacket(Type.PACKET_C2S_BLOCK_ENTITY_REQUEST);
		packet.pos = pos.immutable();
		return packet;
	}

	public static ServuxEntitiesPacket EntityRequest(int entityId)
	{
		var packet = new ServuxEntitiesPacket(Type.PACKET_C2S_ENTITY_REQUEST);
		packet.entityId = entityId;
		return packet;
	}

	// Nbt Packet, using Packet Splitter
	public static ServuxEntitiesPacket ResponseS2CStart(@Nonnull CompoundData nbt)
	{
		var packet = new ServuxEntitiesPacket(Type.PACKET_S2C_NBT_RESPONSE_START);
		packet.nbt.combine(nbt);
		return packet;
	}

	public static ServuxEntitiesPacket ResponseS2CData(@Nonnull FriendlyByteBuf buffer)
	{
		var packet = new ServuxEntitiesPacket(Type.PACKET_S2C_NBT_RESPONSE_DATA);
		packet.buffer = new FriendlyByteBuf(buffer.copy());
		packet.nbt = new CompoundData();
		return packet;
	}

	public static ServuxEntitiesPacket ResponseC2SStart(@Nonnull CompoundData nbt)
	{
		var packet = new ServuxEntitiesPacket(Type.PACKET_C2S_NBT_RESPONSE_START);
		packet.nbt.combine(nbt);
		return packet;
	}

	public static ServuxEntitiesPacket ResponseC2SData(@Nonnull FriendlyByteBuf buffer)
	{
		var packet = new ServuxEntitiesPacket(Type.PACKET_C2S_NBT_RESPONSE_DATA);
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

	public int getEntityId() {return this.entityId;}

	public BlockPos getPos() {return this.pos;}

	public CompoundData getCompound()
	{
		return this.nbt;
	}

	@Deprecated
	private static CompoundData fromVanilla(CompoundTag nbt)
	{
		if (nbt != null && !nbt.isEmpty())
		{
			return DataConverterNbt.fromVanillaCompound(nbt);
		}

		return new CompoundData();
	}

	@Deprecated
	private CompoundTag toVanilla()
	{
		if (this.nbt != null && !this.nbt.isEmpty())
		{
			return DataConverterNbt.toVanillaCompound(this.nbt);
		}

		return new CompoundTag();
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
			case PACKET_C2S_BLOCK_ENTITY_REQUEST ->
			{
				// Write BE Request
				try
				{
					output.writeBlockPos(this.pos);
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxEntitiesPacket#toPacket: error writing Block Entity Request to packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_C2S_ENTITY_REQUEST ->
			{
				// Write Entity Request
				try
				{
					output.writeVarInt(this.entityId);
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxEntitiesPacket#toPacket: error writing Entity Request to packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE ->
			{
				try
				{
					output.writeBlockPos(this.pos);
//                    output.writeNbt(this.nbt);
					DataByteBufUtils.toByteBuf(output, this.nbt, "");
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxEntitiesPacket#toPacket: error writing Block Entity Response to packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_S2C_ENTITY_NBT_RESPONSE_SIMPLE ->
			{
				try
				{
					output.writeVarInt(this.entityId);
//                    output.writeNbt(this.nbt);
					DataByteBufUtils.toByteBuf(output, this.nbt, "");
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxEntitiesPacket#toPacket: error writing Entity Response to packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_S2C_NBT_RESPONSE_DATA, PACKET_C2S_NBT_RESPONSE_DATA ->
			{
				// Write Packet Buffer (Slice)
				try
				{
					output.writeBytes(this.buffer.copy());
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxEntitiesPacket#toPacket: error writing buffer data to packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_C2S_METADATA_REQUEST, PACKET_S2C_METADATA ->
			{
				// Write NBT
				try
				{
					output.writeNbt(this.toVanilla());
//					DataByteBufUtils.toByteBuf(output, this.nbt, "");
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxEntitiesPacket#toPacket: error writing NBT to packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_C2S_UNREGISTER_REPLY ->
			{
				// Write NBT
				try
				{
//                    output.writeNbt(this.nbt);
					DataByteBufUtils.toByteBuf(output, this.nbt, "");
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxEntitiesPacket#toPacket: error writing Data to packet: [{}]", e.getLocalizedMessage());
				}
			}
			default -> MiniHUD.LOGGER.error("ServuxEntitiesPacket#toPacket: Unknown packet type!");
		}
	}

	@Nullable
	public static ServuxEntitiesPacket fromPacket(FriendlyByteBuf input)
	{
		int i = input.readVarInt();
		Type type = getType(i);

		if (type == null)
		{
			// Invalid Type
			MiniHUD.LOGGER.warn("ServuxEntitiesPacket#fromPacket: invalid packet type received");
			return null;
		}
		switch (type)
		{
			case PACKET_C2S_BLOCK_ENTITY_REQUEST ->
			{
				// Read Packet Buffer
				try
				{
					return ServuxEntitiesPacket.BlockEntityRequest(input.readBlockPos());
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxEntitiesPacket#fromPacket: error reading Block Entity Request from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_C2S_ENTITY_REQUEST ->
			{
				// Read Packet Buffer
				try
				{
					return ServuxEntitiesPacket.EntityRequest(input.readVarInt());
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxEntitiesPacket#fromPacket: error reading Entity Request from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE ->
			{
				try
				{
					final BlockPos pos = input.readBlockPos();
					Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(input);
//					return ServuxEntitiesPacket.SimpleBlockResponse(input.readBlockPos(), (CompoundTag) input.readNbt(NbtAccounter.unlimitedHeap()));
					if (opt.isPresent())
					{
						return ServuxEntitiesPacket.SimpleBlockResponse(pos, (CompoundData) opt.get());
					}
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxEntitiesPacket#fromPacket: error reading Block Entity Response from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_S2C_ENTITY_NBT_RESPONSE_SIMPLE ->
			{
				try
				{
					final int entityId = input.readVarInt();
					Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(input);
//					return ServuxEntitiesPacket.SimpleEntityResponse(input.readVarInt(), (CompoundTag) input.readNbt(NbtAccounter.unlimitedHeap()));
					if (opt.isPresent())
					{
						return ServuxEntitiesPacket.SimpleEntityResponse(entityId, (CompoundData) opt.get());
					}
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxEntitiesPacket#fromPacket: error reading Entity Response from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_S2C_NBT_RESPONSE_DATA ->
			{
				// Read Packet Buffer Slice
				try
				{
					return ServuxEntitiesPacket.ResponseS2CData(new FriendlyByteBuf(input.readBytes(input.readableBytes())));
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxEntitiesPacket#fromPacket: error reading S2C Bulk Response Buffer from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_C2S_NBT_RESPONSE_DATA ->
			{
				// Read Packet Buffer Slice
				try
				{
					return ServuxEntitiesPacket.ResponseC2SData(new FriendlyByteBuf(input.readBytes(input.readableBytes())));
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxEntitiesPacket#fromPacket: error reading C2S Bulk Response Buffer from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_C2S_METADATA_REQUEST ->
			{
				// Read Nbt
				try
				{
//					Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(input);
					return ServuxEntitiesPacket.MetadataRequest(fromVanilla(input.readNbt()));
//					if (opt.isPresent())
//					{
//						return ServuxEntitiesPacket.MetadataRequest((CompoundData) opt.get());
//					}
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxEntitiesPacket#fromPacket: error reading Metadata Request from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_S2C_METADATA ->
			{
				// Read Nbt
				try
				{
//					Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(input);
					return ServuxEntitiesPacket.MetadataResponse(fromVanilla(input.readNbt()));
//					if (opt.isPresent())
//					{
//						return ServuxEntitiesPacket.MetadataResponse((CompoundData) opt.get());
//					}
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxEntitiesPacket#fromPacket: error reading Metadata Response from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_C2S_UNREGISTER_REPLY ->
			{
				// Read Nbt
				try
				{
					Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(input);
//                    return ServuxEntitiesPacket.UnregisterReply(input.readNbt());
					if (opt.isPresent())
					{
						return ServuxEntitiesPacket.UnregisterReply((CompoundData) opt.get());
					}
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxEntitiesPacket#fromPacket: error reading Unregister from packet: [{}]", e.getLocalizedMessage());
				}
			}
			default -> MiniHUD.LOGGER.error("ServuxEntitiesPacket#fromPacket: Unknown packet type!");
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
		this.entityId = -1;
		this.pos = BlockPos.ZERO;
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
		PACKET_C2S_BLOCK_ENTITY_REQUEST(3),
		PACKET_C2S_ENTITY_REQUEST(4),
		PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE(5),
		PACKET_S2C_ENTITY_NBT_RESPONSE_SIMPLE(6),
		PACKET_C2S_UNREGISTER_REPLY(7),
		// For Packet Splitter (Oversize Packets, S2C)
		PACKET_S2C_NBT_RESPONSE_START(10),
		PACKET_S2C_NBT_RESPONSE_DATA(11),
		// For Packet Splitter (Oversize Packets, C2S)
		PACKET_C2S_NBT_RESPONSE_START(12),
		PACKET_C2S_NBT_RESPONSE_DATA(13);

		private final int type;

		Type(int type)
		{
			this.type = type;
		}

		int get() {return this.type;}
	}

	public record Payload(ServuxEntitiesPacket data) implements CustomPacketPayload
	{
		public static final CustomPacketPayload.Type<ServuxEntitiesPacket.Payload> ID = new CustomPacketPayload.Type<>(ServuxEntitiesHandler.CHANNEL_ID);
		public static final StreamCodec<FriendlyByteBuf, ServuxEntitiesPacket.Payload> CODEC = CustomPacketPayload.codec(ServuxEntitiesPacket.Payload::write, ServuxEntitiesPacket.Payload::new);

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
