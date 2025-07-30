package sisicat.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.EventStoppable;

public class MouseEvent extends EventStoppable implements Event {

    private final int button;
    private final int action;
    private final float x;
    private final float y;
    private boolean isAlreadyUsed = false;

    public MouseEvent(float x, float y, int button, int action)
    {
        this.button = button;
        this.action = action;
        this.x = x;
        this.y = y;
    }

    public int getButton()
    {
        return button;
    }

    public int getAction()
    {
        return action;
    }

    public float getX()
    {
        return x;
    }

    public float getY()
    {
        return y;
    }

    public boolean getIsAlreadyUsed(){
        return isAlreadyUsed;
    }

    public void setIsAlreadyUsed(boolean isAlreadyUsed){
        this.isAlreadyUsed = isAlreadyUsed;
    }

}
