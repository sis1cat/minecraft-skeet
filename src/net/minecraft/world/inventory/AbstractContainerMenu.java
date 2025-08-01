package net.minecraft.world.inventory;

import com.google.common.base.Suppliers;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;

public abstract class AbstractContainerMenu {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int SLOT_CLICKED_OUTSIDE = -999;
    public static final int QUICKCRAFT_TYPE_CHARITABLE = 0;
    public static final int QUICKCRAFT_TYPE_GREEDY = 1;
    public static final int QUICKCRAFT_TYPE_CLONE = 2;
    public static final int QUICKCRAFT_HEADER_START = 0;
    public static final int QUICKCRAFT_HEADER_CONTINUE = 1;
    public static final int QUICKCRAFT_HEADER_END = 2;
    public static final int CARRIED_SLOT_SIZE = Integer.MAX_VALUE;
    public static final int SLOTS_PER_ROW = 9;
    public static final int SLOT_SIZE = 18;
    private final NonNullList<ItemStack> lastSlots = NonNullList.create();
    public final NonNullList<Slot> slots = NonNullList.create();
    private final List<DataSlot> dataSlots = Lists.newArrayList();
    private ItemStack carried = ItemStack.EMPTY;
    private final NonNullList<ItemStack> remoteSlots = NonNullList.create();
    private final IntList remoteDataSlots = new IntArrayList();
    private ItemStack remoteCarried = ItemStack.EMPTY;
    private int stateId;
    @Nullable
    private final MenuType<?> menuType;
    public final int containerId;
    private int quickcraftType = -1;
    private int quickcraftStatus;
    private final Set<Slot> quickcraftSlots = Sets.newHashSet();
    private final List<ContainerListener> containerListeners = Lists.newArrayList();
    @Nullable
    private ContainerSynchronizer synchronizer;
    private boolean suppressRemoteUpdates;

    protected AbstractContainerMenu(@Nullable MenuType<?> pMenuType, int pContainerId) {
        this.menuType = pMenuType;
        this.containerId = pContainerId;
    }

    protected void addInventoryHotbarSlots(Container pContainer, int pX, int pY) {
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(pContainer, i, pX + i * 18, pY));
        }
    }

    protected void addInventoryExtendedSlots(Container pContainer, int pX, int pY) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(pContainer, j + (i + 1) * 9, pX + j * 18, pY + i * 18));
            }
        }
    }

    protected void addStandardInventorySlots(Container pContainer, int pX, int pY) {
        this.addInventoryExtendedSlots(pContainer, pX, pY);
        int i = 4;
        int j = 58;
        this.addInventoryHotbarSlots(pContainer, pX, pY + 58);
    }

    protected static boolean stillValid(ContainerLevelAccess pAccess, Player pPlayer, Block pTargetBlock) {
        return pAccess.evaluate((p_327069_, p_327070_) -> !p_327069_.getBlockState(p_327070_).is(pTargetBlock) ? false : pPlayer.canInteractWithBlock(p_327070_, 4.0), true);
    }

    public MenuType<?> getType() {
        if (this.menuType == null) {
            throw new UnsupportedOperationException("Unable to construct this menu by type");
        } else {
            return this.menuType;
        }
    }

    protected static void checkContainerSize(Container pContainer, int pMinSize) {
        int i = pContainer.getContainerSize();
        if (i < pMinSize) {
            throw new IllegalArgumentException("Container size " + i + " is smaller than expected " + pMinSize);
        }
    }

    protected static void checkContainerDataCount(ContainerData pIntArray, int pMinSize) {
        int i = pIntArray.getCount();
        if (i < pMinSize) {
            throw new IllegalArgumentException("Container data count " + i + " is smaller than expected " + pMinSize);
        }
    }

    public boolean isValidSlotIndex(int pSlotIndex) {
        return pSlotIndex == -1 || pSlotIndex == -999 || pSlotIndex < this.slots.size();
    }

    protected Slot addSlot(Slot pSlot) {
        pSlot.index = this.slots.size();
        this.slots.add(pSlot);
        this.lastSlots.add(ItemStack.EMPTY);
        this.remoteSlots.add(ItemStack.EMPTY);
        return pSlot;
    }

    protected DataSlot addDataSlot(DataSlot pIntValue) {
        this.dataSlots.add(pIntValue);
        this.remoteDataSlots.add(0);
        return pIntValue;
    }

    protected void addDataSlots(ContainerData pArray) {
        for (int i = 0; i < pArray.getCount(); i++) {
            this.addDataSlot(DataSlot.forContainer(pArray, i));
        }
    }

    public void addSlotListener(ContainerListener pListener) {
        if (!this.containerListeners.contains(pListener)) {
            this.containerListeners.add(pListener);
            this.broadcastChanges();
        }
    }

    public void setSynchronizer(ContainerSynchronizer pSynchronizer) {
        this.synchronizer = pSynchronizer;
        this.sendAllDataToRemote();
    }

    public void sendAllDataToRemote() {
        int i = 0;

        for (int j = this.slots.size(); i < j; i++) {
            this.remoteSlots.set(i, this.slots.get(i).getItem().copy());
        }

        this.remoteCarried = this.getCarried().copy();
        i = 0;

        for (int k = this.dataSlots.size(); i < k; i++) {
            this.remoteDataSlots.set(i, this.dataSlots.get(i).get());
        }

        if (this.synchronizer != null) {
            this.synchronizer.sendInitialData(this, this.remoteSlots, this.remoteCarried, this.remoteDataSlots.toIntArray());
        }
    }

    public void removeSlotListener(ContainerListener pListener) {
        this.containerListeners.remove(pListener);
    }

    public NonNullList<ItemStack> getItems() {
        NonNullList<ItemStack> nonnulllist = NonNullList.create();

        for (Slot slot : this.slots) {
            nonnulllist.add(slot.getItem());
        }

        return nonnulllist;
    }

    public void broadcastChanges() {
        for (int i = 0; i < this.slots.size(); i++) {
            ItemStack itemstack = this.slots.get(i).getItem();
            Supplier<ItemStack> supplier = Suppliers.memoize(itemstack::copy);
            this.triggerSlotListeners(i, itemstack, supplier);
            this.synchronizeSlotToRemote(i, itemstack, supplier);
        }

        this.synchronizeCarriedToRemote();

        for (int j = 0; j < this.dataSlots.size(); j++) {
            DataSlot dataslot = this.dataSlots.get(j);
            int k = dataslot.get();
            if (dataslot.checkAndClearUpdateFlag()) {
                this.updateDataSlotListeners(j, k);
            }

            this.synchronizeDataSlotToRemote(j, k);
        }
    }

    public void broadcastFullState() {
        for (int i = 0; i < this.slots.size(); i++) {
            ItemStack itemstack = this.slots.get(i).getItem();
            this.triggerSlotListeners(i, itemstack, itemstack::copy);
        }

        for (int j = 0; j < this.dataSlots.size(); j++) {
            DataSlot dataslot = this.dataSlots.get(j);
            if (dataslot.checkAndClearUpdateFlag()) {
                this.updateDataSlotListeners(j, dataslot.get());
            }
        }

        this.sendAllDataToRemote();
    }

    private void updateDataSlotListeners(int pSlotIndex, int pValue) {
        for (ContainerListener containerlistener : this.containerListeners) {
            containerlistener.dataChanged(this, pSlotIndex, pValue);
        }
    }

    private void triggerSlotListeners(int pSlotIndex, ItemStack pStack, Supplier<ItemStack> pSupplier) {
        ItemStack itemstack = this.lastSlots.get(pSlotIndex);
        if (!ItemStack.matches(itemstack, pStack)) {
            ItemStack itemstack1 = pSupplier.get();
            this.lastSlots.set(pSlotIndex, itemstack1);

            for (ContainerListener containerlistener : this.containerListeners) {
                containerlistener.slotChanged(this, pSlotIndex, itemstack1);
            }
        }
    }

    private void synchronizeSlotToRemote(int pSlotIndex, ItemStack pStack, Supplier<ItemStack> pSupplier) {
        if (!this.suppressRemoteUpdates) {
            ItemStack itemstack = this.remoteSlots.get(pSlotIndex);
            if (!ItemStack.matches(itemstack, pStack)) {
                ItemStack itemstack1 = pSupplier.get();
                this.remoteSlots.set(pSlotIndex, itemstack1);
                if (this.synchronizer != null) {
                    this.synchronizer.sendSlotChange(this, pSlotIndex, itemstack1);
                }
            }
        }
    }

    private void synchronizeDataSlotToRemote(int pSlotIndex, int pValue) {
        if (!this.suppressRemoteUpdates) {
            int i = this.remoteDataSlots.getInt(pSlotIndex);
            if (i != pValue) {
                this.remoteDataSlots.set(pSlotIndex, pValue);
                if (this.synchronizer != null) {
                    this.synchronizer.sendDataChange(this, pSlotIndex, pValue);
                }
            }
        }
    }

    private void synchronizeCarriedToRemote() {
        if (!this.suppressRemoteUpdates) {
            if (!ItemStack.matches(this.getCarried(), this.remoteCarried)) {
                this.remoteCarried = this.getCarried().copy();
                if (this.synchronizer != null) {
                    this.synchronizer.sendCarriedChange(this, this.remoteCarried);
                }
            }
        }
    }

    public void setRemoteSlot(int pSlot, ItemStack pStack) {
        this.remoteSlots.set(pSlot, pStack.copy());
    }

    public void setRemoteSlotNoCopy(int pSlot, ItemStack pStack) {
        if (pSlot >= 0 && pSlot < this.remoteSlots.size()) {
            this.remoteSlots.set(pSlot, pStack);
        } else {
            LOGGER.debug("Incorrect slot index: {} available slots: {}", pSlot, this.remoteSlots.size());
        }
    }

    public void setRemoteCarried(ItemStack pRemoteCarried) {
        this.remoteCarried = pRemoteCarried.copy();
    }

    public boolean clickMenuButton(Player pPlayer, int pId) {
        return false;
    }

    public Slot getSlot(int pSlotId) {
        return this.slots.get(pSlotId);
    }

    public abstract ItemStack quickMoveStack(Player pPlayer, int pIndex);

    public void setSelectedBundleItemIndex(int pSlotIndex, int pBundleItemIndex) {
        if (pSlotIndex >= 0 && pSlotIndex < this.slots.size()) {
            ItemStack itemstack = this.slots.get(pSlotIndex).getItem();
            BundleItem.toggleSelectedItem(itemstack, pBundleItemIndex);
        }
    }

    public void clicked(int pSlotId, int pButton, ClickType pClickType, Player pPlayer) {
        try {
            this.doClick(pSlotId, pButton, pClickType, pPlayer);
        } catch (Exception exception) {
            CrashReport crashreport = CrashReport.forThrowable(exception, "Container click");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Click info");
            crashreportcategory.setDetail(
                "Menu Type", () -> this.menuType != null ? BuiltInRegistries.MENU.getKey(this.menuType).toString() : "<no type>"
            );
            crashreportcategory.setDetail("Menu Class", () -> this.getClass().getCanonicalName());
            crashreportcategory.setDetail("Slot Count", this.slots.size());
            crashreportcategory.setDetail("Slot", pSlotId);
            crashreportcategory.setDetail("Button", pButton);
            crashreportcategory.setDetail("Type", pClickType);
            throw new ReportedException(crashreport);
        }
    }

    private void doClick(int pSlotId, int pButton, ClickType pClickType, Player pPlayer) {
        Inventory inventory = pPlayer.getInventory();
        if (pClickType == ClickType.QUICK_CRAFT) {
            int i = this.quickcraftStatus;
            this.quickcraftStatus = getQuickcraftHeader(pButton);
            if ((i != 1 || this.quickcraftStatus != 2) && i != this.quickcraftStatus) {
                this.resetQuickCraft();
            } else if (this.getCarried().isEmpty()) {
                this.resetQuickCraft();
            } else if (this.quickcraftStatus == 0) {
                this.quickcraftType = getQuickcraftType(pButton);
                if (isValidQuickcraftType(this.quickcraftType, pPlayer)) {
                    this.quickcraftStatus = 1;
                    this.quickcraftSlots.clear();
                } else {
                    this.resetQuickCraft();
                }
            } else if (this.quickcraftStatus == 1) {
                Slot slot = this.slots.get(pSlotId);
                ItemStack itemstack = this.getCarried();
                if (canItemQuickReplace(slot, itemstack, true)
                    && slot.mayPlace(itemstack)
                    && (this.quickcraftType == 2 || itemstack.getCount() > this.quickcraftSlots.size())
                    && this.canDragTo(slot)) {
                    this.quickcraftSlots.add(slot);
                }
            } else if (this.quickcraftStatus == 2) {
                if (!this.quickcraftSlots.isEmpty()) {
                    if (this.quickcraftSlots.size() == 1) {
                        int i1 = this.quickcraftSlots.iterator().next().index;
                        this.resetQuickCraft();
                        this.doClick(i1, this.quickcraftType, ClickType.PICKUP, pPlayer);
                        return;
                    }

                    ItemStack itemstack3 = this.getCarried().copy();
                    if (itemstack3.isEmpty()) {
                        this.resetQuickCraft();
                        return;
                    }

                    int k1 = this.getCarried().getCount();

                    for (Slot slot1 : this.quickcraftSlots) {
                        ItemStack itemstack1 = this.getCarried();
                        if (slot1 != null
                            && canItemQuickReplace(slot1, itemstack1, true)
                            && slot1.mayPlace(itemstack1)
                            && (this.quickcraftType == 2 || itemstack1.getCount() >= this.quickcraftSlots.size())
                            && this.canDragTo(slot1)) {
                            int j = slot1.hasItem() ? slot1.getItem().getCount() : 0;
                            int k = Math.min(itemstack3.getMaxStackSize(), slot1.getMaxStackSize(itemstack3));
                            int l = Math.min(getQuickCraftPlaceCount(this.quickcraftSlots, this.quickcraftType, itemstack3) + j, k);
                            k1 -= l - j;
                            slot1.setByPlayer(itemstack3.copyWithCount(l));
                        }
                    }

                    itemstack3.setCount(k1);
                    this.setCarried(itemstack3);
                }

                this.resetQuickCraft();
            } else {
                this.resetQuickCraft();
            }
        } else if (this.quickcraftStatus != 0) {
            this.resetQuickCraft();
        } else if ((pClickType == ClickType.PICKUP || pClickType == ClickType.QUICK_MOVE) && (pButton == 0 || pButton == 1)) {
            ClickAction clickaction = pButton == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY;
            if (pSlotId == -999) {
                if (!this.getCarried().isEmpty()) {
                    if (clickaction == ClickAction.PRIMARY) {
                        pPlayer.drop(this.getCarried(), true);
                        this.setCarried(ItemStack.EMPTY);
                    } else {
                        pPlayer.drop(this.getCarried().split(1), true);
                    }
                }
            } else if (pClickType == ClickType.QUICK_MOVE) {
                if (pSlotId < 0) {
                    return;
                }

                Slot slot6 = this.slots.get(pSlotId);
                if (!slot6.mayPickup(pPlayer)) {
                    return;
                }

                ItemStack itemstack8 = this.quickMoveStack(pPlayer, pSlotId);

                while (!itemstack8.isEmpty() && ItemStack.isSameItem(slot6.getItem(), itemstack8)) {
                    itemstack8 = this.quickMoveStack(pPlayer, pSlotId);
                }
            } else {
                if (pSlotId < 0) {
                    return;
                }

                Slot slot7 = this.slots.get(pSlotId);
                ItemStack itemstack9 = slot7.getItem();
                ItemStack itemstack10 = this.getCarried();
                pPlayer.updateTutorialInventoryAction(itemstack10, slot7.getItem(), clickaction);
                if (!this.tryItemClickBehaviourOverride(pPlayer, clickaction, slot7, itemstack9, itemstack10)) {
                    if (itemstack9.isEmpty()) {
                        if (!itemstack10.isEmpty()) {
                            int i3 = clickaction == ClickAction.PRIMARY ? itemstack10.getCount() : 1;
                            this.setCarried(slot7.safeInsert(itemstack10, i3));
                        }
                    } else if (slot7.mayPickup(pPlayer)) {
                        if (itemstack10.isEmpty()) {
                            int j3 = clickaction == ClickAction.PRIMARY ? itemstack9.getCount() : (itemstack9.getCount() + 1) / 2;
                            Optional<ItemStack> optional1 = slot7.tryRemove(j3, Integer.MAX_VALUE, pPlayer);
                            optional1.ifPresent(p_150421_ -> {
                                this.setCarried(p_150421_);
                                slot7.onTake(pPlayer, p_150421_);
                            });
                        } else if (slot7.mayPlace(itemstack10)) {
                            if (ItemStack.isSameItemSameComponents(itemstack9, itemstack10)) {
                                int k3 = clickaction == ClickAction.PRIMARY ? itemstack10.getCount() : 1;
                                this.setCarried(slot7.safeInsert(itemstack10, k3));
                            } else if (itemstack10.getCount() <= slot7.getMaxStackSize(itemstack10)) {
                                this.setCarried(itemstack9);
                                slot7.setByPlayer(itemstack10);
                            }
                        } else if (ItemStack.isSameItemSameComponents(itemstack9, itemstack10)) {
                            Optional<ItemStack> optional = slot7.tryRemove(itemstack9.getCount(), itemstack10.getMaxStackSize() - itemstack10.getCount(), pPlayer);
                            optional.ifPresent(p_150428_ -> {
                                itemstack10.grow(p_150428_.getCount());
                                slot7.onTake(pPlayer, p_150428_);
                            });
                        }
                    }
                }

                slot7.setChanged();
            }
        } else if (pClickType == ClickType.SWAP && (pButton >= 0 && pButton < 9 || pButton == 40)) {
            ItemStack itemstack2 = inventory.getItem(pButton);
            Slot slot5 = this.slots.get(pSlotId);
            ItemStack itemstack7 = slot5.getItem();
            if (!itemstack2.isEmpty() || !itemstack7.isEmpty()) {
                if (itemstack2.isEmpty()) {
                    if (slot5.mayPickup(pPlayer)) {
                        inventory.setItem(pButton, itemstack7);
                        slot5.onSwapCraft(itemstack7.getCount());
                        slot5.setByPlayer(ItemStack.EMPTY);
                        slot5.onTake(pPlayer, itemstack7);
                    }
                } else if (itemstack7.isEmpty()) {
                    if (slot5.mayPlace(itemstack2)) {
                        int j2 = slot5.getMaxStackSize(itemstack2);
                        if (itemstack2.getCount() > j2) {
                            slot5.setByPlayer(itemstack2.split(j2));
                        } else {
                            inventory.setItem(pButton, ItemStack.EMPTY);
                            slot5.setByPlayer(itemstack2);
                        }
                    }
                } else if (slot5.mayPickup(pPlayer) && slot5.mayPlace(itemstack2)) {
                    int k2 = slot5.getMaxStackSize(itemstack2);
                    if (itemstack2.getCount() > k2) {
                        slot5.setByPlayer(itemstack2.split(k2));
                        slot5.onTake(pPlayer, itemstack7);
                        if (!inventory.add(itemstack7)) {
                            pPlayer.drop(itemstack7, true);
                        }
                    } else {
                        inventory.setItem(pButton, itemstack7);
                        slot5.setByPlayer(itemstack2);
                        slot5.onTake(pPlayer, itemstack7);
                    }
                }
            }
        } else if (pClickType == ClickType.CLONE && pPlayer.hasInfiniteMaterials() && this.getCarried().isEmpty() && pSlotId >= 0) {
            Slot slot4 = this.slots.get(pSlotId);
            if (slot4.hasItem()) {
                ItemStack itemstack5 = slot4.getItem();
                this.setCarried(itemstack5.copyWithCount(itemstack5.getMaxStackSize()));
            }
        } else if (pClickType == ClickType.THROW && this.getCarried().isEmpty() && pSlotId >= 0) {
            Slot slot3 = this.slots.get(pSlotId);
            int j1 = pButton == 0 ? 1 : slot3.getItem().getCount();
            if (!pPlayer.canDropItems()) {
                return;
            }

            ItemStack itemstack6 = slot3.safeTake(j1, Integer.MAX_VALUE, pPlayer);
            pPlayer.drop(itemstack6, true);
            pPlayer.handleCreativeModeItemDrop(itemstack6);
            if (pButton == 1) {
                while (!itemstack6.isEmpty() && ItemStack.isSameItem(slot3.getItem(), itemstack6)) {
                    if (!pPlayer.canDropItems()) {
                        return;
                    }

                    itemstack6 = slot3.safeTake(j1, Integer.MAX_VALUE, pPlayer);
                    pPlayer.drop(itemstack6, true);
                    pPlayer.handleCreativeModeItemDrop(itemstack6);
                }
            }
        } else if (pClickType == ClickType.PICKUP_ALL && pSlotId >= 0) {
            Slot slot2 = this.slots.get(pSlotId);
            ItemStack itemstack4 = this.getCarried();
            if (!itemstack4.isEmpty() && (!slot2.hasItem() || !slot2.mayPickup(pPlayer))) {
                int l1 = pButton == 0 ? 0 : this.slots.size() - 1;
                int i2 = pButton == 0 ? 1 : -1;

                for (int l2 = 0; l2 < 2; l2++) {
                    for (int l3 = l1; l3 >= 0 && l3 < this.slots.size() && itemstack4.getCount() < itemstack4.getMaxStackSize(); l3 += i2) {
                        Slot slot8 = this.slots.get(l3);
                        if (slot8.hasItem() && canItemQuickReplace(slot8, itemstack4, true) && slot8.mayPickup(pPlayer) && this.canTakeItemForPickAll(itemstack4, slot8)) {
                            ItemStack itemstack11 = slot8.getItem();
                            if (l2 != 0 || itemstack11.getCount() != itemstack11.getMaxStackSize()) {
                                ItemStack itemstack12 = slot8.safeTake(itemstack11.getCount(), itemstack4.getMaxStackSize() - itemstack4.getCount(), pPlayer);
                                itemstack4.grow(itemstack12.getCount());
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean tryItemClickBehaviourOverride(Player pPlayer, ClickAction pAction, Slot pSlot, ItemStack pClickedItem, ItemStack pCarriedItem) {
        FeatureFlagSet featureflagset = pPlayer.level().enabledFeatures();
        return pCarriedItem.isItemEnabled(featureflagset) && pCarriedItem.overrideStackedOnOther(pSlot, pAction, pPlayer)
            ? true
            : pClickedItem.isItemEnabled(featureflagset) && pClickedItem.overrideOtherStackedOnMe(pCarriedItem, pSlot, pAction, pPlayer, this.createCarriedSlotAccess());
    }

    private SlotAccess createCarriedSlotAccess() {
        return new SlotAccess() {
            @Override
            public ItemStack get() {
                return AbstractContainerMenu.this.getCarried();
            }

            @Override
            public boolean set(ItemStack p_150452_) {
                AbstractContainerMenu.this.setCarried(p_150452_);
                return true;
            }
        };
    }

    public boolean canTakeItemForPickAll(ItemStack pStack, Slot pSlot) {
        return true;
    }

    public void removed(Player pPlayer) {
        if (pPlayer instanceof ServerPlayer) {
            ItemStack itemstack = this.getCarried();
            if (!itemstack.isEmpty()) {
                dropOrPlaceInInventory(pPlayer, itemstack);
                this.setCarried(ItemStack.EMPTY);
            }
        }
    }

    private static void dropOrPlaceInInventory(Player pPlayer, ItemStack pStack) {
        boolean flag;
        boolean flag2;
        label27: {
            flag = pPlayer.isRemoved() && pPlayer.getRemovalReason() != Entity.RemovalReason.CHANGED_DIMENSION;
            if (pPlayer instanceof ServerPlayer serverplayer && serverplayer.hasDisconnected()) {
                flag2 = true;
                break label27;
            }

            flag2 = false;
        }

        boolean flag1 = flag2;
        if (flag || flag1) {
            pPlayer.drop(pStack, false);
        } else if (pPlayer instanceof ServerPlayer) {
            pPlayer.getInventory().placeItemBackInInventory(pStack);
        }
    }

    protected void clearContainer(Player pPlayer, Container pContainer) {
        for (int i = 0; i < pContainer.getContainerSize(); i++) {
            dropOrPlaceInInventory(pPlayer, pContainer.removeItemNoUpdate(i));
        }
    }

    public void slotsChanged(Container pContainer) {
        this.broadcastChanges();
    }

    public void setItem(int pSlotId, int pStateId, ItemStack pStack) {
        this.getSlot(pSlotId).set(pStack);
        this.stateId = pStateId;
    }

    public void initializeContents(int pStateId, List<ItemStack> pItems, ItemStack pCarried) {
        for (int i = 0; i < pItems.size(); i++) {
            this.getSlot(i).set(pItems.get(i));
        }

        this.carried = pCarried;
        this.stateId = pStateId;
    }

    public void setData(int pId, int pData) {
        this.dataSlots.get(pId).set(pData);
    }

    public abstract boolean stillValid(Player pPlayer);

    protected boolean moveItemStackTo(ItemStack pStack, int pStartIndex, int pEndIndex, boolean pReverseDirection) {
        boolean flag = false;
        int i = pStartIndex;
        if (pReverseDirection) {
            i = pEndIndex - 1;
        }

        if (pStack.isStackable()) {
            while (!pStack.isEmpty() && (pReverseDirection ? i >= pStartIndex : i < pEndIndex)) {
                Slot slot = this.slots.get(i);
                ItemStack itemstack = slot.getItem();
                if (!itemstack.isEmpty() && ItemStack.isSameItemSameComponents(pStack, itemstack)) {
                    int j = itemstack.getCount() + pStack.getCount();
                    int k = slot.getMaxStackSize(itemstack);
                    if (j <= k) {
                        pStack.setCount(0);
                        itemstack.setCount(j);
                        slot.setChanged();
                        flag = true;
                    } else if (itemstack.getCount() < k) {
                        pStack.shrink(k - itemstack.getCount());
                        itemstack.setCount(k);
                        slot.setChanged();
                        flag = true;
                    }
                }

                if (pReverseDirection) {
                    i--;
                } else {
                    i++;
                }
            }
        }

        if (!pStack.isEmpty()) {
            if (pReverseDirection) {
                i = pEndIndex - 1;
            } else {
                i = pStartIndex;
            }

            while (pReverseDirection ? i >= pStartIndex : i < pEndIndex) {
                Slot slot1 = this.slots.get(i);
                ItemStack itemstack1 = slot1.getItem();
                if (itemstack1.isEmpty() && slot1.mayPlace(pStack)) {
                    int l = slot1.getMaxStackSize(pStack);
                    slot1.setByPlayer(pStack.split(Math.min(pStack.getCount(), l)));
                    slot1.setChanged();
                    flag = true;
                    break;
                }

                if (pReverseDirection) {
                    i--;
                } else {
                    i++;
                }
            }
        }

        return flag;
    }

    public static int getQuickcraftType(int pEventButton) {
        return pEventButton >> 2 & 3;
    }

    public static int getQuickcraftHeader(int pClickedButton) {
        return pClickedButton & 3;
    }

    public static int getQuickcraftMask(int pQuickCraftingHeader, int pQuickCraftingType) {
        return pQuickCraftingHeader & 3 | (pQuickCraftingType & 3) << 2;
    }

    public static boolean isValidQuickcraftType(int pDragMode, Player pPlayer) {
        if (pDragMode == 0) {
            return true;
        } else {
            return pDragMode == 1 ? true : pDragMode == 2 && pPlayer.hasInfiniteMaterials();
        }
    }

    protected void resetQuickCraft() {
        this.quickcraftStatus = 0;
        this.quickcraftSlots.clear();
    }

    public static boolean canItemQuickReplace(@Nullable Slot pSlot, ItemStack pStack, boolean pStackSizeMatters) {
        boolean flag = pSlot == null || !pSlot.hasItem();
        return !flag && ItemStack.isSameItemSameComponents(pStack, pSlot.getItem())
            ? pSlot.getItem().getCount() + (pStackSizeMatters ? 0 : pStack.getCount()) <= pStack.getMaxStackSize()
            : flag;
    }

    public static int getQuickCraftPlaceCount(Set<Slot> pSlots, int pType, ItemStack pStack) {
        return switch (pType) {
            case 0 -> Mth.floor((float)pStack.getCount() / (float)pSlots.size());
            case 1 -> 1;
            case 2 -> pStack.getMaxStackSize();
            default -> pStack.getCount();
        };
    }

    public boolean canDragTo(Slot pSlot) {
        return true;
    }

    public static int getRedstoneSignalFromBlockEntity(@Nullable BlockEntity pBlockEntity) {
        return pBlockEntity instanceof Container ? getRedstoneSignalFromContainer((Container)pBlockEntity) : 0;
    }

    public static int getRedstoneSignalFromContainer(@Nullable Container pContainer) {
        if (pContainer == null) {
            return 0;
        } else {
            float f = 0.0F;

            for (int i = 0; i < pContainer.getContainerSize(); i++) {
                ItemStack itemstack = pContainer.getItem(i);
                if (!itemstack.isEmpty()) {
                    f += (float)itemstack.getCount() / (float)pContainer.getMaxStackSize(itemstack);
                }
            }

            f /= (float)pContainer.getContainerSize();
            return Mth.lerpDiscrete(f, 0, 15);
        }
    }

    public void setCarried(ItemStack pStack) {
        this.carried = pStack;
    }

    public ItemStack getCarried() {
        return this.carried;
    }

    public void suppressRemoteUpdates() {
        this.suppressRemoteUpdates = true;
    }

    public void resumeRemoteUpdates() {
        this.suppressRemoteUpdates = false;
    }

    public void transferState(AbstractContainerMenu pMenu) {
        Table<Container, Integer, Integer> table = HashBasedTable.create();

        for (int i = 0; i < pMenu.slots.size(); i++) {
            Slot slot = pMenu.slots.get(i);
            table.put(slot.container, slot.getContainerSlot(), i);
        }

        for (int j = 0; j < this.slots.size(); j++) {
            Slot slot1 = this.slots.get(j);
            Integer integer = table.get(slot1.container, slot1.getContainerSlot());
            if (integer != null) {
                this.lastSlots.set(j, pMenu.lastSlots.get(integer));
                this.remoteSlots.set(j, pMenu.remoteSlots.get(integer));
            }
        }
    }

    public OptionalInt findSlot(Container pContainer, int pSlotIndex) {
        for (int i = 0; i < this.slots.size(); i++) {
            Slot slot = this.slots.get(i);
            if (slot.container == pContainer && pSlotIndex == slot.getContainerSlot()) {
                return OptionalInt.of(i);
            }
        }

        return OptionalInt.empty();
    }

    public int getStateId() {
        return this.stateId;
    }

    public int incrementStateId() {
        this.stateId = this.stateId + 1 & 32767;
        return this.stateId;
    }
}