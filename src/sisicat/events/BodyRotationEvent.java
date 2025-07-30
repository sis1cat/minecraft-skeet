package sisicat.events;

import com.darkmagician6.eventapi.events.Event;

public class BodyRotationEvent implements Event {

    public float yaw;

    public BodyRotationEvent(float yaw) {
        this.yaw = yaw;
    }

}
