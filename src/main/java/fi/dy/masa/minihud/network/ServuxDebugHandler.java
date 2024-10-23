package fi.dy.masa.minihud.network;

import java.util.Objects;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import fi.dy.masa.malilib.network.IClientPayloadData;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.data.DebugDataManager;

@Environment(EnvType.CLIENT)
public abstract class ServuxDebugHandler<T extends CustomPayload> implements IPluginClientPlayHandler<T>
{
    private static final ServuxDebugHandler<ServuxDebugPacket.Payload> INSTANCE = new ServuxDebugHandler<>() {
        @Override
        public void receive(ServuxDebugPacket.Payload payload, ClientPlayNetworking.Context context)
        {
            ServuxDebugHandler.INSTANCE.receivePlayPayload(payload, context);
        }
    };
    public static ServuxDebugHandler<ServuxDebugPacket.Payload> getInstance() { return INSTANCE; }

    public static final Identifier CHANNEL_ID = Identifier.of("servux", "debug_service");

    private boolean servuxRegistered;
    private boolean payloadRegistered = false;
    private int failures = 0;
    private static final int MAX_FAILURES = 4;
    //private long readingSessionKey = -1;

    @Override
    public Identifier getPayloadChannel() { return CHANNEL_ID; }

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
        ServuxDebugPacket packet = (ServuxDebugPacket) data;

        if (!channel.equals(CHANNEL_ID) || packet == null)
        {
            return;
        }
        if (Objects.requireNonNull(packet.getType()) == ServuxDebugPacket.Type.PACKET_S2C_METADATA)
        {
            if (DebugDataManager.getInstance().receiveMetadata(packet.getCompound()))
            {
                this.servuxRegistered = true;
            }
        }
        else
        {
            MiniHUD.logger.warn("ServuxDebugHandler#decodeClientData(): received unhandled packetType {} of size {} bytes.", packet.getPacketType(), packet.getTotalSize());
        }
    }

    @Override
    public void reset(Identifier channel)
    {
        if (channel.equals(CHANNEL_ID) && this.servuxRegistered)
        {
            this.servuxRegistered = false;
            this.failures = 0;
            //this.readingSessionKey = -1;
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
    public void encodeWithSplitter(PacketByteBuf buf, ClientPlayNetworkHandler handler)
    {
        // NO-OP
    }

    @Override
    public void receivePlayPayload(T payload, ClientPlayNetworking.Context ctx)
    {
        if (payload.getId().id().equals(CHANNEL_ID))
        {
            ServuxDebugHandler.INSTANCE.decodeClientData(CHANNEL_ID, ((ServuxDebugPacket.Payload) payload).data());
        }
    }

    @Override
    public <P extends IClientPayloadData> void encodeClientData(P data)
    {
        ServuxDebugPacket packet = (ServuxDebugPacket) data;

        if (!ServuxDebugHandler.INSTANCE.sendPlayPayload(new ServuxDebugPacket.Payload(packet)))
        {
            if (this.failures > MAX_FAILURES)
            {
                MiniHUD.printDebug("encodeClientData(): encountered [{}] sendPayload failures, cancelling any Servux join attempt(s)", MAX_FAILURES);
                this.servuxRegistered = false;
                ServuxDebugHandler.INSTANCE.unregisterPlayReceiver();
                DebugDataManager.getInstance().onPacketFailure();
            }
            else
            {
                this.failures++;
            }
        }
    }
}
