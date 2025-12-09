package fi.dy.masa.minihud.network;

import org.jspecify.annotations.NonNull;

import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.malilib.network.PacketSplitter;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.data.HudDataManager;
import fi.dy.masa.minihud.util.DataStorage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public abstract class ServuxStructuresHandler<T extends CustomPacketPayload> implements IPluginClientPlayHandler<T>
{
    private final static ServuxStructuresHandler<ServuxStructuresPacket.Payload> INSTANCE = new ServuxStructuresHandler<>()
    {
        @Override
        public void receive(ServuxStructuresPacket.Payload payload, ClientPlayNetworking.@NonNull Context context)
        {
            ServuxStructuresHandler.INSTANCE.receivePlayPayload(payload, context);
        }
    };
    public static ServuxStructuresHandler<ServuxStructuresPacket.Payload> getInstance() { return INSTANCE; }

    public static final Identifier CHANNEL_ID = Identifier.fromNamespaceAndPath("servux", "structures");

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

    public void decodeStructuresPacket(Identifier channel, ServuxStructuresPacket packet)
    {
        if (!channel.equals(CHANNEL_ID) || packet == null)
        {
            return;
        }
        switch (packet.getType())
        {
            case PACKET_S2C_STRUCTURE_DATA ->
            {
                if (this.readingSessionKey == -1)
                {
                    this.readingSessionKey = RandomSource.create(Util.getMillis()).nextLong();
                }

                FriendlyByteBuf fullPacket = PacketSplitter.receive(this, this.readingSessionKey, packet.getBuffer());

                if (fullPacket != null)
                {
                    try
                    {
                        CompoundTag nbt = (CompoundTag) fullPacket.readNbt(NbtAccounter.unlimitedHeap());
                        this.readingSessionKey = -1;

                        if (nbt != null)
                        {
                            ListTag structures = nbt.getListOrEmpty("Structures");
                            MiniHUD.debugLog("decodeStructuresPacket(): received Structures Data of size {} (in bytes) // structures [{}]", nbt.sizeInBytes(), structures.size());

                            DataStorage.getInstance().addOrUpdateStructuresFromServer(structures, this.servuxRegistered);
                        }
                        else
                        {
                            MiniHUD.LOGGER.warn("decodeStructuresPacket(): Structures Data: error reading fullBuffer NBT is NULL");
                        }
                    }
                    catch (Exception e)
                    {
                        MiniHUD.LOGGER.error("decodeStructuresPacket(): Structures Data: error reading fullBuffer [{}]", e.getLocalizedMessage());
                    }
                }
            }
            case PACKET_S2C_METADATA ->
            {
                if (DataStorage.getInstance().receiveServuxStrucutresMetadata(packet.getCompound()))
                {
                    this.servuxRegistered = true;
                }
            }
            // For backwards compat, only if hud_data isn't connected, or if Servux is too old
            case PACKET_S2C_SPAWN_METADATA ->
            {
                if (HudDataManager.getInstance().hasServuxServer() == false)
                {
                    HudDataManager.getInstance().receiveSpawnMetadata(packet.getCompound());
                }
            }
            case PACKET_S2C_WEATHER_DATA ->
            {
                if (HudDataManager.getInstance().hasServuxServer() == false)
                {
                    HudDataManager.getInstance().receiveWeatherData(packet.getCompound());
                }
            }
            default -> MiniHUD.LOGGER.warn("decodeStructuresPacket(): received unhandled packetType {} of size {} bytes.", packet.getPacketType(), packet.getTotalSize());
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
            ServuxStructuresHandler.INSTANCE.decodeStructuresPacket(CHANNEL_ID, ((ServuxStructuresPacket.Payload) payload).data());
        }
    }

    @Override
    public void encodeWithSplitter(FriendlyByteBuf buffer, ClientPacketListener handler)
    {
        // NO-OP
    }

    public void encodeStructuresPacket(ServuxStructuresPacket packet)
    {
        if (!ServuxStructuresHandler.INSTANCE.sendPlayPayload(new ServuxStructuresPacket.Payload(packet)))
        {
            if (this.failures > MAX_FAILURES)
            {
                MiniHUD.debugLog("encodeStructuresPacket(): encountered [{}] sendPayload failures, cancelling any Servux join attempt(s)", MAX_FAILURES);
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
}
