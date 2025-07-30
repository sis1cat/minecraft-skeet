package sisicat.main.functions.movement;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import sisicat.events.ControllerInputEvent;
import sisicat.events.MovementUpdateEvent;
import sisicat.events.PacketSendEvent;
import sisicat.events.TickEvent;
import sisicat.main.functions.Function;

public class ClientSideMovementFunction extends Function {

    public ClientSideMovementFunction(String name) {
        super(name);

        this.setBindType(1);
    }

    private boolean
            isPlayerFullStopped = false,
            needToStopMovement = true;
    private Vec3
            stoppedPosition;

    private Vec2
            stoppedRotation;

    @EventTarget
    void _event(ControllerInputEvent controllerInputEvent){

        if(needToStopMovement) {

            controllerInputEvent.forwardImpulse = 0;
            controllerInputEvent.leftImpulse = 0;
            controllerInputEvent.jumping = false;

        }

    }

    @EventTarget
    void _event(MovementUpdateEvent movementUpdateEvent){

        if(
            movementUpdateEvent.getType() == MovementUpdateEvent.TYPE.POST &&
            movementUpdateEvent.getVelocity() == 0 &&
            !isPlayerFullStopped
        ) {
            isPlayerFullStopped = true;
            stoppedPosition = mc.player.position();
        }

    }

    @EventTarget
    void _event(PacketSendEvent packetSendEvent){

        if(isPlayerFullStopped) {

            if (packetSendEvent.getPacket() instanceof ServerboundMovePlayerPacket)
                packetSendEvent.setPacket(new ServerboundMovePlayerPacket.Pos(stoppedPosition.x, stoppedPosition.y, stoppedPosition.z, true, true));

            else if (packetSendEvent.getPacket() instanceof ServerboundPlayerCommandPacket)
                packetSendEvent.cancel();

            if(needToStopMovement)
                stoppedRotation = mc.player.getRotationVector();

            needToStopMovement = false;

        }

    }

    public void onDeactivated(){

        if(needToStopMovement) return;

        mc.player.setPos(stoppedPosition);
        mc.player.setDeltaMovement(0, 0, 0);

        mc.player.setXRot(stoppedRotation.x);
        mc.player.setYRot(stoppedRotation.y);

        EventManager.register(
                new OnDeactivatedActions()
        );

    }

    public void onActivated(){

        stoppedPosition = null;
        stoppedRotation = null;
        needToStopMovement = true;
        isPlayerFullStopped = false;

    }

    private static class OnDeactivatedActions {

        private int tickTimer;

        @EventTarget
        void _event(ControllerInputEvent controllerInputEvent) {

            if(tickTimer <= 2) {
                controllerInputEvent.forwardImpulse = 0;
                controllerInputEvent.leftImpulse = 0;
                controllerInputEvent.jumping = false;
            }

        }

        @EventTarget
        void _event(TickEvent tickEvent){

            tickTimer++;

            if(tickTimer > 2) {

                mc.options.keyUp.setDown(
                        GLFW.glfwGetKey(mc.getWindow().getWindow(), mc.options.keyUp.getDefaultKey().getValue()) == 1
                );
                mc.options.keyDown.setDown(
                        GLFW.glfwGetKey(mc.getWindow().getWindow(), mc.options.keyDown.getDefaultKey().getValue()) == 1
                );

                mc.options.keyLeft.setDown(
                        GLFW.glfwGetKey(mc.getWindow().getWindow(), mc.options.keyLeft.getDefaultKey().getValue()) == 1
                );
                mc.options.keyRight.setDown(
                        GLFW.glfwGetKey(mc.getWindow().getWindow(), mc.options.keyRight.getDefaultKey().getValue()) == 1
                );

                mc.options.keyJump.setDown(
                        GLFW.glfwGetKey(mc.getWindow().getWindow(), mc.options.keyJump.getDefaultKey().getValue()) == 1
                );
                mc.options.keySprint.setDown(
                        GLFW.glfwGetKey(mc.getWindow().getWindow(), mc.options.keySprint.getDefaultKey().getValue()) == 1
                );

                EventManager.unregister(this);

            }

        }

    }

}
