package sisicat.events;

import com.darkmagician6.eventapi.events.Event;

public class MovementFixEvent implements Event {
    public float yaw;

    public MovementFixEvent(float yaw)
    {
        this.yaw = yaw;
    }

}
