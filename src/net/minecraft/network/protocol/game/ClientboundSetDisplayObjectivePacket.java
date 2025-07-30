package net.minecraft.network.protocol.game;

import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;

public class ClientboundSetDisplayObjectivePacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundSetDisplayObjectivePacket> STREAM_CODEC = Packet.codec(
        ClientboundSetDisplayObjectivePacket::write, ClientboundSetDisplayObjectivePacket::new
    );
    private final DisplaySlot slot;
    private final String objectiveName;

    public ClientboundSetDisplayObjectivePacket(DisplaySlot pSlot, @Nullable Objective pObjective) {
        this.slot = pSlot;
        if (pObjective == null) {
            this.objectiveName = "";
        } else {
            this.objectiveName = pObjective.getName();
        }
    }

    private ClientboundSetDisplayObjectivePacket(FriendlyByteBuf pBuffer) {
        this.slot = pBuffer.readById(DisplaySlot.BY_ID);
        this.objectiveName = pBuffer.readUtf();
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeById(DisplaySlot::id, this.slot);
        pBuffer.writeUtf(this.objectiveName);
    }

    @Override
    public PacketType<ClientboundSetDisplayObjectivePacket> type() {
        return GamePacketTypes.CLIENTBOUND_SET_DISPLAY_OBJECTIVE;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleSetDisplayObjective(this);
    }

    public DisplaySlot getSlot() {
        return this.slot;
    }

    @Nullable
    public String getObjectiveName() {
        return Objects.equals(this.objectiveName, "") ? null : this.objectiveName;
    }
}