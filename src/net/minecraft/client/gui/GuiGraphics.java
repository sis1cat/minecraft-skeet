package net.minecraft.client.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Either;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.IForgeGuiGraphics;
import net.minecraftforge.eventbus.api.Event;
import net.optifine.Config;
import net.optifine.CustomColors;
import net.optifine.reflect.Reflector;
import org.joml.Matrix4f;
import org.joml.Vector2ic;

public class GuiGraphics implements IForgeGuiGraphics {
    public static final float MAX_GUI_Z = 10000.0F;
    public static final float MIN_GUI_Z = -10000.0F;
    private static final int EXTRA_SPACE_AFTER_FIRST_TOOLTIP_LINE = 2;
    private final Minecraft minecraft;
    private final PoseStack pose;
    private final MultiBufferSource.BufferSource bufferSource;
    private final GuiGraphics.ScissorStack scissorStack = new GuiGraphics.ScissorStack();
    private final GuiSpriteManager sprites;
    private final ItemStackRenderState scratchItemStackRenderState = new ItemStackRenderState();
    private ItemStack tooltipStack = ItemStack.EMPTY;

    private GuiGraphics(Minecraft pMinecraft, PoseStack pPose, MultiBufferSource.BufferSource pBufferSource) {
        this.minecraft = pMinecraft;
        this.pose = pPose;
        this.bufferSource = pBufferSource;
        this.sprites = pMinecraft.getGuiSprites();
    }

    public GuiGraphics(Minecraft pMinecraft, MultiBufferSource.BufferSource pBufferSource) {
        this(pMinecraft, new PoseStack(), pBufferSource);
    }

    public int guiWidth() {
        return this.minecraft.getWindow().getGuiScaledWidth();
    }

    public int guiHeight() {
        return this.minecraft.getWindow().getGuiScaledHeight();
    }

    public PoseStack pose() {
        return this.pose;
    }

    public void flush() {
        this.bufferSource.endBatch();
    }

    public void hLine(int pMinX, int pMaxX, int pY, int pColor) {
        this.hLine(RenderType.gui(), pMinX, pMaxX, pY, pColor);
    }

    public void hLine(RenderType pRenderType, int pMinX, int pMaxX, int pY, int pColor) {
        if (pMaxX < pMinX) {
            int i = pMinX;
            pMinX = pMaxX;
            pMaxX = i;
        }

        this.fill(pRenderType, pMinX, pY, pMaxX + 1, pY + 1, pColor);
    }

    public void vLine(int pX, int pMinY, int pMaxY, int pColor) {
        this.vLine(RenderType.gui(), pX, pMinY, pMaxY, pColor);
    }

    public void vLine(RenderType pRenderType, int pX, int pMinY, int pMaxY, int pColor) {
        if (pMaxY < pMinY) {
            int i = pMinY;
            pMinY = pMaxY;
            pMaxY = i;
        }

        this.fill(pRenderType, pX, pMinY + 1, pX + 1, pMaxY, pColor);
    }

    public void enableScissor(int pMinX, int pMinY, int pMaxX, int pMaxY) {
        ScreenRectangle screenrectangle = new ScreenRectangle(pMinX, pMinY, pMaxX - pMinX, pMaxY - pMinY)
            .transformAxisAligned(this.pose.last().pose());
        this.applyScissor(this.scissorStack.push(screenrectangle));
    }

    public void disableScissor() {
        this.applyScissor(this.scissorStack.pop());
    }

    public boolean containsPointInScissor(int pX, int pY) {
        return this.scissorStack.containsPoint(pX, pY);
    }

    private void applyScissor(@Nullable ScreenRectangle pRectangle) {
        this.flush();
        if (pRectangle != null) {
            Window window = Minecraft.getInstance().getWindow();
            int i = window.getHeight();
            double d0 = window.getGuiScale();
            double d1 = (double)pRectangle.left() * d0;
            double d2 = (double)i - (double)pRectangle.bottom() * d0;
            double d3 = (double)pRectangle.width() * d0;
            double d4 = (double)pRectangle.height() * d0;
            RenderSystem.enableScissor((int)d1, (int)d2, Math.max(0, (int)d3), Math.max(0, (int)d4));
        } else {
            RenderSystem.disableScissor();
        }
    }

    public void fill(int pMinX, int pMinY, int pMaxX, int pMaxY, int pColor) {
        this.fill(pMinX, pMinY, pMaxX, pMaxY, 0, pColor);
    }

    public void fill(int pMinX, int pMinY, int pMaxX, int pMaxY, int pZ, int pColor) {
        this.fill(RenderType.gui(), pMinX, pMinY, pMaxX, pMaxY, pZ, pColor);
    }

    public void fill(RenderType pRenderType, int pMinX, int pMinY, int pMaxX, int pMaxY, int pColor) {
        this.fill(pRenderType, pMinX, pMinY, pMaxX, pMaxY, 0, pColor);
    }

    public void fill(RenderType pRenderType, int pMinX, int pMinY, int pMaxX, int pMaxY, int pZ, int pColor) {
        Matrix4f matrix4f = this.pose.last().pose();
        if (pMinX < pMaxX) {
            int i = pMinX;
            pMinX = pMaxX;
            pMaxX = i;
        }

        if (pMinY < pMaxY) {
            int j = pMinY;
            pMinY = pMaxY;
            pMaxY = j;
        }

        VertexConsumer vertexconsumer = this.bufferSource.getBuffer(pRenderType);
        vertexconsumer.addVertex(matrix4f, (float)pMinX, (float)pMinY, (float)pZ).setColor(pColor);
        vertexconsumer.addVertex(matrix4f, (float)pMinX, (float)pMaxY, (float)pZ).setColor(pColor);
        vertexconsumer.addVertex(matrix4f, (float)pMaxX, (float)pMaxY, (float)pZ).setColor(pColor);
        vertexconsumer.addVertex(matrix4f, (float)pMaxX, (float)pMinY, (float)pZ).setColor(pColor);
    }

    public void fillGradient(int pX1, int pY1, int pX2, int pY2, int pColorFrom, int pColorTo) {
        this.fillGradient(pX1, pY1, pX2, pY2, 0, pColorFrom, pColorTo);
    }

    public void fillGradient(int pX1, int pY1, int pX2, int pY2, int pZ, int pColorFrom, int pColorTo) {
        this.fillGradient(RenderType.gui(), pX1, pY1, pX2, pY2, pColorFrom, pColorTo, pZ);
    }

    public void fillGradient(RenderType pRenderType, int pX1, int pY1, int pX2, int pY2, int pColorFrom, int pColorTo, int pZ) {
        VertexConsumer vertexconsumer = this.bufferSource.getBuffer(pRenderType);
        this.fillGradient(vertexconsumer, pX1, pY1, pX2, pY2, pZ, pColorFrom, pColorTo);
    }

    private void fillGradient(VertexConsumer pConsumer, int pX1, int pY1, int pX2, int pY2, int pZ, int pColorFrom, int pColorTo) {
        Matrix4f matrix4f = this.pose.last().pose();
        pConsumer.addVertex(matrix4f, (float)pX1, (float)pY1, (float)pZ).setColor(pColorFrom);
        pConsumer.addVertex(matrix4f, (float)pX1, (float)pY2, (float)pZ).setColor(pColorTo);
        pConsumer.addVertex(matrix4f, (float)pX2, (float)pY2, (float)pZ).setColor(pColorTo);
        pConsumer.addVertex(matrix4f, (float)pX2, (float)pY1, (float)pZ).setColor(pColorFrom);
    }

    public void fillRenderType(RenderType pRenderType, int pX1, int pY1, int pX2, int pY2, int pZ) {
        Matrix4f matrix4f = this.pose.last().pose();
        VertexConsumer vertexconsumer = this.bufferSource.getBuffer(pRenderType);
        vertexconsumer.addVertex(matrix4f, (float)pX1, (float)pY1, (float)pZ);
        vertexconsumer.addVertex(matrix4f, (float)pX1, (float)pY2, (float)pZ);
        vertexconsumer.addVertex(matrix4f, (float)pX2, (float)pY2, (float)pZ);
        vertexconsumer.addVertex(matrix4f, (float)pX2, (float)pY1, (float)pZ);
    }

    public void drawCenteredString(Font pFont, String pText, int pX, int pY, int pColor) {
        this.drawString(pFont, pText, pX - pFont.width(pText) / 2, pY, pColor);
    }

    public void drawCenteredString(Font pFont, Component pText, int pX, int pY, int pColor) {
        FormattedCharSequence formattedcharsequence = pText.getVisualOrderText();
        this.drawString(pFont, formattedcharsequence, pX - pFont.width(formattedcharsequence) / 2, pY, pColor);
    }

    public void drawCenteredString(Font pFont, FormattedCharSequence pText, int pX, int pY, int pColor) {
        this.drawString(pFont, pText, pX - pFont.width(pText) / 2, pY, pColor);
    }

    public int drawString(Font pFont, @Nullable String pText, int pX, int pY, int pColor) {
        return this.drawString(pFont, pText, pX, pY, pColor, true);
    }

    public int drawString(Font pFont, @Nullable String pText, int pX, int pY, int pColor, boolean pDropShadow) {
        return this.drawString(pFont, pText, (float)pX, (float)pY, pColor, pDropShadow);
    }

    public int drawString(Font font, @Nullable String text, float x, float y, int color, boolean shadow) {
        return text == null
            ? 0
            : font.drawInBatch(text, x, y, color, shadow, this.pose.last().pose(), this.bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
    }

    public int drawString(Font pFont, FormattedCharSequence pText, int pX, int pY, int pColor) {
        return this.drawString(pFont, pText, pX, pY, pColor, true);
    }

    public int drawString(Font pFont, FormattedCharSequence pText, int pX, int pY, int pColor, boolean pDropShadow) {
        return this.drawString(pFont, pText, (float)pX, (float)pY, pColor, pDropShadow);
    }

    public int drawString(Font font, FormattedCharSequence text, float x, float y, int color, boolean shadow) {
        return font.drawInBatch(text, x, y, color, shadow, this.pose.last().pose(), this.bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
    }

    public int drawString(Font pFont, Component pText, int pX, int pY, int pColor) {
        return this.drawString(pFont, pText, pX, pY, pColor, true);
    }

    public int drawString(Font pFont, Component pText, int pX, int pY, int pColor, boolean pDropShadow) {
        return this.drawString(pFont, pText.getVisualOrderText(), pX, pY, pColor, pDropShadow);
    }

    public void drawWordWrap(Font pFont, FormattedText pText, int pX, int pY, int pLineWidth, int pColor) {
        this.drawWordWrap(pFont, pText, pX, pY, pLineWidth, pColor, true);
    }

    public void drawWordWrap(Font pFont, FormattedText pText, int pX, int pY, int pLineWidth, int pColor, boolean pDropShadow) {
        for (FormattedCharSequence formattedcharsequence : pFont.split(pText, pLineWidth)) {
            this.drawString(pFont, formattedcharsequence, pX, pY, pColor, pDropShadow);
            pY += 9;
        }
    }

    public int drawStringWithBackdrop(Font pFont, Component pText, int pX, int pY, int pXOffset, int pColor) {
        int i = this.minecraft.options.getBackgroundColor(0.0F);
        if (i != 0) {
            int j = 2;
            this.fill(pX - 2, pY - 2, pX + pXOffset + 2, pY + 9 + 2, ARGB.multiply(i, pColor));
        }

        return this.drawString(pFont, pText, pX, pY, pColor, true);
    }

    public void renderOutline(int pX, int pY, int pWidth, int pHeight, int pColor) {
        this.fill(pX, pY, pX + pWidth, pY + 1, pColor);
        this.fill(pX, pY + pHeight - 1, pX + pWidth, pY + pHeight, pColor);
        this.fill(pX, pY + 1, pX + 1, pY + pHeight - 1, pColor);
        this.fill(pX + pWidth - 1, pY + 1, pX + pWidth, pY + pHeight - 1, pColor);
    }

    public void blitSprite(
        Function<ResourceLocation, RenderType> pRenderTypeGetter, ResourceLocation pSprite, int pX, int pY, int pWidth, int pHeight
    ) {
        this.blitSprite(pRenderTypeGetter, pSprite, pX, pY, pWidth, pHeight, -1);
    }

    public void blitSprite(
        Function<ResourceLocation, RenderType> pRenderTypeGetter, ResourceLocation pSprite, int pX, int pY, int pWidth, int pHeight, int pBlitOffset
    ) {
        TextureAtlasSprite textureatlassprite = this.sprites.getSprite(pSprite);
        GuiSpriteScaling guispritescaling = this.sprites.getSpriteScaling(textureatlassprite);
        if (guispritescaling instanceof GuiSpriteScaling.Stretch) {
            this.blitSprite(pRenderTypeGetter, textureatlassprite, pX, pY, pWidth, pHeight, pBlitOffset);
        } else if (guispritescaling instanceof GuiSpriteScaling.Tile guispritescaling$tile) {
            this.blitTiledSprite(
                pRenderTypeGetter,
                textureatlassprite,
                pX,
                pY,
                pWidth,
                pHeight,
                0,
                0,
                guispritescaling$tile.width(),
                guispritescaling$tile.height(),
                guispritescaling$tile.width(),
                guispritescaling$tile.height(),
                pBlitOffset
            );
        } else if (guispritescaling instanceof GuiSpriteScaling.NineSlice guispritescaling$nineslice) {
            this.blitNineSlicedSprite(pRenderTypeGetter, textureatlassprite, guispritescaling$nineslice, pX, pY, pWidth, pHeight, pBlitOffset);
        }
    }

    public void blitSprite(
        Function<ResourceLocation, RenderType> pRenderTypeGetter,
        ResourceLocation pSprite,
        int pTextureWidth,
        int pTextureHeight,
        int pUPosition,
        int pVPosition,
        int pX,
        int pY,
        int pUWidth,
        int pVHeight
    ) {
        TextureAtlasSprite textureatlassprite = this.sprites.getSprite(pSprite);
        GuiSpriteScaling guispritescaling = this.sprites.getSpriteScaling(textureatlassprite);
        if (guispritescaling instanceof GuiSpriteScaling.Stretch) {
            this.blitSprite(pRenderTypeGetter, textureatlassprite, pTextureWidth, pTextureHeight, pUPosition, pVPosition, pX, pY, pUWidth, pVHeight, -1);
        } else {
            this.enableScissor(pX, pY, pX + pUWidth, pY + pVHeight);
            this.blitSprite(pRenderTypeGetter, pSprite, pX - pUPosition, pY - pVPosition, pTextureWidth, pTextureHeight, -1);
            this.disableScissor();
        }
    }

    public void blitSprite(
        Function<ResourceLocation, RenderType> pRenderTypeGetter, TextureAtlasSprite pSprite, int pX, int pY, int pWidth, int pHeight
    ) {
        this.blitSprite(pRenderTypeGetter, pSprite, pX, pY, pWidth, pHeight, -1);
    }

    public void blitSprite(
        Function<ResourceLocation, RenderType> pRenderTypeGetter,
        TextureAtlasSprite pSprite,
        int pX,
        int pY,
        int pWidth,
        int pHeight,
        int pBlitOffset
    ) {
        if (pWidth != 0 && pHeight != 0) {
            this.innerBlit(
                pRenderTypeGetter,
                pSprite.atlasLocation(),
                pX,
                pX + pWidth,
                pY,
                pY + pHeight,
                pSprite.getU0(),
                pSprite.getU1(),
                pSprite.getV0(),
                pSprite.getV1(),
                pBlitOffset
            );
        }
    }

    private void blitSprite(
        Function<ResourceLocation, RenderType> pRenderTypeGetter,
        TextureAtlasSprite pSprite,
        int pTextureWidth,
        int pTextureHeight,
        int pUPosition,
        int pVPosition,
        int pX,
        int pY,
        int pUWidth,
        int pVHeight,
        int pBlitOffset
    ) {
        if (pUWidth != 0 && pVHeight != 0) {
            this.innerBlit(
                pRenderTypeGetter,
                pSprite.atlasLocation(),
                pX,
                pX + pUWidth,
                pY,
                pY + pVHeight,
                pSprite.getU((float)pUPosition / (float)pTextureWidth),
                pSprite.getU((float)(pUPosition + pUWidth) / (float)pTextureWidth),
                pSprite.getV((float)pVPosition / (float)pTextureHeight),
                pSprite.getV((float)(pVPosition + pVHeight) / (float)pTextureHeight),
                pBlitOffset
            );
        }
    }

    private void blitNineSlicedSprite(
        Function<ResourceLocation, RenderType> pRenderTypeGetter,
        TextureAtlasSprite pSprite,
        GuiSpriteScaling.NineSlice pNineSlice,
        int pX,
        int pY,
        int pBlitOffset,
        int pWidth,
        int pHeight
    ) {
        GuiSpriteScaling.NineSlice.Border guispritescaling$nineslice$border = pNineSlice.border();
        int i = Math.min(guispritescaling$nineslice$border.left(), pBlitOffset / 2);
        int j = Math.min(guispritescaling$nineslice$border.right(), pBlitOffset / 2);
        int k = Math.min(guispritescaling$nineslice$border.top(), pWidth / 2);
        int l = Math.min(guispritescaling$nineslice$border.bottom(), pWidth / 2);
        if (pBlitOffset == pNineSlice.width() && pWidth == pNineSlice.height()) {
            this.blitSprite(pRenderTypeGetter, pSprite, pNineSlice.width(), pNineSlice.height(), 0, 0, pX, pY, pBlitOffset, pWidth, pHeight);
        } else if (pWidth == pNineSlice.height()) {
            this.blitSprite(pRenderTypeGetter, pSprite, pNineSlice.width(), pNineSlice.height(), 0, 0, pX, pY, i, pWidth, pHeight);
            this.blitNineSliceInnerSegment(
                pRenderTypeGetter,
                pNineSlice,
                pSprite,
                pX + i,
                pY,
                pBlitOffset - j - i,
                pWidth,
                i,
                0,
                pNineSlice.width() - j - i,
                pNineSlice.height(),
                pNineSlice.width(),
                pNineSlice.height(),
                pHeight
            );
            this.blitSprite(
                pRenderTypeGetter,
                pSprite,
                pNineSlice.width(),
                pNineSlice.height(),
                pNineSlice.width() - j,
                0,
                pX + pBlitOffset - j,
                pY,
                j,
                pWidth,
                pHeight
            );
        } else if (pBlitOffset == pNineSlice.width()) {
            this.blitSprite(pRenderTypeGetter, pSprite, pNineSlice.width(), pNineSlice.height(), 0, 0, pX, pY, pBlitOffset, k, pHeight);
            this.blitNineSliceInnerSegment(
                pRenderTypeGetter,
                pNineSlice,
                pSprite,
                pX,
                pY + k,
                pBlitOffset,
                pWidth - l - k,
                0,
                k,
                pNineSlice.width(),
                pNineSlice.height() - l - k,
                pNineSlice.width(),
                pNineSlice.height(),
                pHeight
            );
            this.blitSprite(
                pRenderTypeGetter,
                pSprite,
                pNineSlice.width(),
                pNineSlice.height(),
                0,
                pNineSlice.height() - l,
                pX,
                pY + pWidth - l,
                pBlitOffset,
                l,
                pHeight
            );
        } else {
            this.blitSprite(pRenderTypeGetter, pSprite, pNineSlice.width(), pNineSlice.height(), 0, 0, pX, pY, i, k, pHeight);
            this.blitNineSliceInnerSegment(
                pRenderTypeGetter,
                pNineSlice,
                pSprite,
                pX + i,
                pY,
                pBlitOffset - j - i,
                k,
                i,
                0,
                pNineSlice.width() - j - i,
                k,
                pNineSlice.width(),
                pNineSlice.height(),
                pHeight
            );
            this.blitSprite(
                pRenderTypeGetter,
                pSprite,
                pNineSlice.width(),
                pNineSlice.height(),
                pNineSlice.width() - j,
                0,
                pX + pBlitOffset - j,
                pY,
                j,
                k,
                pHeight
            );
            this.blitSprite(
                pRenderTypeGetter,
                pSprite,
                pNineSlice.width(),
                pNineSlice.height(),
                0,
                pNineSlice.height() - l,
                pX,
                pY + pWidth - l,
                i,
                l,
                pHeight
            );
            this.blitNineSliceInnerSegment(
                pRenderTypeGetter,
                pNineSlice,
                pSprite,
                pX + i,
                pY + pWidth - l,
                pBlitOffset - j - i,
                l,
                i,
                pNineSlice.height() - l,
                pNineSlice.width() - j - i,
                l,
                pNineSlice.width(),
                pNineSlice.height(),
                pHeight
            );
            this.blitSprite(
                pRenderTypeGetter,
                pSprite,
                pNineSlice.width(),
                pNineSlice.height(),
                pNineSlice.width() - j,
                pNineSlice.height() - l,
                pX + pBlitOffset - j,
                pY + pWidth - l,
                j,
                l,
                pHeight
            );
            this.blitNineSliceInnerSegment(
                pRenderTypeGetter,
                pNineSlice,
                pSprite,
                pX,
                pY + k,
                i,
                pWidth - l - k,
                0,
                k,
                i,
                pNineSlice.height() - l - k,
                pNineSlice.width(),
                pNineSlice.height(),
                pHeight
            );
            this.blitNineSliceInnerSegment(
                pRenderTypeGetter,
                pNineSlice,
                pSprite,
                pX + i,
                pY + k,
                pBlitOffset - j - i,
                pWidth - l - k,
                i,
                k,
                pNineSlice.width() - j - i,
                pNineSlice.height() - l - k,
                pNineSlice.width(),
                pNineSlice.height(),
                pHeight
            );
            this.blitNineSliceInnerSegment(
                pRenderTypeGetter,
                pNineSlice,
                pSprite,
                pX + pBlitOffset - j,
                pY + k,
                j,
                pWidth - l - k,
                pNineSlice.width() - j,
                k,
                j,
                pNineSlice.height() - l - k,
                pNineSlice.width(),
                pNineSlice.height(),
                pHeight
            );
        }
    }

    private void blitNineSliceInnerSegment(
        Function<ResourceLocation, RenderType> pRenderTypeGetter,
        GuiSpriteScaling.NineSlice pNineSlice,
        TextureAtlasSprite pSprite,
        int pX,
        int pY,
        int pWidth,
        int pHeight,
        int pUPosition,
        int pVPosition,
        int pSpriteWidth,
        int pSpriteHeight,
        int pNineSliceWidth,
        int pNineSliceHeight,
        int pBlitOffset
    ) {
        if (pWidth > 0 && pHeight > 0) {
            if (pNineSlice.stretchInner()) {
                this.innerBlit(
                    pRenderTypeGetter,
                    pSprite.atlasLocation(),
                    pX,
                    pX + pWidth,
                    pY,
                    pY + pHeight,
                    pSprite.getU((float)pUPosition / (float)pNineSliceWidth),
                    pSprite.getU((float)(pUPosition + pSpriteWidth) / (float)pNineSliceWidth),
                    pSprite.getV((float)pVPosition / (float)pNineSliceHeight),
                    pSprite.getV((float)(pVPosition + pSpriteHeight) / (float)pNineSliceHeight),
                    pBlitOffset
                );
            } else {
                this.blitTiledSprite(
                    pRenderTypeGetter,
                    pSprite,
                    pX,
                    pY,
                    pWidth,
                    pHeight,
                    pUPosition,
                    pVPosition,
                    pSpriteWidth,
                    pSpriteHeight,
                    pNineSliceWidth,
                    pNineSliceHeight,
                    pBlitOffset
                );
            }
        }
    }

    private void blitTiledSprite(
        Function<ResourceLocation, RenderType> pRenderTypeGetter,
        TextureAtlasSprite pSprite,
        int pX,
        int pY,
        int pWidth,
        int pHeight,
        int pUPosition,
        int pVPosition,
        int pSpriteWidth,
        int pSpriteHeight,
        int pNineSliceWidth,
        int pNineSliceHeight,
        int pBlitOffset
    ) {
        if (pWidth > 0 && pHeight > 0) {
            if (pSpriteWidth <= 0 || pSpriteHeight <= 0) {
                throw new IllegalArgumentException("Tiled sprite texture size must be positive, got " + pSpriteWidth + "x" + pSpriteHeight);
            }

            for (int i = 0; i < pWidth; i += pSpriteWidth) {
                int j = Math.min(pSpriteWidth, pWidth - i);

                for (int k = 0; k < pHeight; k += pSpriteHeight) {
                    int l = Math.min(pSpriteHeight, pHeight - k);
                    this.blitSprite(pRenderTypeGetter, pSprite, pNineSliceWidth, pNineSliceHeight, pUPosition, pVPosition, pX + i, pY + k, j, l, pBlitOffset);
                }
            }
        }
    }

    public void blit(
        Function<ResourceLocation, RenderType> pRenderTypeGetter,
        ResourceLocation pAtlasLocation,
        int pX,
        int pY,
        float pUOffset,
        float pVOffset,
        int pUWidth,
        int pVHeight,
        int pTextureWidth,
        int pTextureHeight,
        int pColor
    ) {
        this.blit(
            pRenderTypeGetter, pAtlasLocation, pX, pY, pUOffset, pVOffset, pUWidth, pVHeight, pUWidth, pVHeight, pTextureWidth, pTextureHeight, pColor
        );
    }

    public void blit(
        Function<ResourceLocation, RenderType> pRenderTypeGetter,
        ResourceLocation pAtlasLocation,
        int pX,
        int pY,
        float pUOffset,
        float pVOffset,
        int pUWidth,
        int pVHeight,
        int pTextureWidth,
        int pTextureHeight
    ) {
        this.blit(pRenderTypeGetter, pAtlasLocation, pX, pY, pUOffset, pVOffset, pUWidth, pVHeight, pUWidth, pVHeight, pTextureWidth, pTextureHeight);
    }

    public void blit(
        Function<ResourceLocation, RenderType> pRenderTypeGetter,
        ResourceLocation pAtlasLocation,
        int pX,
        int pY,
        float pUOffset,
        float pVOffset,
        int pUWidth,
        int pVHeight,
        int pWidth,
        int pHeight,
        int pTextureWidth,
        int pTextureHeight
    ) {
        this.blit(pRenderTypeGetter, pAtlasLocation, pX, pY, pUOffset, pVOffset, pUWidth, pVHeight, pWidth, pHeight, pTextureWidth, pTextureHeight, -1);
    }

    public void blit(
        Function<ResourceLocation, RenderType> pRenderTypeGetter,
        ResourceLocation pAtlasLocation,
        int pX,
        int pY,
        float pUOffset,
        float pVOffset,
        int pUWidth,
        int pVHeight,
        int pWidth,
        int pHeight,
        int pTextureWidth,
        int pTextureHeight,
        int pColor
    ) {
        this.innerBlit(
            pRenderTypeGetter,
            pAtlasLocation,
            pX,
            pX + pUWidth,
            pY,
            pY + pVHeight,
            (pUOffset + 0.0F) / (float)pTextureWidth,
            (pUOffset + (float)pWidth) / (float)pTextureWidth,
            (pVOffset + 0.0F) / (float)pTextureHeight,
            (pVOffset + (float)pHeight) / (float)pTextureHeight,
            pColor
        );
    }

    private void innerBlit(
        Function<ResourceLocation, RenderType> pRenderTypeGetter,
        ResourceLocation pAtlasLocation,
        int pX1,
        int pX2,
        int pY1,
        int pY2,
        float pMinU,
        float pMaxU,
        float pMinV,
        float pMaxV,
        int pColor
    ) {
        RenderType rendertype = pRenderTypeGetter.apply(pAtlasLocation);
        Matrix4f matrix4f = this.pose.last().pose();
        VertexConsumer vertexconsumer = this.bufferSource.getBuffer(rendertype);
        vertexconsumer.addVertex(matrix4f, (float)pX1, (float)pY1, 0.0F).setUv(pMinU, pMinV).setColor(pColor);
        vertexconsumer.addVertex(matrix4f, (float)pX1, (float)pY2, 0.0F).setUv(pMinU, pMaxV).setColor(pColor);
        vertexconsumer.addVertex(matrix4f, (float)pX2, (float)pY2, 0.0F).setUv(pMaxU, pMaxV).setColor(pColor);
        vertexconsumer.addVertex(matrix4f, (float)pX2, (float)pY1, 0.0F).setUv(pMaxU, pMinV).setColor(pColor);
    }

    public void renderItem(ItemStack pStack, int pX, int pY) {
        this.renderItem(this.minecraft.player, this.minecraft.level, pStack, pX, pY, 0);
    }

    public void renderItem(ItemStack pStack, int pX, int pY, int pSeed) {
        this.renderItem(this.minecraft.player, this.minecraft.level, pStack, pX, pY, pSeed);
    }

    public void renderItem(ItemStack pStack, int pX, int pY, int pSeed, int pGuiOffset) {
        this.renderItem(this.minecraft.player, this.minecraft.level, pStack, pX, pY, pSeed, pGuiOffset);
    }

    public void renderFakeItem(ItemStack pStack, int pX, int pY) {
        this.renderFakeItem(pStack, pX, pY, 0);
    }

    public void renderFakeItem(ItemStack pStack, int pX, int pY, int pSeed) {
        this.renderItem(null, this.minecraft.level, pStack, pX, pY, pSeed);
    }

    public void renderItem(LivingEntity pEntity, ItemStack pStack, int pX, int pY, int pSeed) {
        this.renderItem(pEntity, pEntity.level(), pStack, pX, pY, pSeed);
    }

    private void renderItem(@Nullable LivingEntity pEntity, @Nullable Level pLevel, ItemStack pStack, int pX, int pY, int pSeed) {
        this.renderItem(pEntity, pLevel, pStack, pX, pY, pSeed, 0);
    }

    private void renderItem(
        @Nullable LivingEntity pEntity, @Nullable Level pLevel, ItemStack pStack, int pX, int pY, int pSeed, int pGuiOffset
    ) {
        ItemRenderer.setRenderItemGui(true);
        if (!pStack.isEmpty()) {
            this.minecraft.getItemModelResolver().updateForTopItem(this.scratchItemStackRenderState, pStack, ItemDisplayContext.GUI, false, pLevel, pEntity, pSeed);
            this.pose.pushPose();
            this.pose.translate((float)(pX + 8), (float)(pY + 8), (float)(150 + (this.scratchItemStackRenderState.isGui3d() ? pGuiOffset : 0)));

            try {
                this.pose.scale(16.0F, -16.0F, 16.0F);
                boolean flag = !this.scratchItemStackRenderState.usesBlockLight();
                if (flag) {
                    this.flush();
                    Lighting.setupForFlatItems();
                }

                this.scratchItemStackRenderState.render(this.pose, this.bufferSource, 15728880, OverlayTexture.NO_OVERLAY);
                this.flush();
                if (flag) {
                    Lighting.setupFor3DItems();
                }
            } catch (Throwable throwable1) {
                CrashReport crashreport = CrashReport.forThrowable(throwable1, "Rendering item");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Item being rendered");
                crashreportcategory.setDetail("Item Type", () -> String.valueOf(pStack.getItem()));
                crashreportcategory.setDetail("Item Components", () -> String.valueOf(pStack.getComponents()));
                crashreportcategory.setDetail("Item Foil", () -> String.valueOf(pStack.hasFoil()));
                throw new ReportedException(crashreport);
            }

            this.pose.popPose();
        }

        ItemRenderer.setRenderItemGui(false);
    }

    public void renderItemDecorations(Font pFont, ItemStack pStack, int pX, int pY) {
        this.renderItemDecorations(pFont, pStack, pX, pY, null);
    }

    public void renderItemDecorations(Font pFont, ItemStack pStack, int pX, int pY, @Nullable String pText) {
        if (!pStack.isEmpty()) {
            this.pose.pushPose();
            this.renderItemBar(pStack, pX, pY);
            this.renderItemCount(pFont, pStack, pX, pY, pText);
            this.renderItemCooldown(pStack, pX, pY);
            this.pose.popPose();
            if (Reflector.ItemDecoratorHandler_render.exists()) {
                Object object = Reflector.call(Reflector.ItemDecoratorHandler_of, pStack);
                Reflector.call(object, Reflector.ItemDecoratorHandler_render, this, pFont, pStack, pX, pY);
            }
        }
    }

    public void renderTooltip(Font pFont, ItemStack pStack, int pMouseX, int pMouseY) {
        this.tooltipStack = pStack;
        this.renderTooltip(
            pFont, Screen.getTooltipFromItem(this.minecraft, pStack), pStack.getTooltipImage(), pMouseX, pMouseY, pStack.get(DataComponents.TOOLTIP_STYLE)
        );
        this.tooltipStack = ItemStack.EMPTY;
    }

    public void renderTooltip(Font font, List<Component> textComponents, Optional<TooltipComponent> tooltipComponent, ItemStack stack, int mouseX, int mouseY) {
        this.tooltipStack = stack;
        this.renderTooltip(font, textComponents, tooltipComponent, mouseX, mouseY, stack.get(DataComponents.TOOLTIP_STYLE));
        this.tooltipStack = ItemStack.EMPTY;
    }

    public void renderTooltip(Font pFont, List<Component> pTooltipLines, Optional<TooltipComponent> pVisualTooltipComponent, int pMouseX, int pMouseY) {
        this.renderTooltip(pFont, pTooltipLines, pVisualTooltipComponent, pMouseX, pMouseY, null);
    }

    public void renderTooltip(
        Font pFont, List<Component> pTooltipLines, Optional<TooltipComponent> pVisualTooltipComponent, int pMouseX, int pMouseY, @Nullable ResourceLocation pSprite
    ) {
        List<ClientTooltipComponent> list = pTooltipLines.stream().map(Component::getVisualOrderText).map(ClientTooltipComponent::create).collect(Util.toMutableList());
        pVisualTooltipComponent.ifPresent(compIn -> list.add(list.isEmpty() ? 0 : 1, ClientTooltipComponent.create(compIn)));
        if (Reflector.ForgeHooksClient_gatherTooltipComponents7.exists()) {
            List list1 = (List)Reflector.ForgeHooksClient_gatherTooltipComponents7
                .call(this.tooltipStack, pTooltipLines, pVisualTooltipComponent, pMouseX, this.guiWidth(), this.guiHeight(), pFont);
            list.clear();
            list.addAll(list1);
        }

        this.renderTooltipInternal(pFont, list, pMouseX, pMouseY, DefaultTooltipPositioner.INSTANCE, pSprite);
    }

    public void renderTooltip(Font pFont, Component pText, int pMouseX, int pMouseY) {
        this.renderTooltip(pFont, pText, pMouseX, pMouseY, null);
    }

    public void renderTooltip(Font pFont, Component pText, int pMouseX, int pMouseY, @Nullable ResourceLocation pSprite) {
        this.renderTooltip(pFont, List.of(pText.getVisualOrderText()), pMouseX, pMouseY, pSprite);
    }

    public void renderComponentTooltip(Font pFont, List<Component> pTooltipLines, int pMouseX, int pMouseY) {
        if (Reflector.ForgeHooksClient_gatherTooltipComponents6.exists()) {
            List<ClientTooltipComponent> list = (List<ClientTooltipComponent>)Reflector.ForgeHooksClient_gatherTooltipComponents6
                .call(this.tooltipStack, pTooltipLines, pMouseX, this.guiWidth(), this.guiHeight(), pFont);
            this.renderTooltipInternal(pFont, list, pMouseX, pMouseY, DefaultTooltipPositioner.INSTANCE, null);
        } else {
            this.renderComponentTooltip(pFont, pTooltipLines, pMouseX, pMouseY, (ResourceLocation)null);
        }
    }

    public void renderComponentTooltip(Font pFont, List<Component> pTooltipLines, int pMouseX, int pMouseY, @Nullable ResourceLocation pSprite) {
        this.renderTooltipInternal(
            pFont,
            pTooltipLines.stream().map(Component::getVisualOrderText).map(ClientTooltipComponent::create).toList(),
            pMouseX,
            pMouseY,
            DefaultTooltipPositioner.INSTANCE,
            pSprite
        );
    }

    public void renderComponentTooltip(Font font, List<? extends FormattedText> tooltips, int mouseX, int mouseY, ItemStack stack) {
        this.tooltipStack = stack;
        List<ClientTooltipComponent> list = (List<ClientTooltipComponent>)Reflector.ForgeHooksClient_gatherTooltipComponents6
            .call(stack, tooltips, mouseX, this.guiWidth(), this.guiHeight(), font);
        this.renderTooltipInternal(font, list, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
        this.tooltipStack = ItemStack.EMPTY;
    }

    public void renderComponentTooltipFromElements(Font font, List<Either<FormattedText, TooltipComponent>> elements, int mouseX, int mouseY, ItemStack stack) {
        this.tooltipStack = stack;
        List<ClientTooltipComponent> list = (List<ClientTooltipComponent>)Reflector.ForgeHooksClient_gatherTooltipComponentsFromElements
            .call(stack, elements, mouseX, this.guiWidth(), this.guiHeight(), font);
        this.renderTooltipInternal(font, list, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
        this.tooltipStack = ItemStack.EMPTY;
    }

    public void renderTooltip(Font pFont, List<? extends FormattedCharSequence> pTooltipLines, int pMouseX, int pMouseY) {
        this.renderTooltip(pFont, pTooltipLines, pMouseX, pMouseY, null);
    }

    public void renderTooltip(Font pFont, List<? extends FormattedCharSequence> pTooltipLines, int pMouseX, int pMouseY, @Nullable ResourceLocation pSprite) {
        this.renderTooltipInternal(
            pFont,
            pTooltipLines.stream().map(ClientTooltipComponent::create).collect(Collectors.toList()),
            pMouseX,
            pMouseY,
            DefaultTooltipPositioner.INSTANCE,
            pSprite
        );
    }

    public void renderTooltip(Font pFont, List<FormattedCharSequence> pTooltipLines, ClientTooltipPositioner pTooltipPositioner, int pMouseX, int pMouseY) {
        this.renderTooltipInternal(pFont, pTooltipLines.stream().map(ClientTooltipComponent::create).collect(Collectors.toList()), pMouseX, pMouseY, pTooltipPositioner, null);
    }

    private void renderTooltipInternal(
        Font pFont,
        List<ClientTooltipComponent> pTooltipLines,
        int pMouseX,
        int pMouseY,
        ClientTooltipPositioner pTooltipPositioner,
        @Nullable ResourceLocation pSprite
    ) {
        if (!pTooltipLines.isEmpty()) {
            Event event = null;
            if (Reflector.ForgeHooksClient_onRenderTooltipPre.exists()) {
                event = (Event)Reflector.ForgeHooksClient_onRenderTooltipPre
                    .call(this.tooltipStack, this, pMouseX, pMouseY, this.guiWidth(), this.guiHeight(), pTooltipLines, pFont, pTooltipPositioner);
                if (event.isCanceled()) {
                    return;
                }
            }

            int i = 0;
            int j = pTooltipLines.size() == 1 ? -2 : 0;

            for (ClientTooltipComponent clienttooltipcomponent : pTooltipLines) {
                if (event != null) {
                    pFont = (Font)Reflector.call(event, Reflector.RenderTooltipEvent_getFont);
                }

                int k = clienttooltipcomponent.getWidth(pFont);
                if (k > i) {
                    i = k;
                }

                j += clienttooltipcomponent.getHeight(pFont);
            }

            int l1 = i;
            int i2 = j;
            if (event != null) {
                pMouseX = Reflector.callInt(event, Reflector.RenderTooltipEvent_getX);
                pMouseY = Reflector.callInt(event, Reflector.RenderTooltipEvent_getY);
            }

            Vector2ic vector2ic = pTooltipPositioner.positionTooltip(this.guiWidth(), this.guiHeight(), pMouseX, pMouseY, i, j);
            int l = vector2ic.x();
            int i1 = vector2ic.y();
            this.pose.pushPose();
            int j1 = 400;
            if (Reflector.ForgeEventFactoryClient_onRenderTooltipBackground.exists()) {
                Object object = Reflector.ForgeEventFactoryClient_onRenderTooltipBackground
                    .call(this.tooltipStack, this, l, i1, pFont, pTooltipLines, pSprite);
                pSprite = (ResourceLocation)Reflector.call(object, Reflector.RenderTooltipEvent_Background_getBackground);
            }

            TooltipRenderUtil.renderTooltipBackground(this, l, i1, i, j, 400, pSprite);
            this.pose.translate(0.0F, 0.0F, 400.0F);
            int j2 = i1;

            for (int k1 = 0; k1 < pTooltipLines.size(); k1++) {
                ClientTooltipComponent clienttooltipcomponent1 = pTooltipLines.get(k1);
                clienttooltipcomponent1.renderText(pFont, l, j2, this.pose.last().pose(), this.bufferSource);
                j2 += clienttooltipcomponent1.getHeight(pFont) + (k1 == 0 ? 2 : 0);
            }

            j2 = i1;

            for (int k2 = 0; k2 < pTooltipLines.size(); k2++) {
                ClientTooltipComponent clienttooltipcomponent2 = pTooltipLines.get(k2);
                clienttooltipcomponent2.renderImage(pFont, l, j2, l1, i2, this);
                j2 += clienttooltipcomponent2.getHeight(pFont) + (k2 == 0 ? 2 : 0);
            }

            this.pose.popPose();
        }
    }

    private void renderItemBar(ItemStack pStack, int pX, int pY) {
        if (pStack.isBarVisible()) {
            int i = pX + 2;
            int j = pY + 13;
            this.fill(RenderType.gui(), i, j, i + 13, j + 2, 200, -16777216);
            int k = pStack.getBarColor();
            if (Config.isCustomColors()) {
                float f = (float)pStack.getDamageValue();
                float f1 = (float)pStack.getMaxDamage();
                float f2 = Math.max(0.0F, (f1 - f) / f1);
                k = CustomColors.getDurabilityColor(f2, k);
            }

            this.fill(RenderType.gui(), i, j, i + pStack.getBarWidth(), j + 1, 200, ARGB.opaque(k));
        }
    }

    private void renderItemCount(Font pFont, ItemStack pStack, int pX, int pY, @Nullable String pText) {
        if (pStack.getCount() != 1 || pText != null) {
            String s = pText == null ? String.valueOf(pStack.getCount()) : pText;
            this.pose.pushPose();
            this.pose.translate(0.0F, 0.0F, 200.0F);
            this.drawString(pFont, s, pX + 19 - 2 - pFont.width(s), pY + 6 + 3, -1, true);
            this.pose.popPose();
        }
    }

    private void renderItemCooldown(ItemStack pStack, int pX, int pY) {
        LocalPlayer localplayer = this.minecraft.player;
        float f = localplayer == null ? 0.0F : localplayer.getCooldowns().getCooldownPercent(pStack, this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true));
        if (f > 0.0F) {
            int i = pY + Mth.floor(16.0F * (1.0F - f));
            int j = i + Mth.ceil(16.0F * f);
            this.fill(RenderType.gui(), pX, i, pX + 16, j, 200, Integer.MAX_VALUE);
        }
    }

    public void renderComponentHoverEffect(Font pFont, @Nullable Style pStyle, int pMouseX, int pMouseY) {
        if (pStyle != null && pStyle.getHoverEvent() != null) {
            HoverEvent hoverevent = pStyle.getHoverEvent();
            HoverEvent.ItemStackInfo hoverevent$itemstackinfo = hoverevent.getValue(HoverEvent.Action.SHOW_ITEM);
            if (hoverevent$itemstackinfo != null) {
                this.renderTooltip(pFont, hoverevent$itemstackinfo.getItemStack(), pMouseX, pMouseY);
            } else {
                HoverEvent.EntityTooltipInfo hoverevent$entitytooltipinfo = hoverevent.getValue(HoverEvent.Action.SHOW_ENTITY);
                if (hoverevent$entitytooltipinfo != null) {
                    if (this.minecraft.options.advancedItemTooltips) {
                        this.renderComponentTooltip(pFont, hoverevent$entitytooltipinfo.getTooltipLines(), pMouseX, pMouseY);
                    }
                } else {
                    Component component = hoverevent.getValue(HoverEvent.Action.SHOW_TEXT);
                    if (component != null) {
                        this.renderTooltip(pFont, pFont.split(component, Math.max(this.guiWidth() / 2, 200)), pMouseX, pMouseY);
                    }
                }
            }
        }
    }

    public void drawSpecial(Consumer<MultiBufferSource> pDrawer) {
        pDrawer.accept(this.bufferSource);
        this.bufferSource.endBatch();
    }

    public void getBulkData(RenderType renderType, ByteBuffer buffer) {
        if (renderType != null) {
            VertexConsumer vertexconsumer = this.bufferSource.getBuffer(renderType);
            if (vertexconsumer.getVertexCount() > 0) {
                vertexconsumer.getBulkData(buffer);
            }
        }
    }

    public void putBulkData(RenderType renderType, ByteBuffer buffer) {
        if (renderType != null) {
            if (buffer.position() < buffer.limit()) {
                VertexConsumer vertexconsumer = this.bufferSource.getBuffer(renderType);
                vertexconsumer.putBulkData(buffer);
            }
        }
    }

    public MultiBufferSource.BufferSource getBufferSource() {
        return this.bufferSource;
    }

    static class ScissorStack {
        private final Deque<ScreenRectangle> stack = new ArrayDeque<>();

        public ScreenRectangle push(ScreenRectangle pScissor) {
            ScreenRectangle screenrectangle = this.stack.peekLast();
            if (screenrectangle != null) {
                ScreenRectangle screenrectangle1 = Objects.requireNonNullElse(pScissor.intersection(screenrectangle), ScreenRectangle.empty());
                this.stack.addLast(screenrectangle1);
                return screenrectangle1;
            } else {
                this.stack.addLast(pScissor);
                return pScissor;
            }
        }

        @Nullable
        public ScreenRectangle pop() {
            if (this.stack.isEmpty()) {
                throw new IllegalStateException("Scissor stack underflow");
            } else {
                this.stack.removeLast();
                return this.stack.peekLast();
            }
        }

        public boolean containsPoint(int pX, int pY) {
            return this.stack.isEmpty() ? true : this.stack.peek().containsPoint(pX, pY);
        }
    }
}