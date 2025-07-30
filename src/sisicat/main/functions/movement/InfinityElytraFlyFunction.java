package sisicat.main.functions.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.util.Mth;
import sisicat.IDefault;
import sisicat.events.MovementCorrectionEvent;
import sisicat.events.MovementUpdateEvent;
import sisicat.events.TickEvent;
import sisicat.main.functions.Function;
import sisicat.main.functions.combat.Rage;

public class InfinityElytraFlyFunction extends Function {

    public InfinityElytraFlyFunction(String name) {
        super(name);
    }

    private float pitch;
    private boolean up = false;
    private float lastFallDistance;

    @EventTarget
    void _event(TickEvent ignored) {

        if (!mc.player.isFallFlying()) {
            pitch = mc.player.getXRot();
            lastFallDistance = 0;
            up = false;
            return;
        }

        if(pitch == 0)
            pitch = mc.player.getXRot();

        if(mc.player.fallDistance > 60) {
            up = true;
            lastFallDistance = 10;
        }
        else if(mc.player.getDeltaMovement().y < 0.2 && mc.player.fallDistance == 1 && lastFallDistance <= 0)
                up = false;



        rotate(pitch, up ? -35 : 40);

        lastFallDistance--;

    }

    @EventTarget
    void _event(MovementUpdateEvent movementUpdateEvent) {

        if(!mc.player.isFallFlying())
            return;

        movementUpdateEvent.setPitch(pitch);

        //mc.player.xHeadRot = pitch;

    }

    @EventTarget
    void _event(MovementCorrectionEvent movementCorrectionEvent) {

        movementCorrectionEvent.setPitchRotation(pitch);

    }

    private void rotate(float from, float to) {

        final float
                pitchDelta = Mth.wrapDegrees(to - from),
                limitedPitchPart = Mth.clamp(pitchDelta, -20f, 20f),
                randomFactorPitch = (float) (Math.random() - 0.5f);

        pitch += Rage.applySensitivityMultiplier(limitedPitchPart + randomFactorPitch);

        pitch = Mth.clamp(pitch, -90f, 90f);

    }

}
