package net.minecraft.network.protocol.game;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ClientboundRespawnPacket(CommonPlayerSpawnInfo commonPlayerSpawnInfo, byte dataToKeep) implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundRespawnPacket> STREAM_CODEC = Packet.codec(
        ClientboundRespawnPacket::write, ClientboundRespawnPacket::new
    );
    public static final byte KEEP_ATTRIBUTE_MODIFIERS = 1;
    public static final byte KEEP_ENTITY_DATA = 2;
    public static final byte KEEP_ALL_DATA = 3;

    private ClientboundRespawnPacket(RegistryFriendlyByteBuf pBuffer) {
        this(new CommonPlayerSpawnInfo(pBuffer), pBuffer.readByte());
    }

    private void write(RegistryFriendlyByteBuf pBuffer) {
        this.commonPlayerSpawnInfo.write(pBuffer);
        pBuffer.writeByte(this.dataToKeep);
    }

    @Override
    public PacketType<ClientboundRespawnPacket> type() {
        return GamePacketTypes.CLIENTBOUND_RESPAWN;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleRespawn(this);
    }

    public boolean shouldKeep(byte pData) {
        return (this.dataToKeep & pData) != 0;
    }
}