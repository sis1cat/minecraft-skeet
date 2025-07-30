package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public abstract class ClientboundMoveEntityPacket implements Packet<ClientGamePacketListener> {
    protected final int entityId;
    protected final short xa;
    protected final short ya;
    protected final short za;
    protected final byte yRot;
    protected final byte xRot;
    protected final boolean onGround;
    protected final boolean hasRot;
    protected final boolean hasPos;

    protected ClientboundMoveEntityPacket(
        int pEntityId,
        short pXa,
        short pYa,
        short pZa,
        byte pYRot,
        byte pXRot,
        boolean pOnGround,
        boolean pHasRot,
        boolean pHasPos
    ) {
        this.entityId = pEntityId;
        this.xa = pXa;
        this.ya = pYa;
        this.za = pZa;
        this.yRot = pYRot;
        this.xRot = pXRot;
        this.onGround = pOnGround;
        this.hasRot = pHasRot;
        this.hasPos = pHasPos;
    }

    @Override
    public abstract PacketType<? extends ClientboundMoveEntityPacket> type();

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleMoveEntity(this);
    }

    @Override
    public String toString() {
        return "Entity_" + super.toString();
    }

    @Nullable
    public Entity getEntity(Level pLevel) {
        return pLevel.getEntity(this.entityId);
    }

    public short getXa() {
        return this.xa;
    }

    public short getYa() {
        return this.ya;
    }

    public short getZa() {
        return this.za;
    }

    public float getyRot() {
        return Mth.unpackDegrees(this.yRot);
    }

    public float getxRot() {
        return Mth.unpackDegrees(this.xRot);
    }

    public boolean hasRotation() {
        return this.hasRot;
    }

    public boolean hasPosition() {
        return this.hasPos;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public static class Pos extends ClientboundMoveEntityPacket {
        public static final StreamCodec<FriendlyByteBuf, ClientboundMoveEntityPacket.Pos> STREAM_CODEC = Packet.codec(
            ClientboundMoveEntityPacket.Pos::write, ClientboundMoveEntityPacket.Pos::read
        );

        public Pos(int pEntityId, short pXa, short pYa, short pZa, boolean pOnGround) {
            super(pEntityId, pXa, pYa, pZa, (byte)0, (byte)0, pOnGround, false, true);
        }

        private static ClientboundMoveEntityPacket.Pos read(FriendlyByteBuf pBuffer) {
            int i = pBuffer.readVarInt();
            short short1 = pBuffer.readShort();
            short short2 = pBuffer.readShort();
            short short3 = pBuffer.readShort();
            boolean flag = pBuffer.readBoolean();
            return new ClientboundMoveEntityPacket.Pos(i, short1, short2, short3, flag);
        }

        private void write(FriendlyByteBuf pBuffer) {
            pBuffer.writeVarInt(this.entityId);
            pBuffer.writeShort(this.xa);
            pBuffer.writeShort(this.ya);
            pBuffer.writeShort(this.za);
            pBuffer.writeBoolean(this.onGround);
        }

        @Override
        public PacketType<ClientboundMoveEntityPacket.Pos> type() {
            return GamePacketTypes.CLIENTBOUND_MOVE_ENTITY_POS;
        }
    }

    public static class PosRot extends ClientboundMoveEntityPacket {
        public static final StreamCodec<FriendlyByteBuf, ClientboundMoveEntityPacket.PosRot> STREAM_CODEC = Packet.codec(
            ClientboundMoveEntityPacket.PosRot::write, ClientboundMoveEntityPacket.PosRot::read
        );

        public PosRot(int pEntityId, short pXa, short pYa, short pZa, byte pYRot, byte pXRot, boolean pOnGround) {
            super(pEntityId, pXa, pYa, pZa, pYRot, pXRot, pOnGround, true, true);
        }

        private static ClientboundMoveEntityPacket.PosRot read(FriendlyByteBuf pBuffer) {
            int i = pBuffer.readVarInt();
            short short1 = pBuffer.readShort();
            short short2 = pBuffer.readShort();
            short short3 = pBuffer.readShort();
            byte b0 = pBuffer.readByte();
            byte b1 = pBuffer.readByte();
            boolean flag = pBuffer.readBoolean();
            return new ClientboundMoveEntityPacket.PosRot(i, short1, short2, short3, b0, b1, flag);
        }

        private void write(FriendlyByteBuf pBuffer) {
            pBuffer.writeVarInt(this.entityId);
            pBuffer.writeShort(this.xa);
            pBuffer.writeShort(this.ya);
            pBuffer.writeShort(this.za);
            pBuffer.writeByte(this.yRot);
            pBuffer.writeByte(this.xRot);
            pBuffer.writeBoolean(this.onGround);
        }

        @Override
        public PacketType<ClientboundMoveEntityPacket.PosRot> type() {
            return GamePacketTypes.CLIENTBOUND_MOVE_ENTITY_POS_ROT;
        }
    }

    public static class Rot extends ClientboundMoveEntityPacket {
        public static final StreamCodec<FriendlyByteBuf, ClientboundMoveEntityPacket.Rot> STREAM_CODEC = Packet.codec(
            ClientboundMoveEntityPacket.Rot::write, ClientboundMoveEntityPacket.Rot::read
        );

        public Rot(int pEntityId, byte pYRot, byte pXRot, boolean pOnGround) {
            super(pEntityId, (short)0, (short)0, (short)0, pYRot, pXRot, pOnGround, true, false);
        }

        private static ClientboundMoveEntityPacket.Rot read(FriendlyByteBuf pBuffer) {
            int i = pBuffer.readVarInt();
            byte b0 = pBuffer.readByte();
            byte b1 = pBuffer.readByte();
            boolean flag = pBuffer.readBoolean();
            return new ClientboundMoveEntityPacket.Rot(i, b0, b1, flag);
        }

        private void write(FriendlyByteBuf pBuffer) {
            pBuffer.writeVarInt(this.entityId);
            pBuffer.writeByte(this.yRot);
            pBuffer.writeByte(this.xRot);
            pBuffer.writeBoolean(this.onGround);
        }

        @Override
        public PacketType<ClientboundMoveEntityPacket.Rot> type() {
            return GamePacketTypes.CLIENTBOUND_MOVE_ENTITY_ROT;
        }
    }
}