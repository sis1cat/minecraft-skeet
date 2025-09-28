package sisicat.events;

import com.darkmagician6.eventapi.events.Event;
import net.minecraft.world.phys.Vec3;

public class AttackAnimationEvent implements Event {

    public float amplitude;
    public Vec3 animationXRots;
    public Vec3 staticXRots;
    public Vec3 viewModel;
    public float strength;

    public AttackAnimationEvent(float amplitude, Vec3 animationXRots, Vec3 staticXRots, Vec3 viewModel, float strength) {
        this.amplitude = amplitude;
        this.animationXRots = animationXRots;
        this.staticXRots = staticXRots;
        this.viewModel = viewModel;
        this.strength = strength;
    }

}
