package sisicat.events;

import com.darkmagician6.eventapi.events.Event;

public class PlayerModelRotateEvent implements Event {

    private float yaw;

    public PlayerModelRotateEvent(float yaw){
        this.yaw = yaw;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

}
