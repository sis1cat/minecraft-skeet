package net.minecraft.client.gui.components;

import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Button extends AbstractButton {
    public static final int SMALL_WIDTH = 120;
    public static final int DEFAULT_WIDTH = 150;
    public static final int BIG_WIDTH = 200;
    public static final int DEFAULT_HEIGHT = 20;
    public static final int DEFAULT_SPACING = 8;
    protected static final Button.CreateNarration DEFAULT_NARRATION = p_253298_ -> p_253298_.get();
    protected final Button.OnPress onPress;
    protected final Button.CreateNarration createNarration;

    public static Button.Builder builder(Component pMessage, Button.OnPress pOnPress) {
        return new Button.Builder(pMessage, pOnPress);
    }

    protected Button(
        int pX, int pY, int pWidth, int pHeight, Component pMessage, Button.OnPress pOnPress, Button.CreateNarration pCreateNarration
    ) {
        super(pX, pY, pWidth, pHeight, pMessage);
        this.onPress = pOnPress;
        this.createNarration = pCreateNarration;
    }

    @Override
    public void onPress() {
        this.onPress.onPress(this);
    }

    @Override
    protected MutableComponent createNarrationMessage() {
        return this.createNarration.createNarrationMessage(() -> super.createNarrationMessage());
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput p_259196_) {
        this.defaultButtonNarrationText(p_259196_);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Builder {
        private final Component message;
        private final Button.OnPress onPress;
        @Nullable
        private Tooltip tooltip;
        private int x;
        private int y;
        private int width = 150;
        private int height = 20;
        private Button.CreateNarration createNarration = Button.DEFAULT_NARRATION;

        public Builder(Component pMessage, Button.OnPress pOnPress) {
            this.message = pMessage;
            this.onPress = pOnPress;
        }

        public Button.Builder pos(int pX, int pY) {
            this.x = pX;
            this.y = pY;
            return this;
        }

        public Button.Builder width(int pWidth) {
            this.width = pWidth;
            return this;
        }

        public Button.Builder size(int pWidth, int pHeight) {
            this.width = pWidth;
            this.height = pHeight;
            return this;
        }

        public Button.Builder bounds(int pX, int pY, int pWidth, int pHeight) {
            return this.pos(pX, pY).size(pWidth, pHeight);
        }

        public Button.Builder tooltip(@Nullable Tooltip pTooltip) {
            this.tooltip = pTooltip;
            return this;
        }

        public Button.Builder createNarration(Button.CreateNarration pCreateNarration) {
            this.createNarration = pCreateNarration;
            return this;
        }

        public Button build() {
            Button button = new Button(this.x, this.y, this.width, this.height, this.message, this.onPress, this.createNarration);
            button.setTooltip(this.tooltip);
            return button;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface CreateNarration {
        MutableComponent createNarrationMessage(Supplier<MutableComponent> pMessageSupplier);
    }

    @OnlyIn(Dist.CLIENT)
    public interface OnPress {
        void onPress(Button pButton);
    }
}