package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundTakeItemEntityPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundTakeItemEntityPacket> STREAM_CODEC = Packet.codec(
        ClientboundTakeItemEntityPacket::write, ClientboundTakeItemEntityPacket::new
    );
    private final int itemId;
    private final int playerId;
    private final int amount;

    public ClientboundTakeItemEntityPacket(int pItemId, int pPlayerId, int pAmount) {
        this.itemId = pItemId;
        this.playerId = pPlayerId;
        this.amount = pAmount;
    }

    private ClientboundTakeItemEntityPacket(FriendlyByteBuf pBuffer) {
        this.itemId = pBuffer.readVarInt();
        this.playerId = pBuffer.readVarInt();
        this.amount = pBuffer.readVarInt();
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.itemId);
        pBuffer.writeVarInt(this.playerId);
        pBuffer.writeVarInt(this.amount);
    }

    @Override
    public PacketType<ClientboundTakeItemEntityPacket> type() {
        return GamePacketTypes.CLIENTBOUND_TAKE_ITEM_ENTITY;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleTakeItemEntity(this);
    }

    public int getItemId() {
        return this.itemId;
    }

    public int getPlayerId() {
        return this.playerId;
    }

    public int getAmount() {
        return this.amount;
    }
}