package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.Difficulty;

public class ServerboundChangeDifficultyPacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundChangeDifficultyPacket> STREAM_CODEC = Packet.codec(
        ServerboundChangeDifficultyPacket::write, ServerboundChangeDifficultyPacket::new
    );
    private final Difficulty difficulty;

    public ServerboundChangeDifficultyPacket(Difficulty pDifficulty) {
        this.difficulty = pDifficulty;
    }

    private ServerboundChangeDifficultyPacket(FriendlyByteBuf pBuffer) {
        this.difficulty = Difficulty.byId(pBuffer.readUnsignedByte());
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeByte(this.difficulty.getId());
    }

    @Override
    public PacketType<ServerboundChangeDifficultyPacket> type() {
        return GamePacketTypes.SERVERBOUND_CHANGE_DIFFICULTY;
    }

    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleChangeDifficulty(this);
    }

    public Difficulty getDifficulty() {
        return this.difficulty;
    }
}