package net.minecraft.network.protocol.login;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundLoginCompressionPacket implements Packet<ClientLoginPacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundLoginCompressionPacket> STREAM_CODEC = Packet.codec(
        ClientboundLoginCompressionPacket::write, ClientboundLoginCompressionPacket::new
    );
    private final int compressionThreshold;

    public ClientboundLoginCompressionPacket(int pCompressionThreshold) {
        this.compressionThreshold = pCompressionThreshold;
    }

    private ClientboundLoginCompressionPacket(FriendlyByteBuf pBuffer) {
        this.compressionThreshold = pBuffer.readVarInt();
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.compressionThreshold);
    }

    @Override
    public PacketType<ClientboundLoginCompressionPacket> type() {
        return LoginPacketTypes.CLIENTBOUND_LOGIN_COMPRESSION;
    }

    public void handle(ClientLoginPacketListener pHandler) {
        pHandler.handleCompression(this);
    }

    public int getCompressionThreshold() {
        return this.compressionThreshold;
    }
}