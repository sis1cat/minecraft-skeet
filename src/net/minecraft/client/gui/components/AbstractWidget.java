package net.minecraft.client.gui.components;

import java.time.Duration;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractWidget implements Renderable, GuiEventListener, LayoutElement, NarratableEntry {
    private static final double PERIOD_PER_SCROLLED_PIXEL = 0.5;
    private static final double MIN_SCROLL_PERIOD = 3.0;
    protected int width;
    protected int height;
    private int x;
    private int y;
    private Component message;
    protected boolean isHovered;
    public boolean active = true;
    public boolean visible = true;
    protected float alpha = 1.0F;
    private int tabOrderGroup;
    private boolean focused;
    private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();

    public AbstractWidget(int pX, int pY, int pWidth, int pHeight, Component pMessage) {
        this.x = pX;
        this.y = pY;
        this.width = pWidth;
        this.height = pHeight;
        this.message = pMessage;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public final void render(GuiGraphics p_282421_, int p_93658_, int p_93659_, float p_93660_) {
        if (this.visible) {
            this.isHovered = p_282421_.containsPointInScissor(p_93658_, p_93659_)
                && p_93658_ >= this.getX()
                && p_93659_ >= this.getY()
                && p_93658_ < this.getX() + this.width
                && p_93659_ < this.getY() + this.height;
            this.renderWidget(p_282421_, p_93658_, p_93659_, p_93660_);
            this.tooltip.refreshTooltipForNextRenderPass(this.isHovered(), this.isFocused(), this.getRectangle());
        }
    }

    public void setTooltip(@Nullable Tooltip pTooltip) {
        this.tooltip.set(pTooltip);
    }

    @Nullable
    public Tooltip getTooltip() {
        return this.tooltip.get();
    }

    public void setTooltipDelay(Duration pTooltipDelay) {
        this.tooltip.setDelay(pTooltipDelay);
    }

    protected MutableComponent createNarrationMessage() {
        return wrapDefaultNarrationMessage(this.getMessage());
    }

    public static MutableComponent wrapDefaultNarrationMessage(Component pMessage) {
        return Component.translatable("gui.narrate.button", pMessage);
    }

    protected abstract void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick);

    protected static void renderScrollingString(
        GuiGraphics pGuiGraphics, Font pFont, Component pText, int pMinX, int pMinY, int pMaxX, int pMaxY, int pColor
    ) {
        renderScrollingString(pGuiGraphics, pFont, pText, (pMinX + pMaxX) / 2, pMinX, pMinY, pMaxX, pMaxY, pColor);
    }

    protected static void renderScrollingString(
        GuiGraphics pGuiGraphics, Font pFont, Component pText, int pCenterX, int pMinX, int pMinY, int pMaxX, int pMaxY, int pColor
    ) {
        int i = pFont.width(pText);
        int j = (pMinY + pMaxY - 9) / 2 + 1;
        int k = pMaxX - pMinX;
        if (i > k) {
            int l = i - k;
            double d0 = (double)Util.getMillis() / 1000.0;
            double d1 = Math.max((double)l * 0.5, 3.0);
            double d2 = Math.sin((Math.PI / 2) * Math.cos((Math.PI * 2) * d0 / d1)) / 2.0 + 0.5;
            double d3 = Mth.lerp(d2, 0.0, (double)l);
            pGuiGraphics.enableScissor(pMinX, pMinY, pMaxX, pMaxY);
            pGuiGraphics.drawString(pFont, pText, pMinX - (int)d3, j, pColor);
            pGuiGraphics.disableScissor();
        } else {
            int i1 = Mth.clamp(pCenterX, pMinX + i / 2, pMaxX - i / 2);
            pGuiGraphics.drawCenteredString(pFont, pText, i1, j, pColor);
        }
    }

    protected void renderScrollingString(GuiGraphics pGuiGraphics, Font pFont, int pWidth, int pColor) {
        int i = this.getX() + pWidth;
        int j = this.getX() + this.getWidth() - pWidth;
        renderScrollingString(pGuiGraphics, pFont, this.getMessage(), i, this.getY(), j, this.getY() + this.getHeight(), pColor);
    }

    public void onClick(double pMouseX, double pMouseY) {
    }

    public void onRelease(double pMouseX, double pMouseY) {
    }

    protected void onDrag(double pMouseX, double pMouseY, double pDragX, double pDragY) {
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (this.active && this.visible) {
            if (this.isValidClickButton(pButton)) {
                boolean flag = this.isMouseOver(pMouseX, pMouseY);
                if (flag) {
                    this.playDownSound(Minecraft.getInstance().getSoundManager());
                    this.onClick(pMouseX, pMouseY);
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        if (this.isValidClickButton(pButton)) {
            this.onRelease(pMouseX, pMouseY);
            return true;
        } else {
            return false;
        }
    }

    protected boolean isValidClickButton(int pButton) {
        return pButton == 0;
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        if (this.isValidClickButton(pButton)) {
            this.onDrag(pMouseX, pMouseY, pDragX, pDragY);
            return true;
        } else {
            return false;
        }
    }

    @Nullable
    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent p_265640_) {
        if (!this.active || !this.visible) {
            return null;
        } else {
            return !this.isFocused() ? ComponentPath.leaf(this) : null;
        }
    }

    @Override
    public boolean isMouseOver(double pMouseX, double pMouseY) {
        return this.active
            && this.visible
            && pMouseX >= (double)this.getX()
            && pMouseY >= (double)this.getY()
            && pMouseX < (double)this.getRight()
            && pMouseY < (double)this.getBottom();
    }

    public void playDownSound(SoundManager pHandler) {
        playButtonClickSound(pHandler);
    }

    public static void playButtonClickSound(SoundManager pSoundManager) {
        pSoundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    public void setWidth(int pWidth) {
        this.width = pWidth;
    }

    public void setHeight(int pHeight) {
        this.height = pHeight;
    }

    public void setAlpha(float pAlpha) {
        this.alpha = pAlpha;
    }

    public void setMessage(Component pMessage) {
        this.message = pMessage;
    }

    public Component getMessage() {
        return this.message;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    public boolean isHovered() {
        return this.isHovered;
    }

    public boolean isHoveredOrFocused() {
        return this.isHovered() || this.isFocused();
    }

    @Override
    public boolean isActive() {
        return this.visible && this.active;
    }

    @Override
    public void setFocused(boolean pFocused) {
        this.focused = pFocused;
    }

    @Override
    public NarratableEntry.NarrationPriority narrationPriority() {
        if (this.isFocused()) {
            return NarratableEntry.NarrationPriority.FOCUSED;
        } else {
            return this.isHovered ? NarratableEntry.NarrationPriority.HOVERED : NarratableEntry.NarrationPriority.NONE;
        }
    }

    @Override
    public final void updateNarration(NarrationElementOutput p_259921_) {
        this.updateWidgetNarration(p_259921_);
        this.tooltip.updateNarration(p_259921_);
    }

    protected abstract void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput);

    protected void defaultButtonNarrationText(NarrationElementOutput pNarrationElementOutput) {
        pNarrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
        if (this.active) {
            if (this.isFocused()) {
                pNarrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.focused"));
            } else {
                pNarrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.hovered"));
            }
        }
    }

    @Override
    public int getX() {
        return this.x;
    }

    @Override
    public void setX(int pX) {
        this.x = pX;
    }

    @Override
    public int getY() {
        return this.y;
    }

    @Override
    public void setY(int pY) {
        this.y = pY;
    }

    public int getRight() {
        return this.getX() + this.getWidth();
    }

    public int getBottom() {
        return this.getY() + this.getHeight();
    }

    @Override
    public void visitWidgets(Consumer<AbstractWidget> p_265566_) {
        p_265566_.accept(this);
    }

    public void setSize(int pWidth, int pHeight) {
        this.width = pWidth;
        this.height = pHeight;
    }

    @Override
    public ScreenRectangle getRectangle() {
        return LayoutElement.super.getRectangle();
    }

    public void setRectangle(int pWidth, int pHeight, int pX, int pY) {
        this.setSize(pWidth, pHeight);
        this.setPosition(pX, pY);
    }

    @Override
    public int getTabOrderGroup() {
        return this.tabOrderGroup;
    }

    public void setTabOrderGroup(int pTabOrderGroup) {
        this.tabOrderGroup = pTabOrderGroup;
    }

    protected int yOffset = 0;

    public void setYOffset(int offset) {
        yOffset = offset;
    }

}