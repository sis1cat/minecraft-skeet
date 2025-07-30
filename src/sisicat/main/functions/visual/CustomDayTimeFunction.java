package sisicat.main.functions.visual;

import com.darkmagician6.eventapi.EventTarget;
import sisicat.events.ClientLevelTickEvent;
import sisicat.main.functions.Function;
import sisicat.main.functions.FunctionSetting;

public class CustomDayTimeFunction extends Function {

    private final FunctionSetting
            gameTime;

    public CustomDayTimeFunction(String name) {
        super(name);

        gameTime =
                new FunctionSetting(
                        "Ticks", 12,
                        0, 24,
                        "kt", 0.1f
                );

        this.addSetting(gameTime);

    }

    @EventTarget
    void _event(ClientLevelTickEvent ignored){
        mc.level.getLevelData().setDayTime((long) (gameTime.getFloatValue() * 1000));
    }

}
