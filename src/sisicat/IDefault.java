package sisicat;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public interface IDefault {

    Minecraft mc = Minecraft.getInstance();

    String name = "minesense";
    String version = "0";

    static void displayClientChatMessage(Object object) {

        if(mc.player == null)
            return;

        mc.player.displayClientMessage(Component.literal("   " + object), false);

    }

}
