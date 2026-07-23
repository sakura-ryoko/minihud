package fi.dy.masa.minihud.network;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import io.netty.buffer.Unpooled;
import org.jspecify.annotations.NonNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import fi.dy.masa.malilib.network.IClientPayloadData;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.converter.DataConverterNbt;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.util.DataStorage;

public class ServuxStructuresPacket implements IClientPayloadData
{
	private Type packetType;
	private CompoundData nbt;
	private FriendlyByteBuf buffer;
	public static final int PROTOCOL_VERSION = 3;

	private ServuxStructuresPacket(Type type)
	{
		this.packetType = type;
		this.nbt = new CompoundData();
		this.clearPacket();
	}

	public static ServuxStructuresPacket MetadataReply(CompoundData nbt)
	{
		var packet = new ServuxStructuresPacket(Type.PACKET_S2C_METADATA);
		if (nbt != null)
		{
			packet.nbt.combine(nbt);
		}
		return packet;
	}
	public static ServuxStructuresPacket StructuresRegister(CompoundData nbt)
	{
		var packet = new ServuxStructuresPacket(Type.PACKET_C2S_STRUCTURES_REGISTER);
		if (nbt != null)
		{
			packet.nbt.combine(nbt);
		}
		return packet;
	}
	public static ServuxStructuresPacket StructuresUnregister(CompoundData nbt)
	{
		var packet = new ServuxStructuresPacket(Type.PACKET_C2S_STRUCTURES_UNREGISTER);
		if (nbt != null)
		{
			packet.nbt.combine(nbt);
		}
		return packet;
	}

	// Nbt Packet, using Packet Splitter
	public static ServuxStructuresPacket StructuresS2CStart(@Nonnull CompoundData nbt)
	{
		var packet = new ServuxStructuresPacket(Type.PACKET_S2C_STRUCTURE_DATA_START);
		packet.nbt.combine(nbt);
		return packet;
	}

	public static ServuxStructuresPacket StructuresS2CData(@Nonnull FriendlyByteBuf buffer)
	{
		var packet = new ServuxStructuresPacket(Type.PACKET_S2C_STRUCTURE_DATA);
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

		if (this.nbt != null && this.nbt.isEmpty() == false)
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
			case PACKET_S2C_STRUCTURE_DATA ->
			{
				try
				{
					output.writeBytes(this.buffer.copy());
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxStructuresPacket#toPacket: error writing data to packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_S2C_METADATA, PACKET_C2S_STRUCTURES_REGISTER, PACKET_C2S_STRUCTURES_UNREGISTER ->
			{
				// Write NBT
				try
				{
                    output.writeNbt(this.toVanilla());
//					DataByteBufUtils.toByteBuf(output, this.nbt, "");
				}
				catch (Exception e)
				{
					MiniHUD.LOGGER.error("ServuxStructuresPacket#toPacket: error writing NBT to packet: [{}]", e.getLocalizedMessage());
				}
			}
			default -> MiniHUD.LOGGER.error("ServuxStructuresPacket#toPacket: Unknown packet type!");
		}
	}

	@Nullable
	public static ServuxStructuresPacket fromPacket(FriendlyByteBuf input)
	{
		try
		{
			int i = input.readVarInt();
			Type type = getType(i);

			if (type == null)
			{
				// Invalid Type
				MiniHUD.LOGGER.warn("ServuxStructuresPacket#fromPacket: invalid packet type received");
				return null;
			}
			switch (type)
			{
				case PACKET_S2C_STRUCTURE_DATA ->
				{
					try
					{
						return ServuxStructuresPacket.StructuresS2CData(new FriendlyByteBuf(input.readBytes(input.readableBytes())));
					}
					catch (Exception e)
					{
						MiniHUD.LOGGER.error("ServuxStructuresPacket#fromPacket: error reading Structure Data Buffer from packet: [{}]", e.getLocalizedMessage());
					}
				}
				case PACKET_S2C_METADATA ->
				{
					try
					{
//						Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(input);
						return ServuxStructuresPacket.MetadataReply(fromVanilla(input.readNbt()));
//						if (opt.isPresent())
//						{
//							return ServuxStructuresPacket.MetadataReply((CompoundData) opt.get());
//						}
					}
					catch (Exception e)
					{
						MiniHUD.LOGGER.error("ServuxStructuresPacket#fromPacket: error reading Metadata Reply from packet: [{}]", e.getLocalizedMessage());
					}
				}
				case PACKET_C2S_STRUCTURES_REGISTER ->
				{
					try
					{
//						Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(input);
						return ServuxStructuresPacket.StructuresRegister(fromVanilla(input.readNbt()));
//						if (opt.isPresent())
//						{
//							return ServuxStructuresPacket.StructuresRegister((CompoundData) opt.get());
//						}
					}
					catch (Exception e)
					{
						MiniHUD.LOGGER.error("ServuxStructuresPacket#fromPacket: error reading Structures Register from packet: [{}]", e.getLocalizedMessage());
					}
				}
				case PACKET_C2S_STRUCTURES_UNREGISTER ->
				{
					try
					{
//						Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(input);
						return ServuxStructuresPacket.StructuresUnregister(fromVanilla(input.readNbt()));
//						if (opt.isPresent())
//						{
//							return ServuxStructuresPacket.StructuresUnregister((CompoundData) opt.get());
//						}
					}
					catch (Exception e)
					{
						MiniHUD.LOGGER.error("ServuxStructuresPacket#fromPacket: error reading Structures Unregister from packet: [{}]", e.getLocalizedMessage());
					}
				}
				default -> MiniHUD.LOGGER.error("ServuxStructuresPacket#fromPacket: Unknown packet type!");
			}

			return null;
		}
		catch (Exception e)
		{
			MiniHUD.LOGGER.error("ServuxStructuresPacket#fromPacket: error reading packet", e);
			DataStorage.getInstance().onPacketFailure();
			return null;
		}
		finally
		{
			if (input.isReadable())
			{
				MiniHUD.LOGGER.error("ServuxStructuresPacket#fromPacket: input buffer is not empty, skipping remaining bytes. are you using the correct version?");
				DataStorage.getInstance().onPacketFailure();
				input.skipBytes(input.readableBytes());
			}
		}
	}

	@Override
	public void clear()
	{
		if (this.nbt != null && this.nbt.isEmpty() == false)
		{
			this.nbt = new CompoundData();
		}
//		if (this.buffer != null && this.buffer.readableBytes() > 0)
//		{
//			this.buffer.clear();
//			this.buffer = new FriendlyByteBuf(Unpooled.buffer());
//		}

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
		PACKET_S2C_STRUCTURE_DATA(2),
		PACKET_C2S_STRUCTURES_REGISTER(3),
		PACKET_C2S_STRUCTURES_UNREGISTER(4),
		PACKET_S2C_STRUCTURE_DATA_START(5),
		;

		private final int type;

		Type(int type)
		{
			this.type = type;
		}

		int get() {return this.type;}
	}

	public record Payload(ServuxStructuresPacket data) implements CustomPacketPayload
	{
		public static final CustomPacketPayload.Type<Payload> ID = new CustomPacketPayload.Type<>(ServuxStructuresHandler.CHANNEL_ID);
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
		public CustomPacketPayload.@NonNull Type<Payload> type()
		{
			return ID;
		}
	}
}
