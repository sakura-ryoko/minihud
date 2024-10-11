package fi.dy.masa.minihud.network;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import io.netty.buffer.Unpooled;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import fi.dy.masa.malilib.network.IClientPayloadData;
import fi.dy.masa.minihud.MiniHUD;

public class ServuxDebugPacket implements IClientPayloadData
{
    private Type packetType;
    private NbtCompound nbt;
    private PacketByteBuf buffer;
    public static final int PROTOCOL_VERSION = 1;

    private ServuxDebugPacket(Type type)
    {
        this.packetType = type;
        this.nbt = new NbtCompound();
        this.clearPacket();
    }

    public static ServuxDebugPacket MetadataRequest(@Nullable NbtCompound nbt)
    {
        var packet = new ServuxDebugPacket(Type.PACKET_C2S_METADATA_REQUEST);
        if (nbt != null)
        {
            packet.nbt.copyFrom(nbt);
        }
        return packet;
    }

    public static ServuxDebugPacket MetadataResponse(@Nullable NbtCompound nbt)
    {
        var packet = new ServuxDebugPacket(Type.PACKET_S2C_METADATA);
        if (nbt != null)
        {
            packet.nbt.copyFrom(nbt);
        }
        return packet;
    }

    public static ServuxDebugPacket MetadataConfirm(@Nullable NbtCompound nbt)
    {
        var packet = new ServuxDebugPacket(Type.PACKET_C2S_METADATA_CONFIRM);
        if (nbt != null)
        {
            packet.nbt.copyFrom(nbt);
        }
        return packet;
    }

    public static ServuxDebugPacket DebugServiceRegister(@Nullable NbtCompound nbt)
    {
        var packet = new ServuxDebugPacket(Type.PACKET_C2S_DEBUG_SERVICE_REGISTER);
        if (nbt != null)
        {
            packet.nbt.copyFrom(nbt);
        }
        return packet;
    }

    public static ServuxDebugPacket DebugServiceUnregister(@Nullable NbtCompound nbt)
    {
        var packet = new ServuxDebugPacket(Type.PACKET_C2S_DEBUG_SERVICE_UNREGISTER);
        if (nbt != null)
        {
            packet.nbt.copyFrom(nbt);
        }
        return packet;
    }

    // Nbt Packet, using Packet Splitter
    public static ServuxDebugPacket ResponseS2CStart(@Nonnull NbtCompound nbt)
    {
        var packet = new ServuxDebugPacket(Type.PACKET_S2C_NBT_RESPONSE_START);
        packet.nbt.copyFrom(nbt);
        return packet;
    }

    public static ServuxDebugPacket ResponseS2CData(@Nonnull PacketByteBuf buffer)
    {
        var packet = new ServuxDebugPacket(Type.PACKET_S2C_NBT_RESPONSE_DATA);
        packet.buffer = buffer;
        packet.nbt = new NbtCompound();
        return packet;
    }

    private void clearPacket()
    {
        if (this.buffer != null)
        {
            this.buffer.clear();
            this.buffer = new PacketByteBuf(Unpooled.buffer());
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
            total += this.nbt.getSizeInBytes();
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

    public NbtCompound getCompound()
    {
        return this.nbt;
    }

    public PacketByteBuf getBuffer()
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
    public void toPacket(PacketByteBuf output)
    {
        output.writeVarInt(this.packetType.get());

        switch (this.packetType)
        {
            case PACKET_S2C_NBT_RESPONSE_DATA ->
            {
                // Write Packet Buffer (Slice)
                try
                {
                    output.writeBytes(this.buffer.readBytes(this.buffer.readableBytes()));
                }
                catch (Exception e)
                {
                    MiniHUD.logger.error("ServuxDebugPacket#toPacket: error writing buffer data to packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_C2S_METADATA_REQUEST, PACKET_S2C_METADATA, PACKET_C2S_METADATA_CONFIRM, PACKET_C2S_DEBUG_SERVICE_REGISTER, PACKET_C2S_DEBUG_SERVICE_UNREGISTER ->
            {
                // Write NBT
                try
                {
                    output.writeNbt(this.nbt);
                }
                catch (Exception e)
                {
                    MiniHUD.logger.error("ServuxDebugPacket#toPacket: error writing NBT to packet: [{}]", e.getLocalizedMessage());
                }
            }
            default -> MiniHUD.logger.error("ServuxDebugPacket#toPacket: Unknown packet type!");
        }
    }

    @Nullable
    public static ServuxDebugPacket fromPacket(PacketByteBuf input)
    {
        int i = input.readVarInt();
        Type type = getType(i);

        if (type == null)
        {
            // Invalid Type
            MiniHUD.logger.warn("ServuxDebugPacket#fromPacket: invalid packet type received");
            return null;
        }
        switch (type)
        {
            case PACKET_S2C_NBT_RESPONSE_DATA ->
            {
                // Read Packet Buffer Slice
                try
                {
                    return ServuxDebugPacket.ResponseS2CData(new PacketByteBuf(input.readBytes(input.readableBytes())));
                }
                catch (Exception e)
                {
                    MiniHUD.logger.error("ServuxDebugPacket#fromPacket: error reading S2C Bulk Response Buffer from packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_C2S_METADATA_REQUEST ->
            {
                // Read Nbt
                try
                {
                    return ServuxDebugPacket.MetadataRequest(input.readNbt());
                }
                catch (Exception e)
                {
                    MiniHUD.logger.error("ServuxDebugPacket#fromPacket: error reading Metadata Request from packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_S2C_METADATA ->
            {
                // Read Nbt
                try
                {
                    return ServuxDebugPacket.MetadataResponse(input.readNbt());
                }
                catch (Exception e)
                {
                    MiniHUD.logger.error("ServuxDebugPacket#fromPacket: error reading Metadata Response from packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_C2S_METADATA_CONFIRM ->
            {
                // Read Nbt
                try
                {
                    return ServuxDebugPacket.MetadataConfirm(input.readNbt());
                }
                catch (Exception e)
                {
                    MiniHUD.logger.error("ServuxDebugPacket#fromPacket: error reading Metadata Confirm from packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_C2S_DEBUG_SERVICE_REGISTER ->
            {
                // Read Nbt
                try
                {
                    return ServuxDebugPacket.DebugServiceRegister(input.readNbt());
                }
                catch (Exception e)
                {
                    MiniHUD.logger.error("ServuxDebugPacket#fromPacket: error reading Spawn Data Request from packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_C2S_DEBUG_SERVICE_UNREGISTER ->
            {
                // Read Nbt
                try
                {
                    return ServuxDebugPacket.DebugServiceUnregister(input.readNbt());
                }
                catch (Exception e)
                {
                    MiniHUD.logger.error("ServuxDebugPacket#fromPacket: error reading Spawn Data Response from packet: [{}]", e.getLocalizedMessage());
                }
            }
            default -> MiniHUD.logger.error("ServuxDebugPacket#fromPacket: Unknown packet type!");
        }

        return null;
    }

    @Override
    public void clear()
    {
        if (this.nbt != null && !this.nbt.isEmpty())
        {
            this.nbt = new NbtCompound();
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
        PACKET_C2S_METADATA_CONFIRM(3),
        PACKET_C2S_DEBUG_SERVICE_REGISTER(4),
        PACKET_C2S_DEBUG_SERVICE_UNREGISTER(5),
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

    public record Payload(ServuxDebugPacket data) implements CustomPayload
    {
        public static final Id<Payload> ID = new Id<>(ServuxDebugHandler.CHANNEL_ID);
        public static final PacketCodec<PacketByteBuf, Payload> CODEC = CustomPayload.codecOf(Payload::write, Payload::new);

        public Payload(PacketByteBuf input)
        {
            this(fromPacket(input));
        }

        private void write(PacketByteBuf output)
        {
            data.toPacket(output);
        }

        @Override
        public Id<? extends CustomPayload> getId()
        {
            return ID;
        }
    }
}
