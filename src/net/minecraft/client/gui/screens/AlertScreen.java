package net.minecraft.client.gui.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AlertScreen extends Screen {
    private static final int LABEL_Y = 90;
    private final Component messageText;
    private MultiLineLabel message = MultiLineLabel.EMPTY;
    private final Runnable callback;
    private final Component okButton;
    private final boolean shouldCloseOnEsc;

    public AlertScreen(Runnable pCallback, Component pTitle, Component pText) {
        this(pCallback, pTitle, pText, CommonComponents.GUI_BACK, true);
    }

    public AlertScreen(Runnable pCallback, Component pTitle, Component pMessageText, Component pOkButton, boolean pShouldCloseOnEsc) {
        super(pTitle);
        this.callback = pCallback;
        this.messageText = pMessageText;
        this.okButton = pOkButton;
        this.shouldCloseOnEsc = pShouldCloseOnEsc;
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(super.getNarrationMessage(), this.messageText);
    }

    @Override
    protected void init() {
        super.init();
        this.message = MultiLineLabel.create(this.font, this.messageText, this.width - 50);
        int i = this.message.getLineCount() * 9;
        int j = Mth.clamp(90 + i + 12, this.height / 6 + 96, this.height - 24);
        int k = 150;
        this.addRenderableWidget(Button.builder(this.okButton, p_95533_ -> this.callback.run()).bounds((this.width - 150) / 2, j, 150, 20).build());
    }

    @Override
    public void render(GuiGraphics p_281989_, int p_281583_, int p_282152_, float p_282198_) {
        super.render(p_281989_, p_281583_, p_282152_, p_282198_);
        p_281989_.drawCenteredString(this.font, this.title, this.width / 2, 70, 16777215);
        this.message.renderCentered(p_281989_, this.width / 2, 90);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return this.shouldCloseOnEsc;
    }
}