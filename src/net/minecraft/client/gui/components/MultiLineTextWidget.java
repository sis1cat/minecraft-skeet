package net.minecraft.client.gui.components;

import java.util.OptionalInt;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.SingleKeyCache;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MultiLineTextWidget extends AbstractStringWidget {
    private OptionalInt maxWidth = OptionalInt.empty();
    private OptionalInt maxRows = OptionalInt.empty();
    private final SingleKeyCache<MultiLineTextWidget.CacheKey, MultiLineLabel> cache;
    private boolean centered = false;

    public MultiLineTextWidget(Component pMessage, Font pFont) {
        this(0, 0, pMessage, pFont);
    }

    public MultiLineTextWidget(int pX, int pY, Component pMessage, Font pFont) {
        super(pX, pY, 0, 0, pMessage, pFont);
        this.cache = Util.singleKeyCache(
            p_340776_ -> p_340776_.maxRows.isPresent()
                    ? MultiLineLabel.create(pFont, p_340776_.maxWidth, p_340776_.maxRows.getAsInt(), p_340776_.message)
                    : MultiLineLabel.create(pFont, p_340776_.message, p_340776_.maxWidth)
        );
        this.active = false;
    }

    public MultiLineTextWidget setColor(int p_270378_) {
        super.setColor(p_270378_);
        return this;
    }

    public MultiLineTextWidget setMaxWidth(int pMaxWidth) {
        this.maxWidth = OptionalInt.of(pMaxWidth);
        return this;
    }

    public MultiLineTextWidget setMaxRows(int pMaxRows) {
        this.maxRows = OptionalInt.of(pMaxRows);
        return this;
    }

    public MultiLineTextWidget setCentered(boolean pCentered) {
        this.centered = pCentered;
        return this;
    }

    @Override
    public int getWidth() {
        return this.cache.getValue(this.getFreshCacheKey()).getWidth();
    }

    @Override
    public int getHeight() {
        return this.cache.getValue(this.getFreshCacheKey()).getLineCount() * 9;
    }

    @Override
    public void renderWidget(GuiGraphics p_282535_, int p_261774_, int p_261640_, float p_261514_) {
        MultiLineLabel multilinelabel = this.cache.getValue(this.getFreshCacheKey());
        int i = this.getX();
        int j = this.getY();
        int k = 9;
        int l = this.getColor();
        if (this.centered) {
            multilinelabel.renderCentered(p_282535_, i + this.getWidth() / 2, j, k, l);
        } else {
            multilinelabel.renderLeftAligned(p_282535_, i, j, k, l);
        }
    }

    private MultiLineTextWidget.CacheKey getFreshCacheKey() {
        return new MultiLineTextWidget.CacheKey(this.getMessage(), this.maxWidth.orElse(Integer.MAX_VALUE), this.maxRows);
    }

    @OnlyIn(Dist.CLIENT)
    static record CacheKey(Component message, int maxWidth, OptionalInt maxRows) {
    }
}