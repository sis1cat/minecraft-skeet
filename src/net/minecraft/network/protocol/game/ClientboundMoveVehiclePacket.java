package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public record ClientboundMoveVehiclePacket(Vec3 position, float yRot, float xRot) implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundMoveVehiclePacket> STREAM_CODEC = StreamCodec.composite(
        Vec3.STREAM_CODEC,
        ClientboundMoveVehiclePacket::position,
        ByteBufCodecs.FLOAT,
        ClientboundMoveVehiclePacket::yRot,
        ByteBufCodecs.FLOAT,
        ClientboundMoveVehiclePacket::xRot,
        ClientboundMoveVehiclePacket::new
    );

    public static ClientboundMoveVehiclePacket fromEntity(Entity pEntity) {
        return new ClientboundMoveVehiclePacket(pEntity.position(), pEntity.getYRot(), pEntity.getXRot());
    }

    @Override
    public PacketType<ClientboundMoveVehiclePacket> type() {
        return GamePacketTypes.CLIENTBOUND_MOVE_VEHICLE;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleMoveVehicle(this);
    }
}