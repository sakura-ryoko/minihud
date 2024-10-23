package fi.dy.masa.minihud.network;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import fi.dy.masa.malilib.network.IClientPayloadData;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.malilib.network.PacketSplitter;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.data.HudDataManager;

@Environment(EnvType.CLIENT)
public abstract class ServuxHudHandler<T extends CustomPayload> implements IPluginClientPlayHandler<T>
{
    private static final ServuxHudHandler<ServuxHudPacket.Payload> INSTANCE = new ServuxHudHandler<>() {
        @Override
        public void receive(ServuxHudPacket.Payload payload, ClientPlayNetworking.Context context)
        {
            ServuxHudHandler.INSTANCE.receivePlayPayload(payload, context);
        }
    };
    public static ServuxHudHandler<ServuxHudPacket.Payload> getInstance() { return INSTANCE; }

    public static final Identifier CHANNEL_ID = Identifier.of("servux", "hud_metadata");

    private boolean servuxRegistered;
    private boolean payloadRegistered = false;
    private int failures = 0;
    private static final int MAX_FAILURES = 4;
    private long readingSessionKey = -1;

    @Override
    public Identifier getPayloadChannel() { return CHANNEL_ID; }

    @Override
    public boolean isPlayRegistered(Identifier channel)
    {
        if (channel.equals(CHANNEL_ID))
        {
            return payloadRegistered;
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
        ServuxHudPacket packet = (ServuxHudPacket) data;

        if (!channel.equals(CHANNEL_ID) || packet == null)
        {
            return;
        }
        switch (packet.getType())
        {
            case PACKET_S2C_METADATA ->
            {
                if (HudDataManager.getInstance().receiveMetadata(packet.getCompound()))
                {
                    this.servuxRegistered = true;
                }
            }
            case PACKET_S2C_SPAWN_DATA ->
            {
                HudDataManager.getInstance().receiveSpawnMetadata(packet.getCompound());
            }
            case PACKET_S2C_WEATHER_TICK ->
            {
                HudDataManager.getInstance().receiveWeatherData(packet.getCompound());
            }
            case PACKET_S2C_NBT_RESPONSE_DATA ->
            {
                if (this.readingSessionKey == -1)
                {
                    this.readingSessionKey = Random.create(Util.getMeasuringTimeMs()).nextLong();
                }

                MiniHUD.printDebug("ServuxHudHandler#decodeClientData(): received Entity Data Packet Slice of size {} (in bytes) // reading session key [{}]", packet.getTotalSize(), this.readingSessionKey);
                PacketByteBuf fullPacket = PacketSplitter.receive(this, this.readingSessionKey, packet.getBuffer());

                if (fullPacket != null)
                {
                    try
                    {
                        this.readingSessionKey = -1;
                        HudDataManager.getInstance().receiveRecipeManager((NbtCompound) fullPacket.readNbt(NbtSizeTracker.ofUnlimitedBytes()));
                    }
                    catch (Exception e)
                    {
                        MiniHUD.logger.error("ServuxHudHandler#decodeClientData(): Entity Data: error reading fullBuffer [{}]", e.getLocalizedMessage());
                    }
                }
            }
            default -> MiniHUD.logger.warn("ServuxHudHandler#decodeClientData(): received unhandled packetType {} of size {} bytes.", packet.getPacketType(), packet.getTotalSize());
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
    public void encodeWithSplitter(PacketByteBuf buf, ClientPlayNetworkHandler handler)
    {
        // NO-OP
    }

    @Override
    public void receivePlayPayload(T payload, ClientPlayNetworking.Context ctx)
    {
        if (payload.getId().id().equals(CHANNEL_ID))
        {
            ServuxHudHandler.INSTANCE.decodeClientData(CHANNEL_ID, ((ServuxHudPacket.Payload) payload).data());
        }
    }

    @Override
    public <P extends IClientPayloadData> void encodeClientData(P data)
    {
        ServuxHudPacket packet = (ServuxHudPacket) data;

        if (!ServuxHudHandler.INSTANCE.sendPlayPayload(new ServuxHudPacket.Payload(packet)))
        {
            if (this.failures > MAX_FAILURES)
            {
                MiniHUD.printDebug("encodeClientData(): encountered [{}] sendPayload failures, cancelling any Servux join attempt(s)", MAX_FAILURES);
                this.servuxRegistered = false;
                ServuxHudHandler.INSTANCE.unregisterPlayReceiver();
                HudDataManager.getInstance().onPacketFailure();
            }
            else
            {
                this.failures++;
            }
        }
    }
}
