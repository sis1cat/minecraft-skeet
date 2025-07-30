package net.minecraft.client.gui.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FittingMultiLineTextWidget extends AbstractTextAreaWidget {
    private final Font font;
    private final MultiLineTextWidget multilineWidget;

    public FittingMultiLineTextWidget(int pX, int pY, int pWidth, int pHeight, Component pMessage, Font pFont) {
        super(pX, pY, pWidth, pHeight, pMessage);
        this.font = pFont;
        this.multilineWidget = new MultiLineTextWidget(pMessage, pFont).setMaxWidth(this.getWidth() - this.totalInnerPadding());
    }

    public FittingMultiLineTextWidget setColor(int pColor) {
        this.multilineWidget.setColor(pColor);
        return this;
    }

    @Override
    public void setWidth(int p_289765_) {
        super.setWidth(p_289765_);
        this.multilineWidget.setMaxWidth(this.getWidth() - this.totalInnerPadding());
    }

    @Override
    protected int getInnerHeight() {
        return this.multilineWidget.getHeight();
    }

    @Override
    protected double scrollRate() {
        return 9.0;
    }

    @Override
    protected void renderBackground(GuiGraphics p_289758_) {
        if (this.scrollbarVisible()) {
            super.renderBackground(p_289758_);
        } else if (this.isFocused()) {
            this.renderBorder(
                p_289758_,
                this.getX() - this.innerPadding(),
                this.getY() - this.innerPadding(),
                this.getWidth() + this.totalInnerPadding(),
                this.getHeight() + this.totalInnerPadding()
            );
        }
    }

    @Override
    public void renderWidget(GuiGraphics p_289802_, int p_289778_, int p_289798_, float p_289804_) {
        if (this.visible) {
            if (!this.scrollbarVisible()) {
                this.renderBackground(p_289802_);
                p_289802_.pose().pushPose();
                p_289802_.pose().translate((float)this.getX(), (float)this.getY(), 0.0F);
                this.multilineWidget.render(p_289802_, p_289778_, p_289798_, p_289804_);
                p_289802_.pose().popPose();
            } else {
                super.renderWidget(p_289802_, p_289778_, p_289798_, p_289804_);
            }
        }
    }

    public boolean showingScrollBar() {
        return super.scrollbarVisible();
    }

    @Override
    protected void renderContents(GuiGraphics p_289766_, int p_289790_, int p_289786_, float p_289767_) {
        p_289766_.pose().pushPose();
        p_289766_.pose().translate((float)this.getInnerLeft(), (float)this.getInnerTop(), 0.0F);
        this.multilineWidget.render(p_289766_, p_289790_, p_289786_, p_289767_);
        p_289766_.pose().popPose();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput p_289784_) {
        p_289784_.add(NarratedElementType.TITLE, this.getMessage());
    }
}