package sisicat.main.functions.miscellaneous;

import com.darkmagician6.eventapi.EventTarget;
import sisicat.events.AttackAnimationEvent;
import sisicat.main.functions.Function;
import sisicat.main.functions.FunctionSetting;

public class CustomViewModelFunction extends Function {

    private final FunctionSetting positionX;
    private final FunctionSetting positionY;
    private final FunctionSetting positionZ;
    private final FunctionSetting rotationX;
    private final FunctionSetting rotationY;
    private final FunctionSetting rotationZ;

    public CustomViewModelFunction(String name) {
        super(name);

        this.positionX = new FunctionSetting("Position X", 0.3f, -1, 1, "", 0.1f);
        this.positionY = new FunctionSetting("Position Y", 0f, -1, 1, "", 0.1f);
        this.positionZ = new FunctionSetting("Position Z", -0.5f, -1, 1, "", 0.1f);
        this.rotationX = new FunctionSetting("Rotation X", 90, -180, 180, "\u00B0", 1f);
        this.rotationY = new FunctionSetting("Rotation Y", -45, -180, 180, "\u00B0", 1f);
        this.rotationZ = new FunctionSetting("Rotation Z", -45, -180, 180, "\u00B0", 1f);

        this.addSetting(
                positionX,
                positionY,
                positionZ,
                rotationX,
                rotationY,
                rotationZ
        );

    }

    @EventTarget
    void _event(AttackAnimationEvent attackAnimationEvent) {

        attackAnimationEvent.viewModel.x = positionX.getFloatValue();
        attackAnimationEvent.viewModel.y = positionY.getFloatValue();
        attackAnimationEvent.viewModel.z = positionZ.getFloatValue();

        attackAnimationEvent.staticXRots.x = rotationX.getFloatValue();
        attackAnimationEvent.staticXRots.y = rotationY.getFloatValue();
        attackAnimationEvent.staticXRots.z = rotationZ.getFloatValue();

    }

}
