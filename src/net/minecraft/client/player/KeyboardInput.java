package net.minecraft.client.player;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.client.Options;
import net.minecraft.world.entity.player.Input;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import sisicat.events.ControllerInputEvent;

@OnlyIn(Dist.CLIENT)
public class KeyboardInput extends ClientInput {
    private final Options options;

    public KeyboardInput(Options pOptions) {
        this.options = pOptions;
    }

    private static float calculateImpulse(boolean pInput, boolean pOtherInput) {
        if (pInput == pOtherInput) {
            return 0.0F;
        } else {
            return pInput ? 1.0F : -1.0F;
        }
    }

    @Override
    public void tick() {

        this.keyPresses = new Input(
            this.options.keyUp.isDown(),
            this.options.keyDown.isDown(),
            this.options.keyLeft.isDown(),
            this.options.keyRight.isDown(),
            this.options.keyJump.isDown(),
            this.options.keyShift.isDown(),
            this.options.keySprint.isDown()
        );

        this.forwardImpulse = calculateImpulse(this.keyPresses.forward(), this.keyPresses.backward());
        this.leftImpulse = calculateImpulse(this.keyPresses.left(), this.keyPresses.right());

        ControllerInputEvent controllerInputEvent = new ControllerInputEvent(forwardImpulse, leftImpulse, keyPresses.jump(), keyPresses.shift(), 0, keyPresses.forward(), keyPresses.backward(), keyPresses.left(), keyPresses.right());
        EventManager.call(controllerInputEvent);

        this.forwardImpulse = controllerInputEvent.forwardImpulse;
        this.leftImpulse = controllerInputEvent.leftImpulse;

        this.keyPresses = new Input(
                controllerInputEvent.up,
                controllerInputEvent.down,
                controllerInputEvent.left,
                controllerInputEvent.right,
                controllerInputEvent.jumping,
                controllerInputEvent.shiftKeyDown,
                this.options.keySprint.isDown()
        );

    }
}