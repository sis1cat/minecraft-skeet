package sisicat.main.functions.miscellaneous;

import com.darkmagician6.eventapi.EventTarget;
import sisicat.events.TickEvent;
import sisicat.main.functions.Function;
import sisicat.main.functions.FunctionSetting;

public class OverrideFOVFunction extends Function {

    private final FunctionSetting fov;

    public OverrideFOVFunction(String name) {
        super(name);

        fov = new FunctionSetting(
                "Override FOV", 115,
                10, 140,
                "\u00B0", 1
        );

        this.addSetting(fov);

        this.setCanBeActivated(true);

    }

    @EventTarget
    void _event(TickEvent tickEvent) {

        mc.options.fov().set((int) fov.getFloatValue());

    }

}
