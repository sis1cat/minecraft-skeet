package net.minecraft.network.protocol.login;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.cookie.ClientCookiePacketListener;

public interface ClientLoginPacketListener extends ClientCookiePacketListener {
    @Override
    default ConnectionProtocol protocol() {
        return ConnectionProtocol.LOGIN;
    }

    void handleHello(ClientboundHelloPacket pPacket);

    void handleLoginFinished(ClientboundLoginFinishedPacket pPacket);

    void handleDisconnect(ClientboundLoginDisconnectPacket pPacket);

    void handleCompression(ClientboundLoginCompressionPacket pPacket);

    void handleCustomQuery(ClientboundCustomQueryPacket pPacket);
}