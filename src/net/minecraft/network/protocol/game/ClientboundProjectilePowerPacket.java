package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundProjectilePowerPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundProjectilePowerPacket> STREAM_CODEC = Packet.codec(
        ClientboundProjectilePowerPacket::write, ClientboundProjectilePowerPacket::new
    );
    private final int id;
    private final double accelerationPower;

    public ClientboundProjectilePowerPacket(int pId, double pAccelerationPower) {
        this.id = pId;
        this.accelerationPower = pAccelerationPower;
    }

    private ClientboundProjectilePowerPacket(FriendlyByteBuf pBuffer) {
        this.id = pBuffer.readVarInt();
        this.accelerationPower = pBuffer.readDouble();
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.id);
        pBuffer.writeDouble(this.accelerationPower);
    }

    @Override
    public PacketType<ClientboundProjectilePowerPacket> type() {
        return GamePacketTypes.CLIENTBOUND_PROJECTILE_POWER;
    }

    public void handle(ClientGamePacketListener p_329858_) {
        p_329858_.handleProjectilePowerPacket(this);
    }

    public int getId() {
        return this.id;
    }

    public double getAccelerationPower() {
        return this.accelerationPower;
    }
}