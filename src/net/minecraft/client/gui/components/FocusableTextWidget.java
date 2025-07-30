package net.minecraft.client.gui.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FocusableTextWidget extends MultiLineTextWidget {
    private static final int DEFAULT_PADDING = 4;
    private final boolean alwaysShowBorder;
    private final int padding;

    public FocusableTextWidget(int pMaxWidth, Component pMessage, Font pFont) {
        this(pMaxWidth, pMessage, pFont, 4);
    }

    public FocusableTextWidget(int pMaxWidth, Component pMessage, Font pFont, int pPadding) {
        this(pMaxWidth, pMessage, pFont, true, pPadding);
    }

    public FocusableTextWidget(int pMaxWidth, Component pMessage, Font pFont, boolean pAlwaysShowBorder, int pPadding) {
        super(pMessage, pFont);
        this.setMaxWidth(pMaxWidth);
        this.setCentered(true);
        this.active = true;
        this.alwaysShowBorder = pAlwaysShowBorder;
        this.padding = pPadding;
    }

    public void containWithin(int pWidth) {
        this.setMaxWidth(pWidth - this.padding * 4);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput p_300724_) {
        p_300724_.add(NarratedElementType.TITLE, this.getMessage());
    }

    @Override
    public void renderWidget(GuiGraphics p_297672_, int p_301298_, int p_300386_, float p_299545_) {
        if (this.isFocused() || this.alwaysShowBorder) {
            int i = this.getX() - this.padding;
            int j = this.getY() - this.padding;
            int k = this.getWidth() + this.padding * 2;
            int l = this.getHeight() + this.padding * 2;
            int i1 = this.alwaysShowBorder ? (this.isFocused() ? -1 : -6250336) : -1;
            p_297672_.fill(i + 1, j, i + k, j + l, -16777216);
            p_297672_.renderOutline(i, j, k, l, i1);
        }

        super.renderWidget(p_297672_, p_301298_, p_300386_, p_299545_);
    }

    @Override
    public void playDownSound(SoundManager p_297351_) {
    }
}