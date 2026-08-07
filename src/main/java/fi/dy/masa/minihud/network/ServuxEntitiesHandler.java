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

	private boolean servuxRegistered;
	private boolean payloadRegistered = false;
	private int failures = 0;

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
		if (channel.equals(CHANNEL_ID) && this.servuxRegistered)
		{
			this.servuxRegistered = false;
			this.failures = 0;
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
		return !(this.failures > this.maxFailures());
	}

	@Override
	public void tickFailures()
	{
		if (this.failures > this.maxFailures())
		{
			MiniHUD.debugLog("ServuxEntitiesHandler#encodeClientData(): encountered [{}] sendPayload failures, cancelling any Servux join attempt(s)", this.maxFailures());
			this.servuxRegistered = false;
			ServuxEntitiesHandler.INSTANCE.unregisterPlayReceiver();
			EntityDataManager.getInstance().onPacketFailure();
		}
		else
		{
			this.failures++;
		}
	}
}
