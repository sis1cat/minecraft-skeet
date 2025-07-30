package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.HotbarManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.SessionSearchTrees;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.inventory.Hotbar;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CreativeModeInventoryScreen extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller");
    private static final ResourceLocation SCROLLER_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller_disabled");
    private static final ResourceLocation[] UNSELECTED_TOP_TABS = new ResourceLocation[]{
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_1"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_2"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_3"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_4"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_5"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_6"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_7")
    };
    private static final ResourceLocation[] SELECTED_TOP_TABS = new ResourceLocation[]{
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_1"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_2"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_3"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_4"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_5"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_6"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_7")
    };
    private static final ResourceLocation[] UNSELECTED_BOTTOM_TABS = new ResourceLocation[]{
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_unselected_1"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_unselected_2"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_unselected_3"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_unselected_4"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_unselected_5"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_unselected_6"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_unselected_7")
    };
    private static final ResourceLocation[] SELECTED_BOTTOM_TABS = new ResourceLocation[]{
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_1"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_2"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_3"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_4"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_5"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_6"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_7")
    };
    private static final int NUM_ROWS = 5;
    private static final int NUM_COLS = 9;
    private static final int TAB_WIDTH = 26;
    private static final int TAB_HEIGHT = 32;
    private static final int SCROLLER_WIDTH = 12;
    private static final int SCROLLER_HEIGHT = 15;
    static final SimpleContainer CONTAINER = new SimpleContainer(45);
    private static final Component TRASH_SLOT_TOOLTIP = Component.translatable("inventory.binSlot");
    private static final int TEXT_COLOR = 16777215;
    private static CreativeModeTab selectedTab = CreativeModeTabs.getDefaultTab();
    private float scrollOffs;
    private boolean scrolling;
    private EditBox searchBox;
    @Nullable
    private List<Slot> originalSlots;
    @Nullable
    private Slot destroyItemSlot;
    private CreativeInventoryListener listener;
    private boolean ignoreTextInput;
    private boolean hasClickedOutside;
    private final Set<TagKey<Item>> visibleTags = new HashSet<>();
    private final boolean displayOperatorCreativeTab;
    private final EffectsInInventory effects;

    public CreativeModeInventoryScreen(LocalPlayer pPlayer, FeatureFlagSet pEnabledFeatures, boolean pDisplayOperatorCreativeTab) {
        super(new CreativeModeInventoryScreen.ItemPickerMenu(pPlayer), pPlayer.getInventory(), CommonComponents.EMPTY);
        pPlayer.containerMenu = this.menu;
        this.imageHeight = 136;
        this.imageWidth = 195;
        this.displayOperatorCreativeTab = pDisplayOperatorCreativeTab;
        this.tryRebuildTabContents(pPlayer.connection.searchTrees(), pEnabledFeatures, this.hasPermissions(pPlayer), pPlayer.level().registryAccess());
        this.effects = new EffectsInInventory(this);
    }

    private boolean hasPermissions(Player pPlayer) {
        return pPlayer.canUseGameMasterBlocks() && this.displayOperatorCreativeTab;
    }

    private void tryRefreshInvalidatedTabs(FeatureFlagSet pEnabledFeatures, boolean pHasPermissions, HolderLookup.Provider pProvider) {
        ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
        if (this.tryRebuildTabContents(clientpacketlistener != null ? clientpacketlistener.searchTrees() : null, pEnabledFeatures, pHasPermissions, pProvider)) {
            for (CreativeModeTab creativemodetab : CreativeModeTabs.allTabs()) {
                Collection<ItemStack> collection = creativemodetab.getDisplayItems();
                if (creativemodetab == selectedTab) {
                    if (creativemodetab.getType() == CreativeModeTab.Type.CATEGORY && collection.isEmpty()) {
                        this.selectTab(CreativeModeTabs.getDefaultTab());
                    } else {
                        this.refreshCurrentTabContents(collection);
                    }
                }
            }
        }
    }

    private boolean tryRebuildTabContents(@Nullable SessionSearchTrees pSearchTrees, FeatureFlagSet pEnabledFeatures, boolean pHasPermissions, HolderLookup.Provider pRegistries) {
        if (!CreativeModeTabs.tryRebuildTabContents(pEnabledFeatures, pHasPermissions, pRegistries)) {
            return false;
        } else {
            if (pSearchTrees != null) {
                List<ItemStack> list = List.copyOf(CreativeModeTabs.searchTab().getDisplayItems());
                pSearchTrees.updateCreativeTooltips(pRegistries, list);
                pSearchTrees.updateCreativeTags(list);
            }

            return true;
        }
    }

    private void refreshCurrentTabContents(Collection<ItemStack> pItems) {
        int i = this.menu.getRowIndexForScroll(this.scrollOffs);
        this.menu.items.clear();
        if (selectedTab.getType() == CreativeModeTab.Type.SEARCH) {
            this.refreshSearchResults();
        } else {
            this.menu.items.addAll(pItems);
        }

        this.scrollOffs = this.menu.getScrollForRowIndex(i);
        this.menu.scrollTo(this.scrollOffs);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (this.minecraft != null) {
            if (this.minecraft.player != null) {
                this.tryRefreshInvalidatedTabs(this.minecraft.player.connection.enabledFeatures(), this.hasPermissions(this.minecraft.player), this.minecraft.player.level().registryAccess());
            }

            if (!this.minecraft.gameMode.hasInfiniteItems()) {
                this.minecraft.setScreen(new InventoryScreen(this.minecraft.player));
            }
        }
    }

    @Override
    protected void slotClicked(@Nullable Slot pSlot, int pSlotId, int pMouseButton, ClickType pType) {
        if (this.isCreativeSlot(pSlot)) {
            this.searchBox.moveCursorToEnd(false);
            this.searchBox.setHighlightPos(0);
        }

        boolean flag = pType == ClickType.QUICK_MOVE;
        pType = pSlotId == -999 && pType == ClickType.PICKUP ? ClickType.THROW : pType;
        if (pType != ClickType.THROW || this.minecraft.player.canDropItems()) {
            this.onMouseClickAction(pSlot, pType);
            if (pSlot == null && selectedTab.getType() != CreativeModeTab.Type.INVENTORY && pType != ClickType.QUICK_CRAFT) {
                if (!this.menu.getCarried().isEmpty() && this.hasClickedOutside) {
                    if (!this.minecraft.player.canDropItems()) {
                        return;
                    }

                    if (pMouseButton == 0) {
                        this.minecraft.player.drop(this.menu.getCarried(), true);
                        this.minecraft.gameMode.handleCreativeModeItemDrop(this.menu.getCarried());
                        this.menu.setCarried(ItemStack.EMPTY);
                    }

                    if (pMouseButton == 1) {
                        ItemStack itemstack5 = this.menu.getCarried().split(1);
                        this.minecraft.player.drop(itemstack5, true);
                        this.minecraft.gameMode.handleCreativeModeItemDrop(itemstack5);
                    }
                }
            } else {
                if (pSlot != null && !pSlot.mayPickup(this.minecraft.player)) {
                    return;
                }

                if (pSlot == this.destroyItemSlot && flag) {
                    for (int i = 0; i < this.minecraft.player.inventoryMenu.getItems().size(); i++) {
                        this.minecraft.player.inventoryMenu.getSlot(i).set(ItemStack.EMPTY);
                        this.minecraft.gameMode.handleCreativeModeItemAdd(ItemStack.EMPTY, i);
                    }
                } else if (selectedTab.getType() == CreativeModeTab.Type.INVENTORY) {
                    if (pSlot == this.destroyItemSlot) {
                        this.menu.setCarried(ItemStack.EMPTY);
                    } else if (pType == ClickType.THROW && pSlot != null && pSlot.hasItem()) {
                        ItemStack itemstack = pSlot.remove(pMouseButton == 0 ? 1 : pSlot.getItem().getMaxStackSize());
                        ItemStack itemstack1 = pSlot.getItem();
                        this.minecraft.player.drop(itemstack, true);
                        this.minecraft.gameMode.handleCreativeModeItemDrop(itemstack);
                        this.minecraft.gameMode.handleCreativeModeItemAdd(itemstack1, ((CreativeModeInventoryScreen.SlotWrapper)pSlot).target.index);
                    } else if (pType == ClickType.THROW && pSlotId == -999 && !this.menu.getCarried().isEmpty()) {
                        this.minecraft.player.drop(this.menu.getCarried(), true);
                        this.minecraft.gameMode.handleCreativeModeItemDrop(this.menu.getCarried());
                        this.menu.setCarried(ItemStack.EMPTY);
                    } else {
                        this.minecraft
                            .player
                            .inventoryMenu
                            .clicked(
                                pSlot == null ? pSlotId : ((CreativeModeInventoryScreen.SlotWrapper)pSlot).target.index,
                                pMouseButton,
                                pType,
                                this.minecraft.player
                            );
                        this.minecraft.player.inventoryMenu.broadcastChanges();
                    }
                } else if (pType != ClickType.QUICK_CRAFT && pSlot.container == CONTAINER) {
                    ItemStack itemstack4 = this.menu.getCarried();
                    ItemStack itemstack6 = pSlot.getItem();
                    if (pType == ClickType.SWAP) {
                        if (!itemstack6.isEmpty()) {
                            this.minecraft.player.getInventory().setItem(pMouseButton, itemstack6.copyWithCount(itemstack6.getMaxStackSize()));
                            this.minecraft.player.inventoryMenu.broadcastChanges();
                        }

                        return;
                    }

                    if (pType == ClickType.CLONE) {
                        if (this.menu.getCarried().isEmpty() && pSlot.hasItem()) {
                            ItemStack itemstack8 = pSlot.getItem();
                            this.menu.setCarried(itemstack8.copyWithCount(itemstack8.getMaxStackSize()));
                        }

                        return;
                    }

                    if (pType == ClickType.THROW) {
                        if (!itemstack6.isEmpty()) {
                            ItemStack itemstack7 = itemstack6.copyWithCount(pMouseButton == 0 ? 1 : itemstack6.getMaxStackSize());
                            this.minecraft.player.drop(itemstack7, true);
                            this.minecraft.gameMode.handleCreativeModeItemDrop(itemstack7);
                        }

                        return;
                    }

                    if (!itemstack4.isEmpty() && !itemstack6.isEmpty() && ItemStack.isSameItemSameComponents(itemstack4, itemstack6)) {
                        if (pMouseButton == 0) {
                            if (flag) {
                                itemstack4.setCount(itemstack4.getMaxStackSize());
                            } else if (itemstack4.getCount() < itemstack4.getMaxStackSize()) {
                                itemstack4.grow(1);
                            }
                        } else {
                            itemstack4.shrink(1);
                        }
                    } else if (!itemstack6.isEmpty() && itemstack4.isEmpty()) {
                        int l = flag ? itemstack6.getMaxStackSize() : itemstack6.getCount();
                        this.menu.setCarried(itemstack6.copyWithCount(l));
                    } else if (pMouseButton == 0) {
                        this.menu.setCarried(ItemStack.EMPTY);
                    } else if (!this.menu.getCarried().isEmpty()) {
                        this.menu.getCarried().shrink(1);
                    }
                } else if (this.menu != null) {
                    ItemStack itemstack3 = pSlot == null ? ItemStack.EMPTY : this.menu.getSlot(pSlot.index).getItem();
                    this.menu.clicked(pSlot == null ? pSlotId : pSlot.index, pMouseButton, pType, this.minecraft.player);
                    if (AbstractContainerMenu.getQuickcraftHeader(pMouseButton) == 2) {
                        for (int j = 0; j < 9; j++) {
                            this.minecraft.gameMode.handleCreativeModeItemAdd(this.menu.getSlot(45 + j).getItem(), 36 + j);
                        }
                    } else if (pSlot != null && Inventory.isHotbarSlot(pSlot.getContainerSlot()) && selectedTab.getType() != CreativeModeTab.Type.INVENTORY) {
                        if (pType == ClickType.THROW && !itemstack3.isEmpty() && !this.menu.getCarried().isEmpty()) {
                            int k = pMouseButton == 0 ? 1 : itemstack3.getCount();
                            ItemStack itemstack2 = itemstack3.copyWithCount(k);
                            itemstack3.shrink(k);
                            this.minecraft.player.drop(itemstack2, true);
                            this.minecraft.gameMode.handleCreativeModeItemDrop(itemstack2);
                        }

                        this.minecraft.player.inventoryMenu.broadcastChanges();
                    }
                }
            }
        }
    }

    private boolean isCreativeSlot(@Nullable Slot pSlot) {
        return pSlot != null && pSlot.container == CONTAINER;
    }

    @Override
    protected void init() {
        if (this.minecraft.gameMode.hasInfiniteItems()) {
            super.init();
            this.searchBox = new EditBox(this.font, this.leftPos + 82, this.topPos + 6, 80, 9, Component.translatable("itemGroup.search"));
            this.searchBox.setMaxLength(50);
            this.searchBox.setBordered(false);
            this.searchBox.setVisible(false);
            this.searchBox.setTextColor(16777215);
            this.addWidget(this.searchBox);
            CreativeModeTab creativemodetab = selectedTab;
            selectedTab = CreativeModeTabs.getDefaultTab();
            this.selectTab(creativemodetab);
            this.minecraft.player.inventoryMenu.removeSlotListener(this.listener);
            this.listener = new CreativeInventoryListener(this.minecraft);
            this.minecraft.player.inventoryMenu.addSlotListener(this.listener);
            if (!selectedTab.shouldDisplay()) {
                this.selectTab(CreativeModeTabs.getDefaultTab());
            }
        } else {
            this.minecraft.setScreen(new InventoryScreen(this.minecraft.player));
        }
    }

    @Override
    public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
        int i = this.menu.getRowIndexForScroll(this.scrollOffs);
        String s = this.searchBox.getValue();
        this.init(pMinecraft, pWidth, pHeight);
        this.searchBox.setValue(s);
        if (!this.searchBox.getValue().isEmpty()) {
            this.refreshSearchResults();
        }

        this.scrollOffs = this.menu.getScrollForRowIndex(i);
        this.menu.scrollTo(this.scrollOffs);
    }

    @Override
    public void removed() {
        super.removed();
        if (this.minecraft.player != null && this.minecraft.player.getInventory() != null) {
            this.minecraft.player.inventoryMenu.removeSlotListener(this.listener);
        }
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        if (this.ignoreTextInput) {
            return false;
        } else if (selectedTab.getType() != CreativeModeTab.Type.SEARCH) {
            return false;
        } else {
            String s = this.searchBox.getValue();
            if (this.searchBox.charTyped(pCodePoint, pModifiers)) {
                if (!Objects.equals(s, this.searchBox.getValue())) {
                    this.refreshSearchResults();
                }

                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        this.ignoreTextInput = false;
        if (selectedTab.getType() != CreativeModeTab.Type.SEARCH) {
            if (this.minecraft.options.keyChat.matches(pKeyCode, pScanCode)) {
                this.ignoreTextInput = true;
                this.selectTab(CreativeModeTabs.searchTab());
                return true;
            } else {
                return super.keyPressed(pKeyCode, pScanCode, pModifiers);
            }
        } else {
            boolean flag = !this.isCreativeSlot(this.hoveredSlot) || this.hoveredSlot.hasItem();
            boolean flag1 = InputConstants.getKey(pKeyCode, pScanCode).getNumericKeyValue().isPresent();
            if (flag && flag1 && this.checkHotbarKeyPressed(pKeyCode, pScanCode)) {
                this.ignoreTextInput = true;
                return true;
            } else {
                String s = this.searchBox.getValue();
                if (this.searchBox.keyPressed(pKeyCode, pScanCode, pModifiers)) {
                    if (!Objects.equals(s, this.searchBox.getValue())) {
                        this.refreshSearchResults();
                    }

                    return true;
                } else {
                    return this.searchBox.isFocused() && this.searchBox.isVisible() && pKeyCode != 256 ? true : super.keyPressed(pKeyCode, pScanCode, pModifiers);
                }
            }
        }
    }

    @Override
    public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
        this.ignoreTextInput = false;
        return super.keyReleased(pKeyCode, pScanCode, pModifiers);
    }

    private void refreshSearchResults() {
        this.menu.items.clear();
        this.visibleTags.clear();
        String s = this.searchBox.getValue();
        if (s.isEmpty()) {
            this.menu.items.addAll(selectedTab.getDisplayItems());
        } else {
            ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
            if (clientpacketlistener != null) {
                SessionSearchTrees sessionsearchtrees = clientpacketlistener.searchTrees();
                SearchTree<ItemStack> searchtree;
                if (s.startsWith("#")) {
                    s = s.substring(1);
                    searchtree = sessionsearchtrees.creativeTagSearch();
                    this.updateVisibleTags(s);
                } else {
                    searchtree = sessionsearchtrees.creativeNameSearch();
                }

                this.menu.items.addAll(searchtree.search(s.toLowerCase(Locale.ROOT)));
            }
        }

        this.scrollOffs = 0.0F;
        this.menu.scrollTo(0.0F);
    }

    private void updateVisibleTags(String pSearch) {
        int i = pSearch.indexOf(58);
        Predicate<ResourceLocation> predicate;
        if (i == -1) {
            predicate = p_98609_ -> p_98609_.getPath().contains(pSearch);
        } else {
            String s = pSearch.substring(0, i).trim();
            String s1 = pSearch.substring(i + 1).trim();
            predicate = p_98606_ -> p_98606_.getNamespace().contains(s) && p_98606_.getPath().contains(s1);
        }

        BuiltInRegistries.ITEM
            .getTags()
            .map(HolderSet.Named::key)
            .filter(p_205410_ -> predicate.test(p_205410_.location()))
            .forEach(this.visibleTags::add);
    }

    @Override
    protected void renderLabels(GuiGraphics p_283168_, int p_281774_, int p_281466_) {
        if (selectedTab.showTitle()) {
            p_283168_.drawString(this.font, selectedTab.getDisplayName(), 8, 6, 4210752, false);
        }
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (pButton == 0) {
            double d0 = pMouseX - (double)this.leftPos;
            double d1 = pMouseY - (double)this.topPos;

            for (CreativeModeTab creativemodetab : CreativeModeTabs.tabs()) {
                if (this.checkTabClicked(creativemodetab, d0, d1)) {
                    return true;
                }
            }

            if (selectedTab.getType() != CreativeModeTab.Type.INVENTORY && this.insideScrollbar(pMouseX, pMouseY)) {
                this.scrolling = this.canScroll();
                return true;
            }
        }

        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        if (pButton == 0) {
            double d0 = pMouseX - (double)this.leftPos;
            double d1 = pMouseY - (double)this.topPos;
            this.scrolling = false;

            for (CreativeModeTab creativemodetab : CreativeModeTabs.tabs()) {
                if (this.checkTabClicked(creativemodetab, d0, d1)) {
                    this.selectTab(creativemodetab);
                    return true;
                }
            }
        }

        return super.mouseReleased(pMouseX, pMouseY, pButton);
    }

    private boolean canScroll() {
        return selectedTab.canScroll() && this.menu.canScroll();
    }

    private void selectTab(CreativeModeTab pTab) {
        CreativeModeTab creativemodetab = selectedTab;
        selectedTab = pTab;
        this.quickCraftSlots.clear();
        this.menu.items.clear();
        this.clearDraggingState();
        if (selectedTab.getType() == CreativeModeTab.Type.HOTBAR) {
            HotbarManager hotbarmanager = this.minecraft.getHotbarManager();

            for (int i = 0; i < 9; i++) {
                Hotbar hotbar = hotbarmanager.get(i);
                if (hotbar.isEmpty()) {
                    for (int j = 0; j < 9; j++) {
                        if (j == i) {
                            ItemStack itemstack = new ItemStack(Items.PAPER);
                            itemstack.set(DataComponents.CREATIVE_SLOT_LOCK, Unit.INSTANCE);
                            Component component = this.minecraft.options.keyHotbarSlots[i].getTranslatedKeyMessage();
                            Component component1 = this.minecraft.options.keySaveHotbarActivator.getTranslatedKeyMessage();
                            itemstack.set(DataComponents.ITEM_NAME, Component.translatable("inventory.hotbarInfo", component1, component));
                            this.menu.items.add(itemstack);
                        } else {
                            this.menu.items.add(ItemStack.EMPTY);
                        }
                    }
                } else {
                    this.menu.items.addAll(hotbar.load(this.minecraft.level.registryAccess()));
                }
            }
        } else if (selectedTab.getType() == CreativeModeTab.Type.CATEGORY) {
            this.menu.items.addAll(selectedTab.getDisplayItems());
        }

        if (selectedTab.getType() == CreativeModeTab.Type.INVENTORY) {
            AbstractContainerMenu abstractcontainermenu = this.minecraft.player.inventoryMenu;
            if (this.originalSlots == null) {
                this.originalSlots = ImmutableList.copyOf(this.menu.slots);
            }

            this.menu.slots.clear();

            for (int k = 0; k < abstractcontainermenu.slots.size(); k++) {
                int l;
                int i1;
                if (k >= 5 && k < 9) {
                    int k1 = k - 5;
                    int i2 = k1 / 2;
                    int k2 = k1 % 2;
                    l = 54 + i2 * 54;
                    i1 = 6 + k2 * 27;
                } else if (k >= 0 && k < 5) {
                    l = -2000;
                    i1 = -2000;
                } else if (k == 45) {
                    l = 35;
                    i1 = 20;
                } else {
                    int j1 = k - 9;
                    int l1 = j1 % 9;
                    int j2 = j1 / 9;
                    l = 9 + l1 * 18;
                    if (k >= 36) {
                        i1 = 112;
                    } else {
                        i1 = 54 + j2 * 18;
                    }
                }

                Slot slot = new CreativeModeInventoryScreen.SlotWrapper(abstractcontainermenu.slots.get(k), k, l, i1);
                this.menu.slots.add(slot);
            }

            this.destroyItemSlot = new Slot(CONTAINER, 0, 173, 112);
            this.menu.slots.add(this.destroyItemSlot);
        } else if (creativemodetab.getType() == CreativeModeTab.Type.INVENTORY) {
            this.menu.slots.clear();
            this.menu.slots.addAll(this.originalSlots);
            this.originalSlots = null;
        }

        if (selectedTab.getType() == CreativeModeTab.Type.SEARCH) {
            this.searchBox.setVisible(true);
            this.searchBox.setCanLoseFocus(false);
            this.searchBox.setFocused(true);
            if (creativemodetab != pTab) {
                this.searchBox.setValue("");
            }

            this.refreshSearchResults();
        } else {
            this.searchBox.setVisible(false);
            this.searchBox.setCanLoseFocus(true);
            this.searchBox.setFocused(false);
            this.searchBox.setValue("");
        }

        this.scrollOffs = 0.0F;
        this.menu.scrollTo(0.0F);
    }

    @Override
    public boolean mouseScrolled(double p_98527_, double p_98528_, double p_98529_, double p_301127_) {
        if (super.mouseScrolled(p_98527_, p_98528_, p_98529_, p_301127_)) {
            return true;
        } else if (!this.canScroll()) {
            return false;
        } else {
            this.scrollOffs = this.menu.subtractInputFromScroll(this.scrollOffs, p_301127_);
            this.menu.scrollTo(this.scrollOffs);
            return true;
        }
    }

    @Override
    protected boolean hasClickedOutside(double pMouseX, double pMouseY, int pGuiLeft, int pGuiTop, int pMouseButton) {
        boolean flag = pMouseX < (double)pGuiLeft
            || pMouseY < (double)pGuiTop
            || pMouseX >= (double)(pGuiLeft + this.imageWidth)
            || pMouseY >= (double)(pGuiTop + this.imageHeight);
        this.hasClickedOutside = flag && !this.checkTabClicked(selectedTab, pMouseX, pMouseY);
        return this.hasClickedOutside;
    }

    protected boolean insideScrollbar(double pMouseX, double pMouseY) {
        int i = this.leftPos;
        int j = this.topPos;
        int k = i + 175;
        int l = j + 18;
        int i1 = k + 14;
        int j1 = l + 112;
        return pMouseX >= (double)k && pMouseY >= (double)l && pMouseX < (double)i1 && pMouseY < (double)j1;
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        if (this.scrolling) {
            int i = this.topPos + 18;
            int j = i + 112;
            this.scrollOffs = ((float)pMouseY - (float)i - 7.5F) / ((float)(j - i) - 15.0F);
            this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
            this.menu.scrollTo(this.scrollOffs);
            return true;
        } else {
            return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
        }
    }

    @Override
    public void render(GuiGraphics p_283000_, int p_281317_, int p_282770_, float p_281295_) {
        super.render(p_283000_, p_281317_, p_282770_, p_281295_);
        this.effects.render(p_283000_, p_281317_, p_282770_, p_281295_);

        for (CreativeModeTab creativemodetab : CreativeModeTabs.tabs()) {
            if (this.checkTabHovering(p_283000_, creativemodetab, p_281317_, p_282770_)) {
                break;
            }
        }

        if (this.destroyItemSlot != null
            && selectedTab.getType() == CreativeModeTab.Type.INVENTORY
            && this.isHovering(this.destroyItemSlot.x, this.destroyItemSlot.y, 16, 16, (double)p_281317_, (double)p_282770_)) {
            p_283000_.renderTooltip(this.font, TRASH_SLOT_TOOLTIP, p_281317_, p_282770_);
        }

        this.renderTooltip(p_283000_, p_281317_, p_282770_);
    }

    @Override
    public boolean showsActiveEffects() {
        return this.effects.canSeeEffects();
    }

    @Override
    public List<Component> getTooltipFromContainerItem(ItemStack p_281769_) {
        boolean flag = this.hoveredSlot != null && this.hoveredSlot instanceof CreativeModeInventoryScreen.CustomCreativeSlot;
        boolean flag1 = selectedTab.getType() == CreativeModeTab.Type.CATEGORY;
        boolean flag2 = selectedTab.getType() == CreativeModeTab.Type.SEARCH;
        TooltipFlag.Default tooltipflag$default = this.minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
        TooltipFlag tooltipflag = flag ? tooltipflag$default.asCreative() : tooltipflag$default;
        List<Component> list = p_281769_.getTooltipLines(Item.TooltipContext.of(this.minecraft.level), this.minecraft.player, tooltipflag);
        if (flag1 && flag) {
            return list;
        } else {
            List<Component> list1 = Lists.newArrayList(list);
            if (flag2 && flag) {
                this.visibleTags.forEach(p_325383_ -> {
                    if (p_281769_.is((TagKey<Item>)p_325383_)) {
                        list1.add(1, Component.literal("#" + p_325383_.location()).withStyle(ChatFormatting.DARK_PURPLE));
                    }
                });
            }

            int i = 1;

            for (CreativeModeTab creativemodetab : CreativeModeTabs.tabs()) {
                if (creativemodetab.getType() != CreativeModeTab.Type.SEARCH && creativemodetab.contains(p_281769_)) {
                    list1.add(i++, creativemodetab.getDisplayName().copy().withStyle(ChatFormatting.BLUE));
                }
            }

            return list1;
        }
    }

    @Override
    protected void renderBg(GuiGraphics p_282663_, float p_282504_, int p_282089_, int p_282249_) {
        for (CreativeModeTab creativemodetab : CreativeModeTabs.tabs()) {
            if (creativemodetab != selectedTab) {
                this.renderTabButton(p_282663_, creativemodetab);
            }
        }

        p_282663_.blit(RenderType::guiTextured, selectedTab.getBackgroundTexture(), this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        this.searchBox.render(p_282663_, p_282089_, p_282249_, p_282504_);
        int j = this.leftPos + 175;
        int k = this.topPos + 18;
        int i = k + 112;
        if (selectedTab.canScroll()) {
            ResourceLocation resourcelocation = this.canScroll() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
            p_282663_.blitSprite(RenderType::guiTextured, resourcelocation, j, k + (int)((float)(i - k - 17) * this.scrollOffs), 12, 15);
        }

        this.renderTabButton(p_282663_, selectedTab);
        if (selectedTab.getType() == CreativeModeTab.Type.INVENTORY) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                p_282663_,
                this.leftPos + 73,
                this.topPos + 6,
                this.leftPos + 105,
                this.topPos + 49,
                20,
                0.0625F,
                (float)p_282089_,
                (float)p_282249_,
                this.minecraft.player
            );
        }
    }

    private int getTabX(CreativeModeTab pTab) {
        int i = pTab.column();
        int j = 27;
        int k = 27 * i;
        if (pTab.isAlignedRight()) {
            k = this.imageWidth - 27 * (7 - i) + 1;
        }

        return k;
    }

    private int getTabY(CreativeModeTab pTab) {
        int i = 0;
        if (pTab.row() == CreativeModeTab.Row.TOP) {
            i -= 32;
        } else {
            i += this.imageHeight;
        }

        return i;
    }

    protected boolean checkTabClicked(CreativeModeTab pCreativeModeTab, double pRelativeMouseX, double pRelativeMouseY) {
        int i = this.getTabX(pCreativeModeTab);
        int j = this.getTabY(pCreativeModeTab);
        return pRelativeMouseX >= (double)i && pRelativeMouseX <= (double)(i + 26) && pRelativeMouseY >= (double)j && pRelativeMouseY <= (double)(j + 32);
    }

    protected boolean checkTabHovering(GuiGraphics pGuiGraphics, CreativeModeTab pCreativeModeTab, int pMouseX, int pMouseY) {
        int i = this.getTabX(pCreativeModeTab);
        int j = this.getTabY(pCreativeModeTab);
        if (this.isHovering(i + 3, j + 3, 21, 27, (double)pMouseX, (double)pMouseY)) {
            pGuiGraphics.renderTooltip(this.font, pCreativeModeTab.getDisplayName(), pMouseX, pMouseY);
            return true;
        } else {
            return false;
        }
    }

    protected void renderTabButton(GuiGraphics pGuiGraphics, CreativeModeTab pCreativeModeTab) {
        boolean flag = pCreativeModeTab == selectedTab;
        boolean flag1 = pCreativeModeTab.row() == CreativeModeTab.Row.TOP;
        int i = pCreativeModeTab.column();
        int j = this.leftPos + this.getTabX(pCreativeModeTab);
        int k = this.topPos - (flag1 ? 28 : -(this.imageHeight - 4));
        ResourceLocation[] aresourcelocation;
        if (flag1) {
            aresourcelocation = flag ? SELECTED_TOP_TABS : UNSELECTED_TOP_TABS;
        } else {
            aresourcelocation = flag ? SELECTED_BOTTOM_TABS : UNSELECTED_BOTTOM_TABS;
        }

        pGuiGraphics.blitSprite(RenderType::guiTextured, aresourcelocation[Mth.clamp(i, 0, aresourcelocation.length)], j, k, 26, 32);
        pGuiGraphics.pose().pushPose();
        pGuiGraphics.pose().translate(0.0F, 0.0F, 100.0F);
        j += 5;
        k += 8 + (flag1 ? 1 : -1);
        ItemStack itemstack = pCreativeModeTab.getIconItem();
        pGuiGraphics.renderItem(itemstack, j, k);
        pGuiGraphics.renderItemDecorations(this.font, itemstack, j, k);
        pGuiGraphics.pose().popPose();
    }

    public boolean isInventoryOpen() {
        return selectedTab.getType() == CreativeModeTab.Type.INVENTORY;
    }

    public static void handleHotbarLoadOrSave(Minecraft pClient, int pIndex, boolean pLoad, boolean pSave) {
        LocalPlayer localplayer = pClient.player;
        RegistryAccess registryaccess = localplayer.level().registryAccess();
        HotbarManager hotbarmanager = pClient.getHotbarManager();
        Hotbar hotbar = hotbarmanager.get(pIndex);
        if (pLoad) {
            List<ItemStack> list = hotbar.load(registryaccess);

            for (int i = 0; i < Inventory.getSelectionSize(); i++) {
                ItemStack itemstack = list.get(i);
                localplayer.getInventory().setItem(i, itemstack);
                pClient.gameMode.handleCreativeModeItemAdd(itemstack, 36 + i);
            }

            localplayer.inventoryMenu.broadcastChanges();
        } else if (pSave) {
            hotbar.storeFrom(localplayer.getInventory(), registryaccess);
            Component component = pClient.options.keyHotbarSlots[pIndex].getTranslatedKeyMessage();
            Component component1 = pClient.options.keyLoadHotbarActivator.getTranslatedKeyMessage();
            Component component2 = Component.translatable("inventory.hotbarSaved", component1, component);
            pClient.gui.setOverlayMessage(component2, false);
            pClient.getNarrator().sayNow(component2);
            hotbarmanager.save();
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class CustomCreativeSlot extends Slot {
        public CustomCreativeSlot(Container p_98633_, int p_98634_, int p_98635_, int p_98636_) {
            super(p_98633_, p_98634_, p_98635_, p_98636_);
        }

        @Override
        public boolean mayPickup(Player pPlayer) {
            ItemStack itemstack = this.getItem();
            return super.mayPickup(pPlayer) && !itemstack.isEmpty()
                ? itemstack.isItemEnabled(pPlayer.level().enabledFeatures()) && !itemstack.has(DataComponents.CREATIVE_SLOT_LOCK)
                : itemstack.isEmpty();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ItemPickerMenu extends AbstractContainerMenu {
        public final NonNullList<ItemStack> items = NonNullList.create();
        private final AbstractContainerMenu inventoryMenu;

        public ItemPickerMenu(Player pPlayer) {
            super(null, 0);
            this.inventoryMenu = pPlayer.inventoryMenu;
            Inventory inventory = pPlayer.getInventory();

            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 9; j++) {
                    this.addSlot(new CreativeModeInventoryScreen.CustomCreativeSlot(CreativeModeInventoryScreen.CONTAINER, i * 9 + j, 9 + j * 18, 18 + i * 18));
                }
            }

            this.addInventoryHotbarSlots(inventory, 9, 112);
            this.scrollTo(0.0F);
        }

        @Override
        public boolean stillValid(Player pPlayer) {
            return true;
        }

        protected int calculateRowCount() {
            return Mth.positiveCeilDiv(this.items.size(), 9) - 5;
        }

        protected int getRowIndexForScroll(float pScrollOffs) {
            return Math.max((int)((double)(pScrollOffs * (float)this.calculateRowCount()) + 0.5), 0);
        }

        protected float getScrollForRowIndex(int pRowIndex) {
            return Mth.clamp((float)pRowIndex / (float)this.calculateRowCount(), 0.0F, 1.0F);
        }

        protected float subtractInputFromScroll(float pScrollOffs, double pInput) {
            return Mth.clamp(pScrollOffs - (float)(pInput / (double)this.calculateRowCount()), 0.0F, 1.0F);
        }

        public void scrollTo(float pPos) {
            int i = this.getRowIndexForScroll(pPos);

            for (int j = 0; j < 5; j++) {
                for (int k = 0; k < 9; k++) {
                    int l = k + (j + i) * 9;
                    if (l >= 0 && l < this.items.size()) {
                        CreativeModeInventoryScreen.CONTAINER.setItem(k + j * 9, this.items.get(l));
                    } else {
                        CreativeModeInventoryScreen.CONTAINER.setItem(k + j * 9, ItemStack.EMPTY);
                    }
                }
            }
        }

        public boolean canScroll() {
            return this.items.size() > 45;
        }

        @Override
        public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
            if (pIndex >= this.slots.size() - 9 && pIndex < this.slots.size()) {
                Slot slot = this.slots.get(pIndex);
                if (slot != null && slot.hasItem()) {
                    slot.setByPlayer(ItemStack.EMPTY);
                }
            }

            return ItemStack.EMPTY;
        }

        @Override
        public boolean canTakeItemForPickAll(ItemStack pStack, Slot pSlot) {
            return pSlot.container != CreativeModeInventoryScreen.CONTAINER;
        }

        @Override
        public boolean canDragTo(Slot pSlot) {
            return pSlot.container != CreativeModeInventoryScreen.CONTAINER;
        }

        @Override
        public ItemStack getCarried() {
            return this.inventoryMenu.getCarried();
        }

        @Override
        public void setCarried(ItemStack p_169751_) {
            this.inventoryMenu.setCarried(p_169751_);
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class SlotWrapper extends Slot {
        final Slot target;

        public SlotWrapper(Slot pSlot, int pIndex, int pX, int pY) {
            super(pSlot.container, pIndex, pX, pY);
            this.target = pSlot;
        }

        @Override
        public void onTake(Player p_169754_, ItemStack p_169755_) {
            this.target.onTake(p_169754_, p_169755_);
        }

        @Override
        public boolean mayPlace(ItemStack pStack) {
            return this.target.mayPlace(pStack);
        }

        @Override
        public ItemStack getItem() {
            return this.target.getItem();
        }

        @Override
        public boolean hasItem() {
            return this.target.hasItem();
        }

        @Override
        public void setByPlayer(ItemStack p_271008_, ItemStack p_299458_) {
            this.target.setByPlayer(p_271008_, p_299458_);
        }

        @Override
        public void set(ItemStack pStack) {
            this.target.set(pStack);
        }

        @Override
        public void setChanged() {
            this.target.setChanged();
        }

        @Override
        public int getMaxStackSize() {
            return this.target.getMaxStackSize();
        }

        @Override
        public int getMaxStackSize(ItemStack pStack) {
            return this.target.getMaxStackSize(pStack);
        }

        @Nullable
        @Override
        public ResourceLocation getNoItemIcon() {
            return this.target.getNoItemIcon();
        }

        @Override
        public ItemStack remove(int pAmount) {
            return this.target.remove(pAmount);
        }

        @Override
        public boolean isActive() {
            return this.target.isActive();
        }

        @Override
        public boolean mayPickup(Player pPlayer) {
            return this.target.mayPickup(pPlayer);
        }
    }
}