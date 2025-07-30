package net.minecraft.network.protocol.game;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.syncher.SynchedEntityData;

public record ClientboundSetEntityDataPacket(int id, List<SynchedEntityData.DataValue<?>> packedItems) implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSetEntityDataPacket> STREAM_CODEC = Packet.codec(
        ClientboundSetEntityDataPacket::write, ClientboundSetEntityDataPacket::new
    );
    public static final int EOF_MARKER = 255;

    private ClientboundSetEntityDataPacket(RegistryFriendlyByteBuf pBuffer) {
        this(pBuffer.readVarInt(), unpack(pBuffer));
    }

    private static void pack(List<SynchedEntityData.DataValue<?>> pDataValues, RegistryFriendlyByteBuf pBuffer) {
        for (SynchedEntityData.DataValue<?> datavalue : pDataValues) {
            datavalue.write(pBuffer);
        }

        pBuffer.writeByte(255);
    }

    private static List<SynchedEntityData.DataValue<?>> unpack(RegistryFriendlyByteBuf pBuffer) {
        List<SynchedEntityData.DataValue<?>> list = new ArrayList<>();

        int i;
        while ((i = pBuffer.readUnsignedByte()) != 255) {
            list.add(SynchedEntityData.DataValue.read(pBuffer, i));
        }

        return list;
    }

    private void write(RegistryFriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.id);
        pack(this.packedItems, pBuffer);
    }

    @Override
    public PacketType<ClientboundSetEntityDataPacket> type() {
        return GamePacketTypes.CLIENTBOUND_SET_ENTITY_DATA;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleSetEntityData(this);
    }
}