package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ServerboundSelectBundleItemPacket(int slotId, int selectedItemIndex) implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundSelectBundleItemPacket> STREAM_CODEC = Packet.codec(
        ServerboundSelectBundleItemPacket::write, ServerboundSelectBundleItemPacket::new
    );

    private ServerboundSelectBundleItemPacket(FriendlyByteBuf pBuffer) {
        this(pBuffer.readVarInt(), pBuffer.readVarInt());
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.slotId);
        pBuffer.writeVarInt(this.selectedItemIndex);
    }

    @Override
    public PacketType<ServerboundSelectBundleItemPacket> type() {
        return GamePacketTypes.SERVERBOUND_BUNDLE_ITEM_SELECTED;
    }

    public void handle(ServerGamePacketListener p_361569_) {
        p_361569_.handleBundleItemSelectedPacket(this);
    }
}