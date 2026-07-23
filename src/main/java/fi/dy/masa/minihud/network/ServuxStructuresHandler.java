package fi.dy.masa.minihud.network;

import java.util.Optional;
import org.jspecify.annotations.NonNull;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import fi.dy.masa.malilib.network.IClientPayloadData;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.malilib.network.PacketSplitter;
import fi.dy.masa.malilib.util.data.Constants;
import fi.dy.masa.malilib.util.data.tag.BaseData;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.ListData;
import fi.dy.masa.malilib.util.data.tag.util.DataByteBufUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.util.DataStorage;

@Environment(EnvType.CLIENT)
public abstract class ServuxStructuresHandler<T extends CustomPacketPayload> implements IPluginClientPlayHandler<T>
{
	private final static ServuxStructuresHandler<ServuxStructuresPacket.Payload> INSTANCE = new ServuxStructuresHandler<>()
	{
		@Override
		public void receive(ServuxStructuresPacket.@NonNull Payload payload, ClientPlayNetworking.@NonNull Context context)
		{
			ServuxStructuresHandler.INSTANCE.receivePlayPayload(payload, context);
		}
	};

	public static ServuxStructuresHandler<ServuxStructuresPacket.Payload> getInstance() {return INSTANCE;}

	public static final Identifier CHANNEL_ID = Identifier.fromNamespaceAndPath("servux", "structures");

	private boolean servuxRegistered;
	private boolean payloadRegistered = false;
	private int failures = 0;
	private long readingSessionKey = -1;

	@Override
	public Identifier getPayloadChannel() {return CHANNEL_ID;}

	@Override
	public boolean isPlayRegistered(Identifier channel)
	{
		if (channel.equals(CHANNEL_ID))
		{
			return this.payloadRegistered;
		}

		return false;
	}

	@Override
	public void setPlayRegistered(Identifier channel)
	{
		if (channel.equals(CHANNEL_ID))
		{
			this.payloadRegistered = true;
		}
	}

	@Override
	public <P extends IClientPayloadData> void decodeClientData(Identifier channel, P data)
	{
		if (!channel.equals(CHANNEL_ID))
		{
			return;
		}
		if (!DataStorage.getInstance().isEnabled() || !this.checkFailures())
		{
			return;
		}

		if (data instanceof ServuxStructuresPacket packet)
		{
			switch (packet.getType())
			{
				case PACKET_S2C_METADATA ->
				{
					if (DataStorage.getInstance().receiveServuxStrucutresMetadata(packet.getCompound()))
					{
						this.servuxRegistered = true;
					}
				}
				case PACKET_S2C_STRUCTURE_DATA ->
				{
					if (this.servuxRegistered)
					{
						if (this.readingSessionKey == -1)
						{
							this.readingSessionKey = RandomSource.create(Util.getMillis()).nextLong();
						}

						MiniHUD.debugLog("ServuxStructuresHandler#decodeClientData(): received Structures Data Packet Slice of size {} (in bytes) // reading session key [{}]", packet.getTotalSize(), this.readingSessionKey);
						FriendlyByteBuf fullPacket = PacketSplitter.receive(this, this.readingSessionKey, packet.getBuffer());

						if (fullPacket != null)
						{
							try
							{
								final int packetSize = fullPacket.readableBytes();
								this.readingSessionKey = -1;
								Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(fullPacket);

								if (opt.isPresent())
								{
									CompoundData received = (CompoundData) opt.get();
									ListData structures = received.getListOrDefault("Structures", Constants.NBT.TAG_COMPOUND, new ListData());
									MiniHUD.debugLog("ServuxStructuresHandler#decodeClientData(): received Structures Data of size {}/{} (in bytes) // structures [{}]", packetSize, received.sizeInBytes(), structures.size());
									DataStorage.getInstance().addOrUpdateStructuresFromServer(structures, this.servuxRegistered);
								}
								else
								{
									MiniHUD.LOGGER.warn("ServuxStructuresHandler#decodeClientData(): Structures Data: error reading fullBuffer NBT is NULL");
								}
							}
							catch (Exception e)
							{
								MiniHUD.LOGGER.error("ServuxStructuresHandler#decodeClientData(): Structures Data: error reading fullBuffer [{}]", e.getLocalizedMessage());
							}
						}
					}
				}
				default ->
						MiniHUD.LOGGER.warn("ServuxStructuresHandler#decodeClientData(): received unhandled packetType {} of size {} bytes.", packet.getPacketType(), packet.getTotalSize());
			}
		}
	}

	@Override
	public void reset(Identifier channel)
	{
		if (channel.equals(CHANNEL_ID) && this.servuxRegistered)
		{
			this.servuxRegistered = false;
			this.failures = 0;
			this.readingSessionKey = -1;
		}
	}

	public void resetFailures(Identifier channel)
	{
		if (channel.equals(CHANNEL_ID) && this.failures > 0)
		{
			this.failures = 0;
		}
	}

	@Override
	public void receivePlayPayload(T payload, ClientPlayNetworking.Context ctx)
	{
		if (payload.type().id().equals(CHANNEL_ID))
		{
			ServuxStructuresHandler.INSTANCE.decodeClientData(CHANNEL_ID, ((ServuxStructuresPacket.Payload) payload).data());
		}
	}

	@Override
	public void encodeWithSplitter(FriendlyByteBuf buffer, ClientPacketListener handler)
	{
		ServuxStructuresHandler.INSTANCE.sendPlayPayload(new ServuxStructuresPacket.Payload(ServuxStructuresPacket.StructuresS2CData(buffer)));
	}

	@Override
	public <P extends IClientPayloadData> void encodeClientData(P data)
	{
		// !DataStorage.getInstance().isEnabled() ||
		if (!this.checkFailures())
		{
			return;
		}

		if (data instanceof ServuxStructuresPacket packet)
		{
			if (!ServuxStructuresHandler.INSTANCE.sendPlayPayload(new ServuxStructuresPacket.Payload(packet)))
			{
				this.tickFailures();
			}
		}
	}

	@Override
	public boolean checkFailures()
	{
		return !(this.failures > this.maxFailures());
	}

	@Override
	public void tickFailures()
	{
		if (this.failures > this.maxFailures())
		{
			MiniHUD.debugLog("encodeStructuresPacket(): encountered [{}] sendPayload failures, cancelling any Servux join attempt(s)", this.maxFailures());
			this.servuxRegistered = false;
			ServuxStructuresHandler.INSTANCE.unregisterPlayReceiver();
			DataStorage.getInstance().onPacketFailure();
		}
		else
		{
			this.failures++;
		}
	}
}
