package net.minecraft.client.gui.screens.advancements;

import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AdvancementsScreen extends Screen implements ClientAdvancements.Listener {
    private static final ResourceLocation WINDOW_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/advancements/window.png");
    public static final int WINDOW_WIDTH = 252;
    public static final int WINDOW_HEIGHT = 140;
    private static final int WINDOW_INSIDE_X = 9;
    private static final int WINDOW_INSIDE_Y = 18;
    public static final int WINDOW_INSIDE_WIDTH = 234;
    public static final int WINDOW_INSIDE_HEIGHT = 113;
    private static final int WINDOW_TITLE_X = 8;
    private static final int WINDOW_TITLE_Y = 6;
    private static final int BACKGROUND_TEXTURE_WIDTH = 256;
    private static final int BACKGROUND_TEXTURE_HEIGHT = 256;
    public static final int BACKGROUND_TILE_WIDTH = 16;
    public static final int BACKGROUND_TILE_HEIGHT = 16;
    public static final int BACKGROUND_TILE_COUNT_X = 14;
    public static final int BACKGROUND_TILE_COUNT_Y = 7;
    private static final double SCROLL_SPEED = 16.0;
    private static final Component VERY_SAD_LABEL = Component.translatable("advancements.sad_label");
    private static final Component NO_ADVANCEMENTS_LABEL = Component.translatable("advancements.empty");
    private static final Component TITLE = Component.translatable("gui.advancements");
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    @Nullable
    private final Screen lastScreen;
    private final ClientAdvancements advancements;
    private final Map<AdvancementHolder, AdvancementTab> tabs = Maps.newLinkedHashMap();
    @Nullable
    private AdvancementTab selectedTab;
    private boolean isScrolling;

    public AdvancementsScreen(ClientAdvancements pAdvancements) {
        this(pAdvancements, null);
    }

    public AdvancementsScreen(ClientAdvancements pAdvancements, @Nullable Screen pLastScreen) {
        super(TITLE);
        this.advancements = pAdvancements;
        this.lastScreen = pLastScreen;
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(TITLE, this.font);
        this.tabs.clear();
        this.selectedTab = null;
        this.advancements.setListener(this);
        if (this.selectedTab == null && !this.tabs.isEmpty()) {
            AdvancementTab advancementtab = this.tabs.values().iterator().next();
            this.advancements.setSelectedTab(advancementtab.getRootNode().holder(), true);
        } else {
            this.advancements.setSelectedTab(this.selectedTab == null ? null : this.selectedTab.getRootNode().holder(), true);
        }

        this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, p_329618_ -> this.onClose()).width(200).build());
        this.layout.visitWidgets(p_335563_ -> {
            AbstractWidget abstractwidget = this.addRenderableWidget(p_335563_);
        });
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    @Override
    public void removed() {
        this.advancements.setListener(null);
        ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
        if (clientpacketlistener != null) {
            clientpacketlistener.send(ServerboundSeenAdvancementsPacket.closedScreen());
        }
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (pButton == 0) {
            int i = (this.width - 252) / 2;
            int j = (this.height - 140) / 2;

            for (AdvancementTab advancementtab : this.tabs.values()) {
                if (advancementtab.isMouseOver(i, j, pMouseX, pMouseY)) {
                    this.advancements.setSelectedTab(advancementtab.getRootNode().holder(), true);
                    break;
                }
            }
        }

        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (this.minecraft.options.keyAdvancements.matches(pKeyCode, pScanCode)) {
            this.minecraft.setScreen(null);
            this.minecraft.mouseHandler.grabMouse();
            return true;
        } else {
            return super.keyPressed(pKeyCode, pScanCode, pModifiers);
        }
    }

    @Override
    public void render(GuiGraphics p_282589_, int p_282255_, int p_283354_, float p_283123_) {
        super.render(p_282589_, p_282255_, p_283354_, p_283123_);
        int i = (this.width - 252) / 2;
        int j = (this.height - 140) / 2;
        this.renderInside(p_282589_, p_282255_, p_283354_, i, j);
        this.renderWindow(p_282589_, i, j);
        this.renderTooltips(p_282589_, p_282255_, p_283354_, i, j);
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        if (pButton != 0) {
            this.isScrolling = false;
            return false;
        } else {
            if (!this.isScrolling) {
                this.isScrolling = true;
            } else if (this.selectedTab != null) {
                this.selectedTab.scroll(pDragX, pDragY);
            }

            return true;
        }
    }

    @Override
    public boolean mouseScrolled(double p_300678_, double p_297858_, double p_301134_, double p_300488_) {
        if (this.selectedTab != null) {
            this.selectedTab.scroll(p_301134_ * 16.0, p_300488_ * 16.0);
            return true;
        } else {
            return false;
        }
    }

    private void renderInside(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, int pOffsetX, int pOffsetY) {
        AdvancementTab advancementtab = this.selectedTab;
        if (advancementtab == null) {
            pGuiGraphics.fill(pOffsetX + 9, pOffsetY + 18, pOffsetX + 9 + 234, pOffsetY + 18 + 113, -16777216);
            int i = pOffsetX + 9 + 117;
            pGuiGraphics.drawCenteredString(this.font, NO_ADVANCEMENTS_LABEL, i, pOffsetY + 18 + 56 - 9 / 2, -1);
            pGuiGraphics.drawCenteredString(this.font, VERY_SAD_LABEL, i, pOffsetY + 18 + 113 - 9, -1);
        } else {
            advancementtab.drawContents(pGuiGraphics, pOffsetX + 9, pOffsetY + 18);
        }
    }

    public void renderWindow(GuiGraphics pGuiGraphics, int pOffsetX, int pOffsetY) {
        pGuiGraphics.blit(RenderType::guiTextured, WINDOW_LOCATION, pOffsetX, pOffsetY, 0.0F, 0.0F, 252, 140, 256, 256);
        if (this.tabs.size() > 1) {
            for (AdvancementTab advancementtab : this.tabs.values()) {
                advancementtab.drawTab(pGuiGraphics, pOffsetX, pOffsetY, advancementtab == this.selectedTab);
            }

            for (AdvancementTab advancementtab1 : this.tabs.values()) {
                advancementtab1.drawIcon(pGuiGraphics, pOffsetX, pOffsetY);
            }
        }

        pGuiGraphics.drawString(this.font, this.selectedTab != null ? this.selectedTab.getTitle() : TITLE, pOffsetX + 8, pOffsetY + 6, 4210752, false);
    }

    private void renderTooltips(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, int pOffsetX, int pOffsetY) {
        if (this.selectedTab != null) {
            pGuiGraphics.pose().pushPose();
            pGuiGraphics.pose().translate((float)(pOffsetX + 9), (float)(pOffsetY + 18), 400.0F);
            this.selectedTab.drawTooltips(pGuiGraphics, pMouseX - pOffsetX - 9, pMouseY - pOffsetY - 18, pOffsetX, pOffsetY);
            pGuiGraphics.pose().popPose();
        }

        if (this.tabs.size() > 1) {
            for (AdvancementTab advancementtab : this.tabs.values()) {
                if (advancementtab.isMouseOver(pOffsetX, pOffsetY, (double)pMouseX, (double)pMouseY)) {
                    pGuiGraphics.renderTooltip(this.font, advancementtab.getTitle(), pMouseX, pMouseY);
                }
            }
        }
    }

    @Override
    public void onAddAdvancementRoot(AdvancementNode p_300702_) {
        AdvancementTab advancementtab = AdvancementTab.create(this.minecraft, this, this.tabs.size(), p_300702_);
        if (advancementtab != null) {
            this.tabs.put(p_300702_.holder(), advancementtab);
        }
    }

    @Override
    public void onRemoveAdvancementRoot(AdvancementNode p_298890_) {
    }

    @Override
    public void onAddAdvancementTask(AdvancementNode p_297934_) {
        AdvancementTab advancementtab = this.getTab(p_297934_);
        if (advancementtab != null) {
            advancementtab.addAdvancement(p_297934_);
        }
    }

    @Override
    public void onRemoveAdvancementTask(AdvancementNode p_301169_) {
    }

    @Override
    public void onUpdateAdvancementProgress(AdvancementNode p_300708_, AdvancementProgress p_97369_) {
        AdvancementWidget advancementwidget = this.getAdvancementWidget(p_300708_);
        if (advancementwidget != null) {
            advancementwidget.setProgress(p_97369_);
        }
    }

    @Override
    public void onSelectedTabChanged(@Nullable AdvancementHolder p_297665_) {
        this.selectedTab = this.tabs.get(p_297665_);
    }

    @Override
    public void onAdvancementsCleared() {
        this.tabs.clear();
        this.selectedTab = null;
    }

    @Nullable
    public AdvancementWidget getAdvancementWidget(AdvancementNode pAdvancement) {
        AdvancementTab advancementtab = this.getTab(pAdvancement);
        return advancementtab == null ? null : advancementtab.getWidget(pAdvancement.holder());
    }

    @Nullable
    private AdvancementTab getTab(AdvancementNode pAdvancement) {
        AdvancementNode advancementnode = pAdvancement.root();
        return this.tabs.get(advancementnode.holder());
    }
}