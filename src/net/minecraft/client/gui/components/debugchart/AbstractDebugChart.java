package net.minecraft.client.gui.components.debugchart;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.debugchart.SampleStorage;

public abstract class AbstractDebugChart {
    protected static final int COLOR_GREY = 14737632;
    protected static final int CHART_HEIGHT = 60;
    protected static final int LINE_WIDTH = 1;
    protected final Font font;
    protected final SampleStorage sampleStorage;

    protected AbstractDebugChart(Font pFont, SampleStorage pSampleStorage) {
        this.font = pFont;
        this.sampleStorage = pSampleStorage;
    }

    public int getWidth(int pMaxWidth) {
        return Math.min(this.sampleStorage.capacity() + 2, pMaxWidth);
    }

    public int getFullHeight() {
        return 69;
    }

    public void drawChart(GuiGraphics pGuiGraphics, int pX, int pWidth) {
        if (this instanceof TpsDebugChart) {
            Minecraft minecraft = Minecraft.getInstance();
            int i = (int)(512.0 / minecraft.getWindow().getGuiScale());
            pX = Math.max(pX, i);
            pWidth = minecraft.getWindow().getGuiScaledWidth() - pX;
        }

        int j2 = pGuiGraphics.guiHeight();
        pGuiGraphics.fill(RenderType.guiOverlay(), pX, j2 - 60, pX + pWidth, j2, -1873784752);
        long k2 = 0L;
        long j = 2147483647L;
        long k = -2147483648L;
        int l = Math.max(0, this.sampleStorage.capacity() - (pWidth - 2));
        int i1 = this.sampleStorage.size() - l;

        for (int j1 = 0; j1 < i1; j1++) {
            int k1 = pX + j1 + 1;
            int l1 = l + j1;
            long i2 = this.getValueForAggregation(l1);
            j = Math.min(j, i2);
            k = Math.max(k, i2);
            k2 += i2;
            this.drawDimensions(pGuiGraphics, j2, k1, l1);
        }

        pGuiGraphics.hLine(RenderType.guiOverlay(), pX, pX + pWidth - 1, j2 - 60, -1);
        pGuiGraphics.hLine(RenderType.guiOverlay(), pX, pX + pWidth - 1, j2 - 1, -1);
        pGuiGraphics.vLine(RenderType.guiOverlay(), pX, j2 - 60, j2, -1);
        pGuiGraphics.vLine(RenderType.guiOverlay(), pX + pWidth - 1, j2 - 60, j2, -1);
        if (i1 > 0) {
            String s = this.toDisplayString((double)j) + " min";
            String s1 = this.toDisplayString((double)k2 / (double)i1) + " avg";
            String s2 = this.toDisplayString((double)k) + " max";
            pGuiGraphics.drawString(this.font, s, pX + 2, j2 - 60 - 9, 14737632);
            pGuiGraphics.drawCenteredString(this.font, s1, pX + pWidth / 2, j2 - 60 - 9, 14737632);
            pGuiGraphics.drawString(this.font, s2, pX + pWidth - this.font.width(s2) - 2, j2 - 60 - 9, 14737632);
        }

        this.renderAdditionalLinesAndLabels(pGuiGraphics, pX, pWidth, j2);
    }

    protected void drawDimensions(GuiGraphics pGuiGraphics, int pHeight, int pX, int pIndex) {
        this.drawMainDimension(pGuiGraphics, pHeight, pX, pIndex);
        this.drawAdditionalDimensions(pGuiGraphics, pHeight, pX, pIndex);
    }

    protected void drawMainDimension(GuiGraphics pGuiGraphics, int pHeight, int pX, int pIndex) {
        long i = this.sampleStorage.get(pIndex);
        int j = this.getSampleHeight((double)i);
        int k = this.getSampleColor(i);
        pGuiGraphics.fill(RenderType.guiOverlay(), pX, pHeight - j, pX + 1, pHeight, k);
    }

    protected void drawAdditionalDimensions(GuiGraphics pGuiGraphics, int pHeight, int pX, int pIndex) {
    }

    protected long getValueForAggregation(int pIndex) {
        return this.sampleStorage.get(pIndex);
    }

    protected void renderAdditionalLinesAndLabels(GuiGraphics pGuiGraphics, int pX, int pWidth, int pHeight) {
    }

    protected void drawStringWithShade(GuiGraphics pGuiGraphics, String pText, int pX, int pY) {
        pGuiGraphics.fill(RenderType.guiOverlay(), pX, pY, pX + this.font.width(pText) + 1, pY + 9, -1873784752);
        pGuiGraphics.drawString(this.font, pText, pX + 1, pY + 1, 14737632, false);
    }

    protected abstract String toDisplayString(double pValue);

    protected abstract int getSampleHeight(double pValue);

    protected abstract int getSampleColor(long pValue);

    protected int getSampleColor(double pValue, double pMinPosition, int pMinColor, double pMidPosition, int pMidColor, double pMaxPosition, int pGuiGraphics) {
        pValue = Mth.clamp(pValue, pMinPosition, pMaxPosition);
        return pValue < pMidPosition
            ? ARGB.lerp((float)((pValue - pMinPosition) / (pMidPosition - pMinPosition)), pMinColor, pMidColor)
            : ARGB.lerp((float)((pValue - pMidPosition) / (pMaxPosition - pMidPosition)), pMidColor, pGuiGraphics);
    }
}