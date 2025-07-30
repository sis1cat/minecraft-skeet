package sisicat.main.functions.movement;

import com.darkmagician6.eventapi.EventTarget;
import sisicat.events.TickEvent;
import sisicat.main.functions.Function;

public class AutomaticSprint extends Function {

    public AutomaticSprint(String name) {
        super(name);
    }

    @EventTarget
    void _event(TickEvent ignored) {
        mc.options.keySprint.setDown(true);
    }

}
