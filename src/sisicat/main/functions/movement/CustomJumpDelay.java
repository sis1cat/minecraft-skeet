package sisicat.main.functions.movement;

import com.darkmagician6.eventapi.EventTarget;
import sisicat.IDefault;
import sisicat.events.MovementUpdateEvent;
import sisicat.events.TickEvent;
import sisicat.main.functions.Function;
import sisicat.main.functions.FunctionSetting;

public class CustomJumpDelay extends Function {

    private final FunctionSetting delay;

    public CustomJumpDelay(String name){
        super(name);

        delay = new FunctionSetting(
                "Delay",
                5, 3, 10,
                "t",1
        );

        addSetting(delay);

    }

    @EventTarget
    void _event(TickEvent tickEvent){
        if(mc.player.noJumpDelay == 10)
            mc.player.noJumpDelay = (int) delay.getFloatValue();
    }

}
