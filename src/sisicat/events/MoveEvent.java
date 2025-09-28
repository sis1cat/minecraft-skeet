package sisicat.events;
import com.darkmagician6.eventapi.events.callables.EventCancellable;

public class MoveEvent extends EventCancellable {

    public float yaw;
    public float pitch;
    public float x;
    public float y;
    public float z;

    public MoveEvent(float yaw, float pitch, float x, float y, float z) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.x = x;
        this.y = y;
        this.z = z;
    }


}