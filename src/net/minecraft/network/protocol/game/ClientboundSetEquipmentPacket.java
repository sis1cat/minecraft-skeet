package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class ClientboundSetEquipmentPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSetEquipmentPacket> STREAM_CODEC = Packet.codec(
        ClientboundSetEquipmentPacket::write, ClientboundSetEquipmentPacket::new
    );
    private static final byte CONTINUE_MASK = -128;
    private final int entity;
    private final List<Pair<EquipmentSlot, ItemStack>> slots;

    public ClientboundSetEquipmentPacket(int pEntity, List<Pair<EquipmentSlot, ItemStack>> pSlots) {
        this.entity = pEntity;
        this.slots = pSlots;
    }

    private ClientboundSetEquipmentPacket(RegistryFriendlyByteBuf pBuffer) {
        this.entity = pBuffer.readVarInt();
        this.slots = Lists.newArrayList();

        int i;
        do {
            i = pBuffer.readByte();
            EquipmentSlot equipmentslot = EquipmentSlot.VALUES.get(i & 127);
            ItemStack itemstack = ItemStack.OPTIONAL_STREAM_CODEC.decode(pBuffer);
            this.slots.add(Pair.of(equipmentslot, itemstack));
        } while ((i & -128) != 0);
    }

    private void write(RegistryFriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.entity);
        int i = this.slots.size();

        for (int j = 0; j < i; j++) {
            Pair<EquipmentSlot, ItemStack> pair = this.slots.get(j);
            EquipmentSlot equipmentslot = pair.getFirst();
            boolean flag = j != i - 1;
            int k = equipmentslot.ordinal();
            pBuffer.writeByte(flag ? k | -128 : k);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(pBuffer, pair.getSecond());
        }
    }

    @Override
    public PacketType<ClientboundSetEquipmentPacket> type() {
        return GamePacketTypes.CLIENTBOUND_SET_EQUIPMENT;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleSetEquipment(this);
    }

    public int getEntity() {
        return this.entity;
    }

    public List<Pair<EquipmentSlot, ItemStack>> getSlots() {
        return this.slots;
    }
}