package net.minecraft.server.network;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.profiling.Profiler;
import org.slf4j.Logger;

public abstract class ServerCommonPacketListenerImpl implements ServerCommonPacketListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int LATENCY_CHECK_INTERVAL = 15000;
    private static final int CLOSED_LISTENER_TIMEOUT = 15000;
    private static final Component TIMEOUT_DISCONNECTION_MESSAGE = Component.translatable("disconnect.timeout");
    static final Component DISCONNECT_UNEXPECTED_QUERY = Component.translatable("multiplayer.disconnect.unexpected_query_response");
    protected final MinecraftServer server;
    protected final Connection connection;
    private final boolean transferred;
    private long keepAliveTime;
    private boolean keepAlivePending;
    private long keepAliveChallenge;
    private long closedListenerTime;
    private boolean closed = false;
    private int latency;
    private volatile boolean suspendFlushingOnServerThread = false;

    public ServerCommonPacketListenerImpl(MinecraftServer pServer, Connection pConnection, CommonListenerCookie pCookie) {
        this.server = pServer;
        this.connection = pConnection;
        this.keepAliveTime = Util.getMillis();
        this.latency = pCookie.latency();
        this.transferred = pCookie.transferred();
    }

    private void close() {
        if (!this.closed) {
            this.closedListenerTime = Util.getMillis();
            this.closed = true;
        }
    }

    @Override
    public void onDisconnect(DisconnectionDetails p_343448_) {
        if (this.isSingleplayerOwner()) {
            LOGGER.info("Stopping singleplayer server as player logged out");
            this.server.halt(false);
        }
    }

    @Override
    public void onPacketError(Packet p_364882_, Exception p_362532_) throws ReportedException {
        ServerCommonPacketListener.super.onPacketError(p_364882_, p_362532_);
        this.server.reportPacketHandlingException(p_362532_, p_364882_.type());
    }

    @Override
    public void handleKeepAlive(ServerboundKeepAlivePacket p_299975_) {
        if (this.keepAlivePending && p_299975_.getId() == this.keepAliveChallenge) {
            int i = (int)(Util.getMillis() - this.keepAliveTime);
            this.latency = (this.latency * 3 + i) / 4;
            this.keepAlivePending = false;
        } else if (!this.isSingleplayerOwner()) {
            this.disconnect(TIMEOUT_DISCONNECTION_MESSAGE);
        }
    }

    @Override
    public void handlePong(ServerboundPongPacket p_299461_) {
    }

    @Override
    public void handleCustomPayload(ServerboundCustomPayloadPacket p_300164_) {
    }

    @Override
    public void handleResourcePackResponse(ServerboundResourcePackPacket p_300656_) {
        PacketUtils.ensureRunningOnSameThread(p_300656_, this, this.server);
        if (p_300656_.action() == ServerboundResourcePackPacket.Action.DECLINED && this.server.isResourcePackRequired()) {
            LOGGER.info("Disconnecting {} due to resource pack {} rejection", this.playerProfile().getName(), p_300656_.id());
            this.disconnect(Component.translatable("multiplayer.requiredTexturePrompt.disconnect"));
        }
    }

    @Override
    public void handleCookieResponse(ServerboundCookieResponsePacket p_335443_) {
        this.disconnect(DISCONNECT_UNEXPECTED_QUERY);
    }

    protected void keepConnectionAlive() {
        Profiler.get().push("keepAlive");
        long i = Util.getMillis();
        if (!this.isSingleplayerOwner() && i - this.keepAliveTime >= 15000L) {
            if (this.keepAlivePending) {
                this.disconnect(TIMEOUT_DISCONNECTION_MESSAGE);
            } else if (this.checkIfClosed(i)) {
                this.keepAlivePending = true;
                this.keepAliveTime = i;
                this.keepAliveChallenge = i;
                this.send(new ClientboundKeepAlivePacket(this.keepAliveChallenge));
            }
        }

        Profiler.get().pop();
    }

    private boolean checkIfClosed(long pTime) {
        if (this.closed) {
            if (pTime - this.closedListenerTime >= 15000L) {
                this.disconnect(TIMEOUT_DISCONNECTION_MESSAGE);
            }

            return false;
        } else {
            return true;
        }
    }

    public void suspendFlushing() {
        this.suspendFlushingOnServerThread = true;
    }

    public void resumeFlushing() {
        this.suspendFlushingOnServerThread = false;
        this.connection.flushChannel();
    }

    public void send(Packet<?> pPacket) {
        this.send(pPacket, null);
    }

    public void send(Packet<?> pPacket, @Nullable PacketSendListener pListener) {
        if (pPacket.isTerminal()) {
            this.close();
        }

        boolean flag = !this.suspendFlushingOnServerThread || !this.server.isSameThread();

        try {
            this.connection.send(pPacket, pListener, flag);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Sending packet");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Packet being sent");
            crashreportcategory.setDetail("Packet class", () -> pPacket.getClass().getCanonicalName());
            throw new ReportedException(crashreport);
        }
    }

    public void disconnect(Component pReason) {
        this.disconnect(new DisconnectionDetails(pReason));
    }

    public void disconnect(DisconnectionDetails pDisconnectionDetails) {
        this.connection
            .send(new ClientboundDisconnectPacket(pDisconnectionDetails.reason()), PacketSendListener.thenRun(() -> this.connection.disconnect(pDisconnectionDetails)));
        this.connection.setReadOnly();
        this.server.executeBlocking(this.connection::handleDisconnection);
    }

    protected boolean isSingleplayerOwner() {
        return this.server.isSingleplayerOwner(this.playerProfile());
    }

    protected abstract GameProfile playerProfile();

    @VisibleForDebug
    public GameProfile getOwner() {
        return this.playerProfile();
    }

    public int latency() {
        return this.latency;
    }

    protected CommonListenerCookie createCookie(ClientInformation pClientInformation) {
        return new CommonListenerCookie(this.playerProfile(), this.latency, pClientInformation, this.transferred);
    }
}