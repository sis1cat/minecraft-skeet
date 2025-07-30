package net.minecraft.world.level.gameevent;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public interface GameEventListener {
    PositionSource getListenerSource();

    int getListenerRadius();

    boolean handleGameEvent(ServerLevel pLevel, Holder<GameEvent> pGameEvent, GameEvent.Context pContext, Vec3 pPos);

    default GameEventListener.DeliveryMode getDeliveryMode() {
        return GameEventListener.DeliveryMode.UNSPECIFIED;
    }

    public static enum DeliveryMode {
        UNSPECIFIED,
        BY_DISTANCE;
    }

    public interface Provider<T extends GameEventListener> {
        T getListener();
    }
}