package sisicat.events;

import com.darkmagician6.eventapi.events.Event;

public class TickEvent implements Event {

    public boolean isPost = false;

    public TickEvent(boolean isPost) {
        this.isPost = isPost;
    }

}
