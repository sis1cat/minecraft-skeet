package sisicat.main.functions.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.world.item.Items;
import sisicat.events.ControllerInputEvent;
import sisicat.events.MovementUpdateEvent;
import sisicat.main.functions.Function;

public class AutomaticElytraJump extends Function {

    public AutomaticElytraJump(String name) {
        super(name);
    }

    @EventTarget
    void _event(MovementUpdateEvent ignored){

        if(mc.player == null || mc.player.getInventory().getItem(38).getItem() != Items.ELYTRA || mc.player.getAbilities().flying)
            return;

        if(mc.player.input.keyPresses.jump())
            mc.player.input.stopJump();

    }

    @EventTarget
    void event(ControllerInputEvent controllerInputEvent) {

        if(mc.player.getInventory().getItem(38).getItem() != Items.ELYTRA || mc.player.getAbilities().flying)
            return;

        controllerInputEvent.jumping = true;

    }

}
