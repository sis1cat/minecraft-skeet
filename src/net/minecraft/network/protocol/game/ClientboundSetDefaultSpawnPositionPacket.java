package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundSetDefaultSpawnPositionPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundSetDefaultSpawnPositionPacket> STREAM_CODEC = Packet.codec(
        ClientboundSetDefaultSpawnPositionPacket::write, ClientboundSetDefaultSpawnPositionPacket::new
    );
    private final BlockPos pos;
    private final float angle;

    public ClientboundSetDefaultSpawnPositionPacket(BlockPos pPos, float pAngle) {
        this.pos = pPos;
        this.angle = pAngle;
    }

    private ClientboundSetDefaultSpawnPositionPacket(FriendlyByteBuf pBuffer) {
        this.pos = pBuffer.readBlockPos();
        this.angle = pBuffer.readFloat();
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeBlockPos(this.pos);
        pBuffer.writeFloat(this.angle);
    }

    @Override
    public PacketType<ClientboundSetDefaultSpawnPositionPacket> type() {
        return GamePacketTypes.CLIENTBOUND_SET_DEFAULT_SPAWN_POSITION;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleSetSpawn(this);
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public float getAngle() {
        return this.angle;
    }
}