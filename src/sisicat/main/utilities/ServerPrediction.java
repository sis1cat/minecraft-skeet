package sisicat.main.utilities;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import com.darkmagician6.eventapi.events.Event;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import sisicat.IDefault;
import sisicat.events.PacketReceiveEvent;

import java.util.Objects;
import java.util.Optional;

public class ServerPrediction implements IDefault { // just for test, u can remove it

    static {
        EventManager.register(new ServerPrediction());
    }

    private static float getClientPing() {

        if(mc.player == null)
            return 0;

        return Optional.ofNullable(mc.getConnection())
                .map(c -> c.getPlayerInfo(mc.player.getUUID()))
                .map(PlayerInfo::getLatency)
                .orElse(0) / 2f;

    }

    public static float getAttackStrengthScale() {

        if(mc.player == null)
            return 0f;

        float adjust = (mc.getCurrentServer() != null ? (getClientPing() / 1000f) * 20f : 0);

        return mc.player.getAttackStrengthScale(0.5F);

    }

    public static boolean isClientGrounded() {

        if(mc.player == null)
            return false;

        long timeDelta = System.currentTimeMillis() - groundPacketTimePoint;

        return timeDelta > getClientPing() ? mc.player.wasGrounded : prevWasGrounded;

    }

    private static long groundPacketTimePoint = Long.MAX_VALUE;
    private static boolean prevWasGrounded = false;

    @EventTarget
    private void event(AttackStrengthTickerFix ignored) {

        if(mc.player == null)
            return;

        mc.player.resetAttackStrengthTicker();

    }

    @EventTarget
    private void event(PacketReceiveEvent<?> packetReceiveEvent) {

        if (mc.level == null || mc.player == null)
            return;

        if (packetReceiveEvent.getPacket() instanceof ClientboundMoveEntityPacket clientboundMoveEntityPacket && clientboundMoveEntityPacket.getEntity(mc.level) == mc.player) {
            prevWasGrounded = mc.player.wasGrounded;
            mc.player.wasGrounded = clientboundMoveEntityPacket.isOnGround();
            groundPacketTimePoint = System.currentTimeMillis();
        }

    }

    public static class AttackStrengthTickerFix implements Event {
    }

}
