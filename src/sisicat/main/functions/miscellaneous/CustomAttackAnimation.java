package sisicat.main.functions.miscellaneous;

import com.darkmagician6.eventapi.EventTarget;
import sisicat.events.AttackAnimationEvent;
import sisicat.main.functions.Function;
import sisicat.main.functions.FunctionSetting;

public class CustomAttackAnimation extends Function {

    private final FunctionSetting amplitude;
    private final FunctionSetting animationRotX;
    private final FunctionSetting animationRotY;
    private final FunctionSetting animationRotZ;
    private final FunctionSetting strength;

    public CustomAttackAnimation(String name) {
        super(name);

        this.amplitude = new FunctionSetting("Swing amplitude", -20, -90, 90, "\u00B0", 1);
        this.strength = new FunctionSetting("Position animation intensity Y", 0, 0, 1, "", 0.1f);
        this.animationRotX = new FunctionSetting("Rotation animation intensity X", 1, 0, 2, "", 0.1f);
        this.animationRotY = new FunctionSetting("Rotation animation intensity Y", 1, 0, 2, "", 0.1f);
        this.animationRotZ = new FunctionSetting("Rotation animation intensity Z", 2, 0, 2, "", 0.1f);


        this.addSetting(
                this.amplitude,
                this.strength,
                this.animationRotX,
                this.animationRotY,
                this.animationRotZ
        );

    }

    @EventTarget
    void _event(AttackAnimationEvent attackAnimationEvent) {

        attackAnimationEvent.amplitude = amplitude.getFloatValue();
        attackAnimationEvent.strength = strength.getFloatValue();
        attackAnimationEvent.animationXRots.x = animationRotX.getFloatValue();
        attackAnimationEvent.animationXRots.y = animationRotY.getFloatValue();
        attackAnimationEvent.animationXRots.z = animationRotZ.getFloatValue();

    }

}
