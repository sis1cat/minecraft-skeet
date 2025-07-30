package net.minecraft.client.gui.screens.packs;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TransferableSelectionList extends ObjectSelectionList<TransferableSelectionList.PackEntry> {
    static final ResourceLocation SELECT_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/select_highlighted");
    static final ResourceLocation SELECT_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/select");
    static final ResourceLocation UNSELECT_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/unselect_highlighted");
    static final ResourceLocation UNSELECT_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/unselect");
    static final ResourceLocation MOVE_UP_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/move_up_highlighted");
    static final ResourceLocation MOVE_UP_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/move_up");
    static final ResourceLocation MOVE_DOWN_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/move_down_highlighted");
    static final ResourceLocation MOVE_DOWN_SPRITE = ResourceLocation.withDefaultNamespace("transferable_list/move_down");
    static final Component INCOMPATIBLE_TITLE = Component.translatable("pack.incompatible");
    static final Component INCOMPATIBLE_CONFIRM_TITLE = Component.translatable("pack.incompatible.confirm.title");
    private final Component title;
    final PackSelectionScreen screen;

    public TransferableSelectionList(Minecraft pMinecraft, PackSelectionScreen pScreen, int pWidth, int pHeight, Component pTitle) {
        super(pMinecraft, pWidth, pHeight, 33, 36, (int)(9.0F * 1.5F));
        this.screen = pScreen;
        this.title = pTitle;
        this.centerListVertically = false;
    }

    @Override
    protected void renderHeader(GuiGraphics p_282135_, int p_282032_, int p_283198_) {
        Component component = Component.empty().append(this.title).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD);
        p_282135_.drawString(
            this.minecraft.font,
            component,
            p_282032_ + this.width / 2 - this.minecraft.font.width(component) / 2,
            Math.min(this.getY() + 3, p_283198_),
            -1
        );
    }

    @Override
    public int getRowWidth() {
        return this.width;
    }

    @Override
    protected int scrollBarX() {
        return this.getRight() - 6;
    }

    @Override
    protected void renderSelection(GuiGraphics p_331157_, int p_334621_, int p_330518_, int p_327737_, int p_334843_, int p_336171_) {
        if (this.scrollbarVisible()) {
            int i = 2;
            int j = this.getRowLeft() - 2;
            int k = this.getRight() - 6 - 1;
            int l = p_334621_ - 2;
            int i1 = p_334621_ + p_327737_ + 2;
            p_331157_.fill(j, l, k, i1, p_334843_);
            p_331157_.fill(j + 1, l + 1, k - 1, i1 - 1, p_336171_);
        } else {
            super.renderSelection(p_331157_, p_334621_, p_330518_, p_327737_, p_334843_, p_336171_);
        }
    }

    @Override
    public boolean keyPressed(int p_265499_, int p_265510_, int p_265548_) {
        if (this.getSelected() != null) {
            switch (p_265499_) {
                case 32:
                case 257:
                    this.getSelected().keyboardSelection();
                    return true;
                default:
                    if (Screen.hasShiftDown()) {
                        switch (p_265499_) {
                            case 264:
                                this.getSelected().keyboardMoveDown();
                                return true;
                            case 265:
                                this.getSelected().keyboardMoveUp();
                                return true;
                        }
                    }
            }
        }

        return super.keyPressed(p_265499_, p_265510_, p_265548_);
    }

    @OnlyIn(Dist.CLIENT)
    public static class PackEntry extends ObjectSelectionList.Entry<TransferableSelectionList.PackEntry> {
        private static final int MAX_DESCRIPTION_WIDTH_PIXELS = 157;
        private static final int MAX_NAME_WIDTH_PIXELS = 157;
        private static final String TOO_LONG_NAME_SUFFIX = "...";
        private final TransferableSelectionList parent;
        protected final Minecraft minecraft;
        private final PackSelectionModel.Entry pack;
        private final FormattedCharSequence nameDisplayCache;
        private final MultiLineLabel descriptionDisplayCache;
        private final FormattedCharSequence incompatibleNameDisplayCache;
        private final MultiLineLabel incompatibleDescriptionDisplayCache;

        public PackEntry(Minecraft pMinecraft, TransferableSelectionList pParent, PackSelectionModel.Entry pPack) {
            this.minecraft = pMinecraft;
            this.pack = pPack;
            this.parent = pParent;
            this.nameDisplayCache = cacheName(pMinecraft, pPack.getTitle());
            this.descriptionDisplayCache = cacheDescription(pMinecraft, pPack.getExtendedDescription());
            this.incompatibleNameDisplayCache = cacheName(pMinecraft, TransferableSelectionList.INCOMPATIBLE_TITLE);
            this.incompatibleDescriptionDisplayCache = cacheDescription(pMinecraft, pPack.getCompatibility().getDescription());
        }

        private static FormattedCharSequence cacheName(Minecraft pMinecraft, Component pName) {
            int i = pMinecraft.font.width(pName);
            if (i > 157) {
                FormattedText formattedtext = FormattedText.composite(
                    pMinecraft.font.substrByWidth(pName, 157 - pMinecraft.font.width("...")), FormattedText.of("...")
                );
                return Language.getInstance().getVisualOrder(formattedtext);
            } else {
                return pName.getVisualOrderText();
            }
        }

        private static MultiLineLabel cacheDescription(Minecraft pMinecraft, Component pText) {
            return MultiLineLabel.create(pMinecraft.font, 157, 2, pText);
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", this.pack.getTitle());
        }

        @Override
        public void render(
            GuiGraphics p_281314_,
            int p_283311_,
            int p_281984_,
            int p_282250_,
            int p_281869_,
            int p_283138_,
            int p_282529_,
            int p_282107_,
            boolean p_282429_,
            float p_282306_
        ) {
            PackCompatibility packcompatibility = this.pack.getCompatibility();
            if (!packcompatibility.isCompatible()) {
                int i = p_282250_ + p_281869_ - 3 - (this.parent.scrollbarVisible() ? 7 : 0);
                p_281314_.fill(p_282250_ - 1, p_281984_ - 1, i, p_281984_ + p_283138_ + 1, -8978432);
            }

            p_281314_.blit(RenderType::guiTextured, this.pack.getIconTexture(), p_282250_, p_281984_, 0.0F, 0.0F, 32, 32, 32, 32);
            FormattedCharSequence formattedcharsequence = this.nameDisplayCache;
            MultiLineLabel multilinelabel = this.descriptionDisplayCache;
            if (this.showHoverOverlay()
                && (this.minecraft.options.touchscreen().get() || p_282429_ || this.parent.getSelected() == this && this.parent.isFocused())) {
                p_281314_.fill(p_282250_, p_281984_, p_282250_ + 32, p_281984_ + 32, -1601138544);
                int j = p_282529_ - p_282250_;
                int k = p_282107_ - p_281984_;
                if (!this.pack.getCompatibility().isCompatible()) {
                    formattedcharsequence = this.incompatibleNameDisplayCache;
                    multilinelabel = this.incompatibleDescriptionDisplayCache;
                }

                if (this.pack.canSelect()) {
                    if (j < 32) {
                        p_281314_.blitSprite(RenderType::guiTextured, TransferableSelectionList.SELECT_HIGHLIGHTED_SPRITE, p_282250_, p_281984_, 32, 32);
                    } else {
                        p_281314_.blitSprite(RenderType::guiTextured, TransferableSelectionList.SELECT_SPRITE, p_282250_, p_281984_, 32, 32);
                    }
                } else {
                    if (this.pack.canUnselect()) {
                        if (j < 16) {
                            p_281314_.blitSprite(RenderType::guiTextured, TransferableSelectionList.UNSELECT_HIGHLIGHTED_SPRITE, p_282250_, p_281984_, 32, 32);
                        } else {
                            p_281314_.blitSprite(RenderType::guiTextured, TransferableSelectionList.UNSELECT_SPRITE, p_282250_, p_281984_, 32, 32);
                        }
                    }

                    if (this.pack.canMoveUp()) {
                        if (j < 32 && j > 16 && k < 16) {
                            p_281314_.blitSprite(RenderType::guiTextured, TransferableSelectionList.MOVE_UP_HIGHLIGHTED_SPRITE, p_282250_, p_281984_, 32, 32);
                        } else {
                            p_281314_.blitSprite(RenderType::guiTextured, TransferableSelectionList.MOVE_UP_SPRITE, p_282250_, p_281984_, 32, 32);
                        }
                    }

                    if (this.pack.canMoveDown()) {
                        if (j < 32 && j > 16 && k > 16) {
                            p_281314_.blitSprite(RenderType::guiTextured, TransferableSelectionList.MOVE_DOWN_HIGHLIGHTED_SPRITE, p_282250_, p_281984_, 32, 32);
                        } else {
                            p_281314_.blitSprite(RenderType::guiTextured, TransferableSelectionList.MOVE_DOWN_SPRITE, p_282250_, p_281984_, 32, 32);
                        }
                    }
                }
            }

            p_281314_.drawString(this.minecraft.font, formattedcharsequence, p_282250_ + 32 + 2, p_281984_ + 1, 16777215);
            multilinelabel.renderLeftAligned(p_281314_, p_282250_ + 32 + 2, p_281984_ + 12, 10, -8355712);
        }

        public String getPackId() {
            return this.pack.getId();
        }

        private boolean showHoverOverlay() {
            return !this.pack.isFixedPosition() || !this.pack.isRequired();
        }

        public void keyboardSelection() {
            if (this.pack.canSelect() && this.handlePackSelection()) {
                this.parent.screen.updateFocus(this.parent);
            } else if (this.pack.canUnselect()) {
                this.pack.unselect();
                this.parent.screen.updateFocus(this.parent);
            }
        }

        void keyboardMoveUp() {
            if (this.pack.canMoveUp()) {
                this.pack.moveUp();
            }
        }

        void keyboardMoveDown() {
            if (this.pack.canMoveDown()) {
                this.pack.moveDown();
            }
        }

        private boolean handlePackSelection() {
            if (this.pack.getCompatibility().isCompatible()) {
                this.pack.select();
                return true;
            } else {
                Component component = this.pack.getCompatibility().getConfirmation();
                this.minecraft.setScreen(new ConfirmScreen(p_264693_ -> {
                    this.minecraft.setScreen(this.parent.screen);
                    if (p_264693_) {
                        this.pack.select();
                    }
                }, TransferableSelectionList.INCOMPATIBLE_CONFIRM_TITLE, component));
                return false;
            }
        }

        @Override
        public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            double d0 = pMouseX - (double)this.parent.getRowLeft();
            double d1 = pMouseY - (double)this.parent.getRowTop(this.parent.children().indexOf(this));
            if (this.showHoverOverlay() && d0 <= 32.0) {
                this.parent.screen.clearSelected();
                if (this.pack.canSelect()) {
                    this.handlePackSelection();
                    return true;
                }

                if (d0 < 16.0 && this.pack.canUnselect()) {
                    this.pack.unselect();
                    return true;
                }

                if (d0 > 16.0 && d1 < 16.0 && this.pack.canMoveUp()) {
                    this.pack.moveUp();
                    return true;
                }

                if (d0 > 16.0 && d1 > 16.0 && this.pack.canMoveDown()) {
                    this.pack.moveDown();
                    return true;
                }
            }

            return super.mouseClicked(pMouseX, pMouseY, pButton);
        }
    }
}