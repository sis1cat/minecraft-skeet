package sisicat.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;

public class SprintEvent extends EventCancellable {
    boolean flag;
    boolean serverState;

    public SprintEvent(boolean flag, boolean serverState)
    {
        this.flag = flag;
        this.serverState = serverState;
    }

    public boolean getFlag(){ return this.flag; }
    public void setFlag(boolean flag){ this.flag = flag; }

    public boolean getServerState(){ return this.serverState; }

}
