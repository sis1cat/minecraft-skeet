package net.minecraft.client.gui.screens.advancements;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AdvancementTab {
    private final Minecraft minecraft;
    private final AdvancementsScreen screen;
    private final AdvancementTabType type;
    private final int index;
    private final AdvancementNode rootNode;
    private final DisplayInfo display;
    private final ItemStack icon;
    private final Component title;
    private final AdvancementWidget root;
    private final Map<AdvancementHolder, AdvancementWidget> widgets = Maps.newLinkedHashMap();
    private double scrollX;
    private double scrollY;
    private int minX = Integer.MAX_VALUE;
    private int minY = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;
    private int maxY = Integer.MIN_VALUE;
    private float fade;
    private boolean centered;

    public AdvancementTab(
        Minecraft pMinecraft, AdvancementsScreen pScreen, AdvancementTabType pType, int pIndex, AdvancementNode pRootNode, DisplayInfo pDisplay
    ) {
        this.minecraft = pMinecraft;
        this.screen = pScreen;
        this.type = pType;
        this.index = pIndex;
        this.rootNode = pRootNode;
        this.display = pDisplay;
        this.icon = pDisplay.getIcon();
        this.title = pDisplay.getTitle();
        this.root = new AdvancementWidget(this, pMinecraft, pRootNode, pDisplay);
        this.addWidget(this.root, pRootNode.holder());
    }

    public AdvancementTabType getType() {
        return this.type;
    }

    public int getIndex() {
        return this.index;
    }

    public AdvancementNode getRootNode() {
        return this.rootNode;
    }

    public Component getTitle() {
        return this.title;
    }

    public DisplayInfo getDisplay() {
        return this.display;
    }

    public void drawTab(GuiGraphics pGuiGraphics, int pOffsetX, int pOffsetY, boolean pIsSelected) {
        this.type.draw(pGuiGraphics, pOffsetX, pOffsetY, pIsSelected, this.index);
    }

    public void drawIcon(GuiGraphics pGuiGraphics, int pOffsetX, int pOffsetY) {
        this.type.drawIcon(pGuiGraphics, pOffsetX, pOffsetY, this.index, this.icon);
    }

    public void drawContents(GuiGraphics pGuiGraphics, int pX, int pY) {
        if (!this.centered) {
            this.scrollX = (double)(117 - (this.maxX + this.minX) / 2);
            this.scrollY = (double)(56 - (this.maxY + this.minY) / 2);
            this.centered = true;
        }

        pGuiGraphics.enableScissor(pX, pY, pX + 234, pY + 113);
        pGuiGraphics.pose().pushPose();
        pGuiGraphics.pose().translate((float)pX, (float)pY, 0.0F);
        ResourceLocation resourcelocation = this.display.getBackground().orElse(TextureManager.INTENTIONAL_MISSING_TEXTURE);
        int i = Mth.floor(this.scrollX);
        int j = Mth.floor(this.scrollY);
        int k = i % 16;
        int l = j % 16;

        for (int i1 = -1; i1 <= 15; i1++) {
            for (int j1 = -1; j1 <= 8; j1++) {
                pGuiGraphics.blit(RenderType::guiTextured, resourcelocation, k + 16 * i1, l + 16 * j1, 0.0F, 0.0F, 16, 16, 16, 16);
            }
        }

        this.root.drawConnectivity(pGuiGraphics, i, j, true);
        this.root.drawConnectivity(pGuiGraphics, i, j, false);
        this.root.draw(pGuiGraphics, i, j);
        pGuiGraphics.pose().popPose();
        pGuiGraphics.disableScissor();
    }

    public void drawTooltips(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, int pWidth, int pHeight) {
        pGuiGraphics.pose().pushPose();
        pGuiGraphics.pose().translate(0.0F, 0.0F, -200.0F);
        pGuiGraphics.fill(0, 0, 234, 113, Mth.floor(this.fade * 255.0F) << 24);
        boolean flag = false;
        int i = Mth.floor(this.scrollX);
        int j = Mth.floor(this.scrollY);
        if (pMouseX > 0 && pMouseX < 234 && pMouseY > 0 && pMouseY < 113) {
            for (AdvancementWidget advancementwidget : this.widgets.values()) {
                if (advancementwidget.isMouseOver(i, j, pMouseX, pMouseY)) {
                    flag = true;
                    advancementwidget.drawHover(pGuiGraphics, i, j, this.fade, pWidth, pHeight);
                    break;
                }
            }
        }

        pGuiGraphics.pose().popPose();
        if (flag) {
            this.fade = Mth.clamp(this.fade + 0.02F, 0.0F, 0.3F);
        } else {
            this.fade = Mth.clamp(this.fade - 0.04F, 0.0F, 1.0F);
        }
    }

    public boolean isMouseOver(int pOffsetX, int pOffsetY, double pMouseX, double pMouseY) {
        return this.type.isMouseOver(pOffsetX, pOffsetY, this.index, pMouseX, pMouseY);
    }

    @Nullable
    public static AdvancementTab create(Minecraft pMinecraft, AdvancementsScreen pScreen, int pIndex, AdvancementNode pRootNode) {
        Optional<DisplayInfo> optional = pRootNode.advancement().display();
        if (optional.isEmpty()) {
            return null;
        } else {
            for (AdvancementTabType advancementtabtype : AdvancementTabType.values()) {
                if (pIndex < advancementtabtype.getMax()) {
                    return new AdvancementTab(pMinecraft, pScreen, advancementtabtype, pIndex, pRootNode, optional.get());
                }

                pIndex -= advancementtabtype.getMax();
            }

            return null;
        }
    }

    public void scroll(double pDragX, double pDragY) {
        if (this.maxX - this.minX > 234) {
            this.scrollX = Mth.clamp(this.scrollX + pDragX, (double)(-(this.maxX - 234)), 0.0);
        }

        if (this.maxY - this.minY > 113) {
            this.scrollY = Mth.clamp(this.scrollY + pDragY, (double)(-(this.maxY - 113)), 0.0);
        }
    }

    public void addAdvancement(AdvancementNode pNode) {
        Optional<DisplayInfo> optional = pNode.advancement().display();
        if (!optional.isEmpty()) {
            AdvancementWidget advancementwidget = new AdvancementWidget(this, this.minecraft, pNode, optional.get());
            this.addWidget(advancementwidget, pNode.holder());
        }
    }

    private void addWidget(AdvancementWidget pWidget, AdvancementHolder pAdvancement) {
        this.widgets.put(pAdvancement, pWidget);
        int i = pWidget.getX();
        int j = i + 28;
        int k = pWidget.getY();
        int l = k + 27;
        this.minX = Math.min(this.minX, i);
        this.maxX = Math.max(this.maxX, j);
        this.minY = Math.min(this.minY, k);
        this.maxY = Math.max(this.maxY, l);

        for (AdvancementWidget advancementwidget : this.widgets.values()) {
            advancementwidget.attachToParent();
        }
    }

    @Nullable
    public AdvancementWidget getWidget(AdvancementHolder pAdvancement) {
        return this.widgets.get(pAdvancement);
    }

    public AdvancementsScreen getScreen() {
        return this.screen;
    }
}