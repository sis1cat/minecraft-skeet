package sisicat.events;

import com.darkmagician6.eventapi.events.Event;

public class ControllerInputEvent implements Event {

    public float forwardImpulse, leftImpulse;
    public boolean jumping, shiftKeyDown;
    public float shiftSlowMultiplier;

    public boolean
            up,
            down,
            left,
            right;

    public ControllerInputEvent(float forwardImpulse, float leftImpulse, boolean jumping, boolean shiftKeyDown, float shiftSlowMultiplier, boolean up, boolean down, boolean left, boolean right)
    {
        this.forwardImpulse = forwardImpulse;
        this.leftImpulse = leftImpulse;
        this.jumping = jumping;
        this.shiftKeyDown = shiftKeyDown;
        this.shiftSlowMultiplier = shiftSlowMultiplier;
        this.up = up;
        this.down = down;
        this.left = left;
        this.right = right;
    }

}
