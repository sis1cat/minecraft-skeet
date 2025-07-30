package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ServerboundChatSessionUpdatePacket(RemoteChatSession.Data chatSession) implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundChatSessionUpdatePacket> STREAM_CODEC = Packet.codec(
        ServerboundChatSessionUpdatePacket::write, ServerboundChatSessionUpdatePacket::new
    );

    private ServerboundChatSessionUpdatePacket(FriendlyByteBuf pBuffer) {
        this(RemoteChatSession.Data.read(pBuffer));
    }

    private void write(FriendlyByteBuf pBuffer) {
        RemoteChatSession.Data.write(pBuffer, this.chatSession);
    }

    @Override
    public PacketType<ServerboundChatSessionUpdatePacket> type() {
        return GamePacketTypes.SERVERBOUND_CHAT_SESSION_UPDATE;
    }

    public void handle(ServerGamePacketListener p_253620_) {
        p_253620_.handleChatSessionUpdate(this);
    }
}