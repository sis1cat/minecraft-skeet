package net.minecraft.client.gui.screens;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ConfirmScreen extends Screen {
    private static final int MARGIN = 20;
    private final Component message;
    private MultiLineLabel multilineMessage = MultiLineLabel.EMPTY;
    protected Component yesButton;
    protected Component noButton;
    private int delayTicker;
    protected final BooleanConsumer callback;
    private final List<Button> exitButtons = Lists.newArrayList();

    public ConfirmScreen(BooleanConsumer pCallback, Component pTitle, Component pMessage) {
        this(pCallback, pTitle, pMessage, CommonComponents.GUI_YES, CommonComponents.GUI_NO);
    }

    public ConfirmScreen(BooleanConsumer pCallback, Component pTitle, Component pMessage, Component pYesButton, Component pNoButton) {
        super(pTitle);
        this.callback = pCallback;
        this.message = pMessage;
        this.yesButton = pYesButton;
        this.noButton = pNoButton;
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(super.getNarrationMessage(), this.message);
    }

    @Override
    protected void init() {
        super.init();
        this.multilineMessage = MultiLineLabel.create(this.font, this.message, this.width - 50);
        int i = Mth.clamp(this.messageTop() + this.messageHeight() + 20, this.height / 6 + 96, this.height - 24);
        this.exitButtons.clear();
        this.addButtons(i);
    }

    protected void addButtons(int pY) {
        this.addExitButton(
            Button.builder(this.yesButton, p_169259_ -> this.callback.accept(true)).bounds(this.width / 2 - 155, pY, 150, 20).build()
        );
        this.addExitButton(
            Button.builder(this.noButton, p_169257_ -> this.callback.accept(false)).bounds(this.width / 2 - 155 + 160, pY, 150, 20).build()
        );
    }

    protected void addExitButton(Button pExitButton) {
        this.exitButtons.add(this.addRenderableWidget(pExitButton));
    }

    @Override
    public void render(GuiGraphics p_281588_, int p_283592_, int p_283446_, float p_282443_) {
        super.render(p_281588_, p_283592_, p_283446_, p_282443_);
        p_281588_.drawCenteredString(this.font, this.title, this.width / 2, this.titleTop(), 16777215);
        this.multilineMessage.renderCentered(p_281588_, this.width / 2, this.messageTop());
    }

    private int titleTop() {
        int i = (this.height - this.messageHeight()) / 2;
        return Mth.clamp(i - 20 - 9, 10, 80);
    }

    private int messageTop() {
        return this.titleTop() + 20;
    }

    private int messageHeight() {
        return this.multilineMessage.getLineCount() * 9;
    }

    public void setDelay(int pTicksUntilEnable) {
        this.delayTicker = pTicksUntilEnable;

        for (Button button : this.exitButtons) {
            button.active = false;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (--this.delayTicker == 0) {
            for (Button button : this.exitButtons) {
                button.active = true;
            }
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == 256) {
            this.callback.accept(false);
            return true;
        } else {
            return super.keyPressed(pKeyCode, pScanCode, pModifiers);
        }
    }
}