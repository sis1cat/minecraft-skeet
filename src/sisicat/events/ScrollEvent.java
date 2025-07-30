package sisicat.events;

import com.darkmagician6.eventapi.events.Event;

public class ScrollEvent implements Event {

    private final float y;

    public ScrollEvent(float y)
    {
        this.y = y;
    }

    public float getY()
    {
        return y;
    }

}