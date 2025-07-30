package sisicat.events;

import com.darkmagician6.eventapi.events.Event;

public class MouseMoveEvent implements Event {

    private final float x;
    private final float y;

    public MouseMoveEvent(float x, float y)
    {
        this.x = x;
        this.y = y;
    }


    public float getX()
    {
        return x;
    }

    public float getY()
    {
        return y;
    }

}
