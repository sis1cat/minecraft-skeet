package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.ResourceLocation;

public class ClientboundSelectAdvancementsTabPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundSelectAdvancementsTabPacket> STREAM_CODEC = Packet.codec(
        ClientboundSelectAdvancementsTabPacket::write, ClientboundSelectAdvancementsTabPacket::new
    );
    @Nullable
    private final ResourceLocation tab;

    public ClientboundSelectAdvancementsTabPacket(@Nullable ResourceLocation pTab) {
        this.tab = pTab;
    }

    private ClientboundSelectAdvancementsTabPacket(FriendlyByteBuf pBuffer) {
        this.tab = pBuffer.readNullable(FriendlyByteBuf::readResourceLocation);
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeNullable(this.tab, FriendlyByteBuf::writeResourceLocation);
    }

    @Override
    public PacketType<ClientboundSelectAdvancementsTabPacket> type() {
        return GamePacketTypes.CLIENTBOUND_SELECT_ADVANCEMENTS_TAB;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleSelectAdvancementsTab(this);
    }

    @Nullable
    public ResourceLocation getTab() {
        return this.tab;
    }
}