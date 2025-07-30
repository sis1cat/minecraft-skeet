package com.mojang.realmsclient.gui.screens;

import java.util.function.Consumer;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsPopups {
    private static final int COLOR_INFO = 8226750;
    private static final Component INFO = Component.translatable("mco.info").withColor(8226750);
    private static final Component WARNING = Component.translatable("mco.warning").withColor(-65536);

    public static PopupScreen infoPopupScreen(Screen pBackgroundScreen, Component pMessage, Consumer<PopupScreen> pOnContinue) {
        return new PopupScreen.Builder(pBackgroundScreen, INFO)
            .setMessage(pMessage)
            .addButton(CommonComponents.GUI_CONTINUE, pOnContinue)
            .addButton(CommonComponents.GUI_CANCEL, PopupScreen::onClose)
            .build();
    }

    public static PopupScreen warningPopupScreen(Screen pBackgroundScreen, Component pMessage, Consumer<PopupScreen> pOnContinue) {
        return new PopupScreen.Builder(pBackgroundScreen, WARNING)
            .setMessage(pMessage)
            .addButton(CommonComponents.GUI_CONTINUE, pOnContinue)
            .addButton(CommonComponents.GUI_CANCEL, PopupScreen::onClose)
            .build();
    }

    public static PopupScreen warningAcknowledgePopupScreen(Screen pBackgroundScreen, Component pMessage, Consumer<PopupScreen> pOnContinue) {
        return new PopupScreen.Builder(pBackgroundScreen, WARNING).setMessage(pMessage).addButton(CommonComponents.GUI_OK, pOnContinue).build();
    }
}