package sisicat.events;

import com.darkmagician6.eventapi.events.callables.EventCancellable;

public class MouseHandlerOnActionEvent extends EventCancellable {

    public enum TYPE {
        GRAB_MOUSE
    }

    private final TYPE type;

    public MouseHandlerOnActionEvent(TYPE type){
        this.type = type;
    }

    public MouseHandlerOnActionEvent(){
        this.type = null;
    }

    public TYPE getType(){
        return type;
    }

}
