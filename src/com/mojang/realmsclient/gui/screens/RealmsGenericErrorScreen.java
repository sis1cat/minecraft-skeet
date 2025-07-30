package com.mojang.realmsclient.gui.screens;

import com.mojang.realmsclient.client.RealmsError;
import com.mojang.realmsclient.exception.RealmsServiceException;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsGenericErrorScreen extends RealmsScreen {
    private final Screen nextScreen;
    private final RealmsGenericErrorScreen.ErrorMessage lines;
    private MultiLineLabel line2Split = MultiLineLabel.EMPTY;

    public RealmsGenericErrorScreen(RealmsServiceException pServiceException, Screen pNextScreen) {
        super(GameNarrator.NO_TITLE);
        this.nextScreen = pNextScreen;
        this.lines = errorMessage(pServiceException);
    }

    public RealmsGenericErrorScreen(Component pMessage, Screen pNextScreen) {
        super(GameNarrator.NO_TITLE);
        this.nextScreen = pNextScreen;
        this.lines = errorMessage(pMessage);
    }

    public RealmsGenericErrorScreen(Component pTitle, Component pLine2, Screen pMessage) {
        super(GameNarrator.NO_TITLE);
        this.nextScreen = pMessage;
        this.lines = errorMessage(pTitle, pLine2);
    }

    private static RealmsGenericErrorScreen.ErrorMessage errorMessage(RealmsServiceException pException) {
        RealmsError realmserror = pException.realmsError;
        return errorMessage(Component.translatable("mco.errorMessage.realmsService.realmsError", realmserror.errorCode()), realmserror.errorMessage());
    }

    private static RealmsGenericErrorScreen.ErrorMessage errorMessage(Component pMessage) {
        return errorMessage(Component.translatable("mco.errorMessage.generic"), pMessage);
    }

    private static RealmsGenericErrorScreen.ErrorMessage errorMessage(Component pTitle, Component pMessage) {
        return new RealmsGenericErrorScreen.ErrorMessage(pTitle, pMessage);
    }

    @Override
    public void init() {
        this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_OK, p_325126_ -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 52, 200, 20)
                .build()
        );
        this.line2Split = MultiLineLabel.create(this.font, this.lines.detail, this.width * 3 / 4);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.nextScreen);
    }

    @Override
    public Component getNarrationMessage() {
        return Component.empty().append(this.lines.title).append(": ").append(this.lines.detail);
    }

    @Override
    public void render(GuiGraphics p_283497_, int p_88680_, int p_88681_, float p_88682_) {
        super.render(p_283497_, p_88680_, p_88681_, p_88682_);
        p_283497_.drawCenteredString(this.font, this.lines.title, this.width / 2, 80, -1);
        this.line2Split.renderCentered(p_283497_, this.width / 2, 100, 9, -2142128);
    }

    @OnlyIn(Dist.CLIENT)
    static record ErrorMessage(Component title, Component detail) {
    }
}