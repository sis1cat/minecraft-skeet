package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundRenameItemPacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundRenameItemPacket> STREAM_CODEC = Packet.codec(
        ServerboundRenameItemPacket::write, ServerboundRenameItemPacket::new
    );
    private final String name;

    public ServerboundRenameItemPacket(String pName) {
        this.name = pName;
    }

    private ServerboundRenameItemPacket(FriendlyByteBuf pBuffer) {
        this.name = pBuffer.readUtf();
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeUtf(this.name);
    }

    @Override
    public PacketType<ServerboundRenameItemPacket> type() {
        return GamePacketTypes.SERVERBOUND_RENAME_ITEM;
    }

    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleRenameItem(this);
    }

    public String getName() {
        return this.name;
    }
}