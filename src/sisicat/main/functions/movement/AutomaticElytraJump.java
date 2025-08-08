package sisicat.main.functions.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import sisicat.IDefault;
import sisicat.events.ControllerInputEvent;
import sisicat.events.MovementUpdateEvent;
import sisicat.events.TickEvent;
import sisicat.main.functions.Function;

public class AutomaticElytraJump extends Function {

    public AutomaticElytraJump(String name) {
        super(name);
    }

    @EventTarget
    void _event(TickEvent ignored) {

        ItemStack elytraItemStack = mc.player.getInventory().getItem(38);

        if(elytraItemStack.getItem() != Items.ELYTRA || mc.player.getAbilities().flying || (elytraItemStack.getDamageValue() == elytraItemStack.getMaxDamage() - 1))
            return;

        mc.player.input.stopJump();

    }

    @EventTarget
    void event(ControllerInputEvent controllerInputEvent) {

        ItemStack elytraItemStack = mc.player.getInventory().getItem(38);

        if(elytraItemStack.getItem() != Items.ELYTRA || mc.player.getAbilities().flying || (elytraItemStack.getDamageValue() == elytraItemStack.getMaxDamage() - 1))
            return;

        controllerInputEvent.jumping = true;

    }

}
