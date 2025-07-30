package sisicat.events;

import com.darkmagician6.eventapi.events.Event;

public class MovementCorrectionEvent implements Event {

    private float
            yawRotation,
            pitchRotation;

    public MovementCorrectionEvent(float yawRotation, float pitchRotation) {
        this.yawRotation = yawRotation;
        this.pitchRotation = pitchRotation;
    }

    public float getYawRotation() {
        return yawRotation;
    }

    public float getPitchRotation() {
        return pitchRotation;
    }

    public void setYawRotation(float yawRotation) {
        this.yawRotation = yawRotation;
    }

    public void setPitchRotation(float pitchRotation) {
        this.pitchRotation = pitchRotation;
    }

}
