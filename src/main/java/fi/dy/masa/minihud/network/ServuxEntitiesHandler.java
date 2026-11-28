package fi.dy.masa.minihud.network;

import org.jspecify.annotations.NonNull;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import fi.dy.masa.malilib.network.IClientPayloadData;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.malilib.network.status.NetworkState;
import fi.dy.masa.malilib.network.status.NetworkStateTracker;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.data.EntityDataManager;

@Environment(EnvType.CLIENT)
public abstract class ServuxEntitiesHandler<T extends CustomPacketPayload> implements IPluginClientPlayHandler<T>
{
	private final static ServuxEntitiesHandler<ServuxEntitiesPacket.Payload> INSTANCE = new ServuxEntitiesHandler<>()
	{
		@Override
		public void receive(ServuxEntitiesPacket.@NonNull Payload payload, ClientPlayNetworking.@NonNull Context context)
		{
			ServuxEntitiesHandler.INSTANCE.receivePlayPayload(payload, context);
		}
	};

	public static ServuxEntitiesHandler<ServuxEntitiesPacket.Payload> getInstance() {return INSTANCE;}

	public static final Identifier CHANNEL_ID = Identifier.fromNamespaceAndPath("servux", "entity_data");
	private final NetworkStateTracker tracker = new NetworkStateTracker("entity_data");

	private boolean servuxRegistered;
	private boolean payloadRegistered = false;

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
		if (!EntityDataManager.getInstance().isEnabled() || !this.checkFailures())
		{
			return;
		}

		if (data instanceof ServuxEntitiesPacket packet)
		{
			switch (packet.getType())
			{
				case PACKET_S2C_METADATA ->
				{
					if (EntityDataManager.getInstance().receiveServuxMetadata(packet.getCompound()))
					{
						this.servuxRegistered = true;
					}
				}
				case PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE ->
				{
					if (this.servuxRegistered)
					{
						EntityDataManager.getInstance().handleBlockEntityData(packet.getPos(), packet.getCompound());
					}
				}
				case PACKET_S2C_ENTITY_NBT_RESPONSE_SIMPLE ->
				{
					if (this.servuxRegistered)
					{
						EntityDataManager.getInstance().handleEntityData(packet.getEntityId(), packet.getCompound());
					}
				}
				default ->
						MiniHUD.LOGGER.warn("ServuxEntitiesHandler#decodeClientData(): received unhandled packetType {} of size {} bytes.", packet.getPacketType(), packet.getTotalSize());
			}
		}
	}

	@Override
	public void reset(Identifier channel)
	{
		if (channel.equals(CHANNEL_ID))
		{
			this.servuxRegistered = false;
			this.tracker().reset();
		}
	}

	@Override
	public void receivePlayPayload(T payload, ClientPlayNetworking.Context ctx)
	{
		if (payload.type().id().equals(CHANNEL_ID))
		{
			ServuxEntitiesHandler.INSTANCE.decodeClientData(CHANNEL_ID, ((ServuxEntitiesPacket.Payload) payload).data());
		}
	}

	@Override
	public void encodeWithSplitter(FriendlyByteBuf buffer, ClientPacketListener handler)
	{
		// Send each PacketSplitter buffer slice
		ServuxEntitiesHandler.INSTANCE.sendPlayPayload(new ServuxEntitiesPacket.Payload(ServuxEntitiesPacket.ResponseC2SData(buffer)));
	}

	@Override
	public <P extends IClientPayloadData> void encodeClientData(P data)
	{
		if (!EntityDataManager.getInstance().isEnabled() || !this.checkFailures())
		{
			return;
		}

		if (data instanceof ServuxEntitiesPacket packet)
		{
            if (!ServuxEntitiesHandler.INSTANCE.sendPlayPayload(new ServuxEntitiesPacket.Payload(packet)))
			{
				this.tickFailures();
			}
		}
	}

	@Override
	public boolean checkFailures()
	{
		return !(this.tracker().failureCount() > this.maxFailures());
	}

	@Override
	public NetworkStateTracker tracker()
	{
		return this.tracker;
	}

	@Override
	public void tickFailures()
	{
		int count = this.tracker().failureCount();

		if (count > this.maxFailures())
		{
			MiniHUD.debugLog("ServuxEntitiesHandler#encodeClientData(): encountered [{}] sendPayload failures, cancelling any Servux join attempt(s)", this.maxFailures());
			this.servuxRegistered = false;
			this.tracker().lock();
			ServuxEntitiesHandler.INSTANCE.unregisterPlayReceiver();
			EntityDataManager.getInstance().onPacketFailure();
		}
		else
		{
			this.tracker().tickState(NetworkState.GENERAL_ERROR);
		}
	}
}
