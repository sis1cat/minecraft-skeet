package sisicat.main.utilities;

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import sisicat.IDefault;
import sisicat.events.EventWindowClick;

public class Inventory implements IDefault {

    public static int getHotbarAxeSlot() {

        for (int i = 0; i < 9; i++)
            if (mc.player.getInventory().getItem(i).getItem() instanceof AxeItem)
                return i;

        return -1;

    }


}
