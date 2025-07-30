package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundPaddleBoatPacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundPaddleBoatPacket> STREAM_CODEC = Packet.codec(
        ServerboundPaddleBoatPacket::write, ServerboundPaddleBoatPacket::new
    );
    private final boolean left;
    private final boolean right;

    public ServerboundPaddleBoatPacket(boolean pLeft, boolean pRight) {
        this.left = pLeft;
        this.right = pRight;
    }

    private ServerboundPaddleBoatPacket(FriendlyByteBuf pBuffer) {
        this.left = pBuffer.readBoolean();
        this.right = pBuffer.readBoolean();
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeBoolean(this.left);
        pBuffer.writeBoolean(this.right);
    }

    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handlePaddleBoat(this);
    }

    @Override
    public PacketType<ServerboundPaddleBoatPacket> type() {
        return GamePacketTypes.SERVERBOUND_PADDLE_BOAT;
    }

    public boolean getLeft() {
        return this.left;
    }

    public boolean getRight() {
        return this.right;
    }
}