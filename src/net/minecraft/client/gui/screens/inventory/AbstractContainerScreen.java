package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.BundleMouseActions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.ItemSlotMouseAction;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractContainerScreen<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T> {
    public static final ResourceLocation INVENTORY_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png");
    private static final ResourceLocation SLOT_HIGHLIGHT_BACK_SPRITE = ResourceLocation.withDefaultNamespace("container/slot_highlight_back");
    private static final ResourceLocation SLOT_HIGHLIGHT_FRONT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot_highlight_front");
    protected static final int BACKGROUND_TEXTURE_WIDTH = 256;
    protected static final int BACKGROUND_TEXTURE_HEIGHT = 256;
    private static final float SNAPBACK_SPEED = 100.0F;
    private static final int QUICKDROP_DELAY = 500;
    public static final int SLOT_ITEM_BLIT_OFFSET = 100;
    private static final int HOVER_ITEM_BLIT_OFFSET = 200;
    protected int imageWidth = 176;
    protected int imageHeight = 166;
    protected int titleLabelX;
    protected int titleLabelY;
    protected int inventoryLabelX;
    protected int inventoryLabelY;
    private final List<ItemSlotMouseAction> itemSlotMouseActions;
    protected final T menu;
    protected final Component playerInventoryTitle;
    @Nullable
    protected Slot hoveredSlot;
    @Nullable
    private Slot clickedSlot;
    @Nullable
    private Slot snapbackEnd;
    @Nullable
    private Slot quickdropSlot;
    @Nullable
    private Slot lastClickSlot;
    protected int leftPos;
    protected int topPos;
    private boolean isSplittingStack;
    private ItemStack draggingItem = ItemStack.EMPTY;
    private int snapbackStartX;
    private int snapbackStartY;
    private long snapbackTime;
    private ItemStack snapbackItem = ItemStack.EMPTY;
    private long quickdropTime;
    protected final Set<Slot> quickCraftSlots = Sets.newHashSet();
    protected boolean isQuickCrafting;
    private int quickCraftingType;
    private int quickCraftingButton;
    private boolean skipNextRelease;
    private int quickCraftingRemainder;
    private long lastClickTime;
    private int lastClickButton;
    private boolean doubleclick;
    private ItemStack lastQuickMoved = ItemStack.EMPTY;

    public AbstractContainerScreen(T pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pTitle);
        this.menu = pMenu;
        this.playerInventoryTitle = pPlayerInventory.getDisplayName();
        this.skipNextRelease = true;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
        this.itemSlotMouseActions = new ArrayList<>();
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        this.itemSlotMouseActions.clear();
        this.addItemSlotMouseAction(new BundleMouseActions(this.minecraft));
    }

    protected void addItemSlotMouseAction(ItemSlotMouseAction pItemSlotMouseAction) {
        this.itemSlotMouseActions.add(pItemSlotMouseAction);
    }

    @Override
    public void render(GuiGraphics p_283479_, int p_283661_, int p_281248_, float p_281886_) {
        int i = this.leftPos;
        int j = this.topPos;
        super.render(p_283479_, p_283661_, p_281248_, p_281886_);
        p_283479_.pose().pushPose();
        p_283479_.pose().translate((float)i, (float)j, 0.0F);
        Slot slot = this.hoveredSlot;
        this.hoveredSlot = this.getHoveredSlot((double)p_283661_, (double)p_281248_);
        this.renderSlotHighlightBack(p_283479_);
        this.renderSlots(p_283479_);
        this.renderSlotHighlightFront(p_283479_);
        if (slot != null && slot != this.hoveredSlot) {
            this.onStopHovering(slot);
        }

        this.renderLabels(p_283479_, p_283661_, p_281248_);
        ItemStack itemstack = this.draggingItem.isEmpty() ? this.menu.getCarried() : this.draggingItem;
        if (!itemstack.isEmpty()) {
            int k = 8;
            int l = this.draggingItem.isEmpty() ? 8 : 16;
            String s = null;
            if (!this.draggingItem.isEmpty() && this.isSplittingStack) {
                itemstack = itemstack.copyWithCount(Mth.ceil((float)itemstack.getCount() / 2.0F));
            } else if (this.isQuickCrafting && this.quickCraftSlots.size() > 1) {
                itemstack = itemstack.copyWithCount(this.quickCraftingRemainder);
                if (itemstack.isEmpty()) {
                    s = ChatFormatting.YELLOW + "0";
                }
            }

            this.renderFloatingItem(p_283479_, itemstack, p_283661_ - i - 8, p_281248_ - j - l, s);
        }

        if (!this.snapbackItem.isEmpty()) {
            float f = (float)(Util.getMillis() - this.snapbackTime) / 100.0F;
            if (f >= 1.0F) {
                f = 1.0F;
                this.snapbackItem = ItemStack.EMPTY;
            }

            int k1 = this.snapbackEnd.x - this.snapbackStartX;
            int l1 = this.snapbackEnd.y - this.snapbackStartY;
            int i1 = this.snapbackStartX + (int)((float)k1 * f);
            int j1 = this.snapbackStartY + (int)((float)l1 * f);
            this.renderFloatingItem(p_283479_, this.snapbackItem, i1, j1, null);
        }

        p_283479_.pose().popPose();
    }

    protected void renderSlots(GuiGraphics pGuiGraphics) {
        for (Slot slot : this.menu.slots) {
            if (slot.isActive()) {
                this.renderSlot(pGuiGraphics, slot);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics p_300197_, int p_297538_, int p_300104_, float p_298759_) {
        this.renderTransparentBackground(p_300197_);
        this.renderBg(p_300197_, p_298759_, p_297538_, p_300104_);
    }

    @Override
    public boolean mouseScrolled(double p_367670_, double p_363682_, double p_364454_, double p_367273_) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            for (ItemSlotMouseAction itemslotmouseaction : this.itemSlotMouseActions) {
                if (itemslotmouseaction.matches(this.hoveredSlot)
                    && itemslotmouseaction.onMouseScrolled(p_364454_, p_367273_, this.hoveredSlot.index, this.hoveredSlot.getItem())) {
                    return true;
                }
            }
        }

        return false;
    }

    private void renderSlotHighlightBack(GuiGraphics pGuiGraphics) {
        if (this.hoveredSlot != null && this.hoveredSlot.isHighlightable()) {
            pGuiGraphics.blitSprite(RenderType::guiTextured, SLOT_HIGHLIGHT_BACK_SPRITE, this.hoveredSlot.x - 4, this.hoveredSlot.y - 4, 24, 24);
        }
    }

    private void renderSlotHighlightFront(GuiGraphics pGuiGraphics) {
        if (this.hoveredSlot != null && this.hoveredSlot.isHighlightable()) {
            pGuiGraphics.blitSprite(RenderType::guiTexturedOverlay, SLOT_HIGHLIGHT_FRONT_SPRITE, this.hoveredSlot.x - 4, this.hoveredSlot.y - 4, 24, 24);
        }
    }

    protected void renderTooltip(GuiGraphics pGuiGraphics, int pX, int pY) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            ItemStack itemstack = this.hoveredSlot.getItem();
            if (this.menu.getCarried().isEmpty() || this.showTooltipWithItemInHand(itemstack)) {
                pGuiGraphics.renderTooltip(
                    this.font, this.getTooltipFromContainerItem(itemstack), itemstack.getTooltipImage(), pX, pY, itemstack.get(DataComponents.TOOLTIP_STYLE)
                );
            }
        }
    }

    private boolean showTooltipWithItemInHand(ItemStack pStack) {
        return pStack.getTooltipImage().map(ClientTooltipComponent::create).map(ClientTooltipComponent::showTooltipWithItemInHand).orElse(false);
    }

    protected List<Component> getTooltipFromContainerItem(ItemStack pStack) {
        return getTooltipFromItem(this.minecraft, pStack);
    }

    private void renderFloatingItem(GuiGraphics pGuiGraphics, ItemStack pStack, int pX, int pY, @Nullable String pText) {
        pGuiGraphics.pose().pushPose();
        pGuiGraphics.pose().translate(0.0F, 0.0F, 232.0F);
        pGuiGraphics.renderItem(pStack, pX, pY);
        pGuiGraphics.renderItemDecorations(this.font, pStack, pX, pY - (this.draggingItem.isEmpty() ? 0 : 8), pText);
        pGuiGraphics.pose().popPose();
    }

    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        pGuiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        pGuiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    protected abstract void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY);

    protected void renderSlot(GuiGraphics pGuiGraphics, Slot pSlot) {
        int i = pSlot.x;
        int j = pSlot.y;
        ItemStack itemstack = pSlot.getItem();
        boolean flag = false;
        boolean flag1 = pSlot == this.clickedSlot && !this.draggingItem.isEmpty() && !this.isSplittingStack;
        ItemStack itemstack1 = this.menu.getCarried();
        String s = null;
        if (pSlot == this.clickedSlot && !this.draggingItem.isEmpty() && this.isSplittingStack && !itemstack.isEmpty()) {
            itemstack = itemstack.copyWithCount(itemstack.getCount() / 2);
        } else if (this.isQuickCrafting && this.quickCraftSlots.contains(pSlot) && !itemstack1.isEmpty()) {
            if (this.quickCraftSlots.size() == 1) {
                return;
            }

            if (AbstractContainerMenu.canItemQuickReplace(pSlot, itemstack1, true) && this.menu.canDragTo(pSlot)) {
                flag = true;
                int k = Math.min(itemstack1.getMaxStackSize(), pSlot.getMaxStackSize(itemstack1));
                int l = pSlot.getItem().isEmpty() ? 0 : pSlot.getItem().getCount();
                int i1 = AbstractContainerMenu.getQuickCraftPlaceCount(this.quickCraftSlots, this.quickCraftingType, itemstack1) + l;
                if (i1 > k) {
                    i1 = k;
                    s = ChatFormatting.YELLOW.toString() + k;
                }

                itemstack = itemstack1.copyWithCount(i1);
            } else {
                this.quickCraftSlots.remove(pSlot);
                this.recalculateQuickCraftRemaining();
            }
        }

        pGuiGraphics.pose().pushPose();
        pGuiGraphics.pose().translate(0.0F, 0.0F, 100.0F);
        if (itemstack.isEmpty() && pSlot.isActive()) {
            ResourceLocation resourcelocation = pSlot.getNoItemIcon();
            if (resourcelocation != null) {
                pGuiGraphics.blitSprite(RenderType::guiTextured, resourcelocation, i, j, 16, 16);
                flag1 = true;
            }
        }

        if (!flag1) {
            if (flag) {
                pGuiGraphics.fill(i, j, i + 16, j + 16, -2130706433);
            }

            int j1 = pSlot.x + pSlot.y * this.imageWidth;
            if (pSlot.isFake()) {
                pGuiGraphics.renderFakeItem(itemstack, i, j, j1);
            } else {
                pGuiGraphics.renderItem(itemstack, i, j, j1);
            }

            pGuiGraphics.renderItemDecorations(this.font, itemstack, i, j, s);
        }

        pGuiGraphics.pose().popPose();
    }

    private void recalculateQuickCraftRemaining() {
        ItemStack itemstack = this.menu.getCarried();
        if (!itemstack.isEmpty() && this.isQuickCrafting) {
            if (this.quickCraftingType == 2) {
                this.quickCraftingRemainder = itemstack.getMaxStackSize();
            } else {
                this.quickCraftingRemainder = itemstack.getCount();

                for (Slot slot : this.quickCraftSlots) {
                    ItemStack itemstack1 = slot.getItem();
                    int i = itemstack1.isEmpty() ? 0 : itemstack1.getCount();
                    int j = Math.min(itemstack.getMaxStackSize(), slot.getMaxStackSize(itemstack));
                    int k = Math.min(AbstractContainerMenu.getQuickCraftPlaceCount(this.quickCraftSlots, this.quickCraftingType, itemstack) + i, j);
                    this.quickCraftingRemainder -= k - i;
                }
            }
        }
    }

    @Nullable
    private Slot getHoveredSlot(double pMouseX, double pMouseY) {
        for (Slot slot : this.menu.slots) {
            if (slot.isActive() && this.isHovering(slot, pMouseX, pMouseY)) {
                return slot;
            }
        }

        return null;
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (super.mouseClicked(pMouseX, pMouseY, pButton)) {
            return true;
        } else {
            boolean flag = this.minecraft.options.keyPickItem.matchesMouse(pButton) && this.minecraft.gameMode.hasInfiniteItems();
            Slot slot = this.getHoveredSlot(pMouseX, pMouseY);
            long i = Util.getMillis();
            this.doubleclick = this.lastClickSlot == slot && i - this.lastClickTime < 250L && this.lastClickButton == pButton;
            this.skipNextRelease = false;
            if (pButton != 0 && pButton != 1 && !flag) {
                this.checkHotbarMouseClicked(pButton);
            } else {
                int j = this.leftPos;
                int k = this.topPos;
                boolean flag1 = this.hasClickedOutside(pMouseX, pMouseY, j, k, pButton);
                int l = -1;
                if (slot != null) {
                    l = slot.index;
                }

                if (flag1) {
                    l = -999;
                }

                if (this.minecraft.options.touchscreen().get() && flag1 && this.menu.getCarried().isEmpty()) {
                    this.onClose();
                    return true;
                }

                if (l != -1) {
                    if (this.minecraft.options.touchscreen().get()) {
                        if (slot != null && slot.hasItem()) {
                            this.clickedSlot = slot;
                            this.draggingItem = ItemStack.EMPTY;
                            this.isSplittingStack = pButton == 1;
                        } else {
                            this.clickedSlot = null;
                        }
                    } else if (!this.isQuickCrafting) {
                        if (this.menu.getCarried().isEmpty()) {
                            if (flag) {
                                this.slotClicked(slot, l, pButton, ClickType.CLONE);
                            } else {
                                boolean flag2 = l != -999
                                    && (
                                        InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 340)
                                            || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 344)
                                    );
                                ClickType clicktype = ClickType.PICKUP;
                                if (flag2) {
                                    this.lastQuickMoved = slot != null && slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
                                    clicktype = ClickType.QUICK_MOVE;
                                } else if (l == -999) {
                                    clicktype = ClickType.THROW;
                                }

                                this.slotClicked(slot, l, pButton, clicktype);
                            }

                            this.skipNextRelease = true;
                        } else {
                            this.isQuickCrafting = true;
                            this.quickCraftingButton = pButton;
                            this.quickCraftSlots.clear();
                            if (pButton == 0) {
                                this.quickCraftingType = 0;
                            } else if (pButton == 1) {
                                this.quickCraftingType = 1;
                            } else if (flag) {
                                this.quickCraftingType = 2;
                            }
                        }
                    }
                }
            }

            this.lastClickSlot = slot;
            this.lastClickTime = i;
            this.lastClickButton = pButton;
            return true;
        }
    }

    private void checkHotbarMouseClicked(int pKeyCode) {
        if (this.hoveredSlot != null && this.menu.getCarried().isEmpty()) {
            if (this.minecraft.options.keySwapOffhand.matchesMouse(pKeyCode)) {
                this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 40, ClickType.SWAP);
                return;
            }

            for (int i = 0; i < 9; i++) {
                if (this.minecraft.options.keyHotbarSlots[i].matchesMouse(pKeyCode)) {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, i, ClickType.SWAP);
                }
            }
        }
    }

    protected boolean hasClickedOutside(double pMouseX, double pMouseY, int pGuiLeft, int pGuiTop, int pMouseButton) {
        return pMouseX < (double)pGuiLeft
            || pMouseY < (double)pGuiTop
            || pMouseX >= (double)(pGuiLeft + this.imageWidth)
            || pMouseY >= (double)(pGuiTop + this.imageHeight);
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        Slot slot = this.getHoveredSlot(pMouseX, pMouseY);
        ItemStack itemstack = this.menu.getCarried();
        if (this.clickedSlot != null && this.minecraft.options.touchscreen().get()) {
            if (pButton == 0 || pButton == 1) {
                if (this.draggingItem.isEmpty()) {
                    if (slot != this.clickedSlot && !this.clickedSlot.getItem().isEmpty()) {
                        this.draggingItem = this.clickedSlot.getItem().copy();
                    }
                } else if (this.draggingItem.getCount() > 1 && slot != null && AbstractContainerMenu.canItemQuickReplace(slot, this.draggingItem, false)) {
                    long i = Util.getMillis();
                    if (this.quickdropSlot == slot) {
                        if (i - this.quickdropTime > 500L) {
                            this.slotClicked(this.clickedSlot, this.clickedSlot.index, 0, ClickType.PICKUP);
                            this.slotClicked(slot, slot.index, 1, ClickType.PICKUP);
                            this.slotClicked(this.clickedSlot, this.clickedSlot.index, 0, ClickType.PICKUP);
                            this.quickdropTime = i + 750L;
                            this.draggingItem.shrink(1);
                        }
                    } else {
                        this.quickdropSlot = slot;
                        this.quickdropTime = i;
                    }
                }
            }
        } else if (this.isQuickCrafting
            && slot != null
            && !itemstack.isEmpty()
            && (itemstack.getCount() > this.quickCraftSlots.size() || this.quickCraftingType == 2)
            && AbstractContainerMenu.canItemQuickReplace(slot, itemstack, true)
            && slot.mayPlace(itemstack)
            && this.menu.canDragTo(slot)) {
            this.quickCraftSlots.add(slot);
            this.recalculateQuickCraftRemaining();
        }

        return true;
    }

    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        Slot slot = this.getHoveredSlot(pMouseX, pMouseY);
        int i = this.leftPos;
        int j = this.topPos;
        boolean flag = this.hasClickedOutside(pMouseX, pMouseY, i, j, pButton);
        int k = -1;
        if (slot != null) {
            k = slot.index;
        }

        if (flag) {
            k = -999;
        }

        if (this.doubleclick && slot != null && pButton == 0 && this.menu.canTakeItemForPickAll(ItemStack.EMPTY, slot)) {
            if (hasShiftDown()) {
                if (!this.lastQuickMoved.isEmpty()) {
                    for (Slot slot2 : this.menu.slots) {
                        if (slot2 != null
                            && slot2.mayPickup(this.minecraft.player)
                            && slot2.hasItem()
                            && slot2.container == slot.container
                            && AbstractContainerMenu.canItemQuickReplace(slot2, this.lastQuickMoved, true)) {
                            this.slotClicked(slot2, slot2.index, pButton, ClickType.QUICK_MOVE);
                        }
                    }
                }
            } else {
                this.slotClicked(slot, k, pButton, ClickType.PICKUP_ALL);
            }

            this.doubleclick = false;
            this.lastClickTime = 0L;
        } else {
            if (this.isQuickCrafting && this.quickCraftingButton != pButton) {
                this.isQuickCrafting = false;
                this.quickCraftSlots.clear();
                this.skipNextRelease = true;
                return true;
            }

            if (this.skipNextRelease) {
                this.skipNextRelease = false;
                return true;
            }

            if (this.clickedSlot != null && this.minecraft.options.touchscreen().get()) {
                if (pButton == 0 || pButton == 1) {
                    if (this.draggingItem.isEmpty() && slot != this.clickedSlot) {
                        this.draggingItem = this.clickedSlot.getItem();
                    }

                    boolean flag2 = AbstractContainerMenu.canItemQuickReplace(slot, this.draggingItem, false);
                    if (k != -1 && !this.draggingItem.isEmpty() && flag2) {
                        this.slotClicked(this.clickedSlot, this.clickedSlot.index, pButton, ClickType.PICKUP);
                        this.slotClicked(slot, k, 0, ClickType.PICKUP);
                        if (this.menu.getCarried().isEmpty()) {
                            this.snapbackItem = ItemStack.EMPTY;
                        } else {
                            this.slotClicked(this.clickedSlot, this.clickedSlot.index, pButton, ClickType.PICKUP);
                            this.snapbackStartX = Mth.floor(pMouseX - (double)i);
                            this.snapbackStartY = Mth.floor(pMouseY - (double)j);
                            this.snapbackEnd = this.clickedSlot;
                            this.snapbackItem = this.draggingItem;
                            this.snapbackTime = Util.getMillis();
                        }
                    } else if (!this.draggingItem.isEmpty()) {
                        this.snapbackStartX = Mth.floor(pMouseX - (double)i);
                        this.snapbackStartY = Mth.floor(pMouseY - (double)j);
                        this.snapbackEnd = this.clickedSlot;
                        this.snapbackItem = this.draggingItem;
                        this.snapbackTime = Util.getMillis();
                    }

                    this.clearDraggingState();
                }
            } else if (this.isQuickCrafting && !this.quickCraftSlots.isEmpty()) {
                this.slotClicked(null, -999, AbstractContainerMenu.getQuickcraftMask(0, this.quickCraftingType), ClickType.QUICK_CRAFT);

                for (Slot slot1 : this.quickCraftSlots) {
                    this.slotClicked(slot1, slot1.index, AbstractContainerMenu.getQuickcraftMask(1, this.quickCraftingType), ClickType.QUICK_CRAFT);
                }

                this.slotClicked(null, -999, AbstractContainerMenu.getQuickcraftMask(2, this.quickCraftingType), ClickType.QUICK_CRAFT);
            } else if (!this.menu.getCarried().isEmpty()) {
                if (this.minecraft.options.keyPickItem.matchesMouse(pButton)) {
                    this.slotClicked(slot, k, pButton, ClickType.CLONE);
                } else {
                    boolean flag1 = k != -999
                        && (
                            InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 340)
                                || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 344)
                        );
                    if (flag1) {
                        this.lastQuickMoved = slot != null && slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
                    }

                    this.slotClicked(slot, k, pButton, flag1 ? ClickType.QUICK_MOVE : ClickType.PICKUP);
                }
            }
        }

        if (this.menu.getCarried().isEmpty()) {
            this.lastClickTime = 0L;
        }

        this.isQuickCrafting = false;
        return true;
    }

    public void clearDraggingState() {
        this.draggingItem = ItemStack.EMPTY;
        this.clickedSlot = null;
    }

    private boolean isHovering(Slot pSlot, double pMouseX, double pMouseY) {
        return this.isHovering(pSlot.x, pSlot.y, 16, 16, pMouseX, pMouseY);
    }

    protected boolean isHovering(int pX, int pY, int pWidth, int pHeight, double pMouseX, double pMouseY) {
        int i = this.leftPos;
        int j = this.topPos;
        pMouseX -= (double)i;
        pMouseY -= (double)j;
        return pMouseX >= (double)(pX - 1)
            && pMouseX < (double)(pX + pWidth + 1)
            && pMouseY >= (double)(pY - 1)
            && pMouseY < (double)(pY + pHeight + 1);
    }

    private void onStopHovering(Slot pSlot) {
        if (pSlot.hasItem()) {
            for (ItemSlotMouseAction itemslotmouseaction : this.itemSlotMouseActions) {
                if (itemslotmouseaction.matches(pSlot)) {
                    itemslotmouseaction.onStopHovering(pSlot);
                }
            }
        }
    }

    protected void slotClicked(Slot pSlot, int pSlotId, int pMouseButton, ClickType pType) {
        if (pSlot != null) {
            pSlotId = pSlot.index;
        }

        this.onMouseClickAction(pSlot, pType);
        this.minecraft.gameMode.handleInventoryMouseClick(this.menu.containerId, pSlotId, pMouseButton, pType, this.minecraft.player);
    }

    void onMouseClickAction(@Nullable Slot pSlot, ClickType pType) {
        if (pSlot != null && pSlot.hasItem()) {
            for (ItemSlotMouseAction itemslotmouseaction : this.itemSlotMouseActions) {
                if (itemslotmouseaction.matches(pSlot)) {
                    itemslotmouseaction.onSlotClicked(pSlot, pType);
                }
            }
        }
    }

    protected void handleSlotStateChanged(int pSlotId, int pContainerId, boolean pNewState) {
        this.minecraft.gameMode.handleSlotStateChanged(pSlotId, pContainerId, pNewState);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (super.keyPressed(pKeyCode, pScanCode, pModifiers)) {
            return true;
        } else if (this.minecraft.options.keyInventory.matches(pKeyCode, pScanCode)) {
            this.onClose();
            return true;
        } else {
            this.checkHotbarKeyPressed(pKeyCode, pScanCode);
            if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
                if (this.minecraft.options.keyPickItem.matches(pKeyCode, pScanCode)) {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 0, ClickType.CLONE);
                } else if (this.minecraft.options.keyDrop.matches(pKeyCode, pScanCode)) {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, hasControlDown() ? 1 : 0, ClickType.THROW);
                }
            }

            return true;
        }
    }

    protected boolean checkHotbarKeyPressed(int pKeyCode, int pScanCode) {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null) {
            if (this.minecraft.options.keySwapOffhand.matches(pKeyCode, pScanCode)) {
                this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 40, ClickType.SWAP);
                return true;
            }

            for (int i = 0; i < 9; i++) {
                if (this.minecraft.options.keyHotbarSlots[i].matches(pKeyCode, pScanCode)) {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, i, ClickType.SWAP);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void removed() {
        if (this.minecraft.player != null) {
            this.menu.removed(this.minecraft.player);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public final void tick() {
        super.tick();
        if (this.minecraft.player.isAlive() && !this.minecraft.player.isRemoved()) {
            this.containerTick();
        } else {
            this.minecraft.player.closeContainer();
        }
    }

    protected void containerTick() {
    }

    @Override
    public T getMenu() {
        return this.menu;
    }

    @Override
    public void onClose() {
        this.minecraft.player.closeContainer();
        if (this.hoveredSlot != null) {
            this.onStopHovering(this.hoveredSlot);
        }

        super.onClose();
    }
}