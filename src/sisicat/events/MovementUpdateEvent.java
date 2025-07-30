package sisicat.events;

import com.darkmagician6.eventapi.events.Event;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class MovementUpdateEvent implements Event {

    public enum TYPE {

        PRE,
        POST

    }

    private double
            x,
            y,
            z;

    private final double
            velocity;

    private Vec3
            deltaMovement;

    private float
            yaw,
            pitch;
    private boolean
            onGround;

    private final TYPE
            type;

    public MovementUpdateEvent(double x, double y, double z, double velocity, Vec3 deltaMovement, float yaw, float pitch, boolean onGround, TYPE type){
        this.x = x;
        this.y = y;
        this.z = z;
        this.velocity = velocity;
        this.deltaMovement = deltaMovement;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
        this.type = type;
    }

    public float getYaw(){
        return yaw;
    }

    public float getPitch(){
        return pitch;
    }

    public Vec3 getPosition(){
        return new Vec3(x, y, z);
    }

    public double getVelocity() {
        return velocity;
    }

    public Vec3 getDeltaMovement() {
        return deltaMovement;
    }

    public boolean getOnGround(){
        return onGround;
    }

    public TYPE getType(){
        return type;
    }

    public void setPosition(Vec3 position){
        this.x = position.x;
        this.y = position.y;
        this.z = position.z;
    }

    public void setYaw(float yaw){
        this.yaw = yaw;
    }

    public void setPitch(float pitch){
        this.pitch = pitch;
    }

    public void setOnGround(boolean onGround){
        this.onGround = onGround;
    }

    public void setDeltaMovement(Vec3 deltaMovement) {
        this.deltaMovement = deltaMovement;
    }

}
