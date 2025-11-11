package fi.dy.masa.minihud.network;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import io.netty.buffer.Unpooled;
import fi.dy.masa.malilib.network.IClientPayloadData;
import fi.dy.masa.minihud.MiniHUD;

public class ServuxHudPacket implements IClientPayloadData
{
    private Type packetType;
    private CompoundTag nbt;
    private FriendlyByteBuf buffer;
    public static final int PROTOCOL_VERSION = 2;

    private ServuxHudPacket(Type type)
    {
        this.packetType = type;
        this.nbt = new CompoundTag();
        this.clearPacket();
    }

    public static ServuxHudPacket MetadataRequest(@Nullable CompoundTag nbt)
    {
        var packet = new ServuxHudPacket(Type.PACKET_C2S_METADATA_REQUEST);
        if (nbt != null)
        {
            packet.nbt.merge(nbt);
        }
        return packet;
    }

    public static ServuxHudPacket MetadataResponse(@Nullable CompoundTag nbt)
    {
        var packet = new ServuxHudPacket(Type.PACKET_S2C_METADATA);
        if (nbt != null)
        {
            packet.nbt.merge(nbt);
        }
        return packet;
    }

    public static ServuxHudPacket SpawnRequest(@Nullable CompoundTag nbt)
    {
        var packet = new ServuxHudPacket(Type.PACKET_C2S_SPAWN_DATA_REQUEST);
        if (nbt != null)
        {
            packet.nbt.merge(nbt);
        }
        return packet;
    }

    public static ServuxHudPacket SpawnResponse(@Nullable CompoundTag nbt)
    {
        var packet = new ServuxHudPacket(Type.PACKET_S2C_SPAWN_DATA);
        if (nbt != null)
        {
            packet.nbt.merge(nbt);
        }
        return packet;
    }

    public static ServuxHudPacket DataLoggerRequest(@Nullable CompoundTag nbt)
    {
        var packet = new ServuxHudPacket(Type.PACKET_C2S_DATA_LOGGER_REQUEST);
        if (nbt != null)
        {
            packet.nbt.merge(nbt);
        }
        return packet;
    }

    public static ServuxHudPacket DataLoggerTick(@Nullable CompoundTag nbt)
    {
        var packet = new ServuxHudPacket(Type.PACKET_S2C_DATA_LOGGER_TICK);
        if (nbt != null)
        {
            packet.nbt.merge(nbt);
        }
        return packet;
    }

    public static ServuxHudPacket WeatherTick(@Nullable CompoundTag nbt)
    {
        var packet = new ServuxHudPacket(Type.PACKET_S2C_WEATHER_TICK);
        if (nbt != null)
        {
            packet.nbt.merge(nbt);
        }
        return packet;
    }

    public static ServuxHudPacket RecipeManagerRequest(@Nullable CompoundTag nbt)
    {
        var packet = new ServuxHudPacket(Type.PACKET_C2S_RECIPE_MANAGER_REQUEST);
        if (nbt != null)
        {
            packet.nbt.merge(nbt);
        }
        return packet;
    }

    // Nbt Packet, using Packet Splitter
    public static ServuxHudPacket ResponseS2CStart(@Nonnull CompoundTag nbt)
    {
        var packet = new ServuxHudPacket(Type.PACKET_S2C_NBT_RESPONSE_START);
        packet.nbt.merge(nbt);
        return packet;
    }

    public static ServuxHudPacket ResponseS2CData(@Nonnull FriendlyByteBuf buffer)
    {
        var packet = new ServuxHudPacket(Type.PACKET_S2C_NBT_RESPONSE_DATA);
        packet.buffer = new FriendlyByteBuf(buffer.copy());
        packet.nbt = new CompoundTag();
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

    public CompoundTag getCompound()
    {
        return this.nbt;
    }

    public FriendlyByteBuf getBuffer()
    {
        return this.buffer;
    }

    public boolean hasBuffer() { return this.buffer != null && this.buffer.isReadable(); }

    public boolean hasNbt() { return this.nbt != null && !this.nbt.isEmpty(); }

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
                    /*
                    PacketByteBuf serverReplay = new PacketByteBuf(this.buffer.copy());
                    output.writeBytes(serverReplay.readBytes(serverReplay.readableBytes()));
                     */

                    output.writeBytes(this.buffer.copy());
                }
                catch (Exception e)
                {
                    MiniHUD.LOGGER.error("ServuxHudPacket#toPacket: error writing buffer data to packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_C2S_METADATA_REQUEST, PACKET_S2C_METADATA, PACKET_C2S_SPAWN_DATA_REQUEST, PACKET_S2C_SPAWN_DATA, PACKET_S2C_WEATHER_TICK, PACKET_C2S_RECIPE_MANAGER_REQUEST, PACKET_C2S_DATA_LOGGER_REQUEST, PACKET_S2C_DATA_LOGGER_TICK ->
            {
                // Write NBT
                try
                {
                    output.writeNbt(this.nbt);
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
                    return ServuxHudPacket.MetadataRequest(input.readNbt());
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
                    return ServuxHudPacket.MetadataResponse(input.readNbt());
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
                    return ServuxHudPacket.SpawnRequest(input.readNbt());
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
                    return ServuxHudPacket.SpawnResponse(input.readNbt());
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
                    return ServuxHudPacket.DataLoggerRequest(input.readNbt());
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
                    return ServuxHudPacket.DataLoggerTick(input.readNbt());
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
                    return ServuxHudPacket.WeatherTick(input.readNbt());
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
                    return ServuxHudPacket.RecipeManagerRequest(input.readNbt());
                }
                catch (Exception e)
                {
                    MiniHUD.LOGGER.error("ServuxHudPacket#fromPacket: error reading Recipe Request from packet: [{}]", e.getLocalizedMessage());
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
            this.nbt = new CompoundTag();
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
        // For Packet Splitter (Oversize Packets, S2C)
        PACKET_S2C_NBT_RESPONSE_START(10),
        PACKET_S2C_NBT_RESPONSE_DATA(11);

        private final int type;

        Type(int type)
        {
            this.type = type;
        }

        int get() { return this.type; }
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
