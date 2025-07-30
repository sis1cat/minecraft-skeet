package net.minecraft.world.inventory;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class BeaconMenu extends AbstractContainerMenu {
    private static final int PAYMENT_SLOT = 0;
    private static final int SLOT_COUNT = 1;
    private static final int DATA_COUNT = 3;
    private static final int INV_SLOT_START = 1;
    private static final int INV_SLOT_END = 28;
    private static final int USE_ROW_SLOT_START = 28;
    private static final int USE_ROW_SLOT_END = 37;
    private static final int NO_EFFECT = 0;
    private final Container beacon = new SimpleContainer(1) {
        @Override
        public boolean canPlaceItem(int p_39066_, ItemStack p_39067_) {
            return p_39067_.is(ItemTags.BEACON_PAYMENT_ITEMS);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    };
    private final BeaconMenu.PaymentSlot paymentSlot;
    private final ContainerLevelAccess access;
    private final ContainerData beaconData;

    public BeaconMenu(int pContainerId, Container pContainer) {
        this(pContainerId, pContainer, new SimpleContainerData(3), ContainerLevelAccess.NULL);
    }

    public BeaconMenu(int pContainerId, Container pContainer, ContainerData pBeaconData, ContainerLevelAccess pAccess) {
        super(MenuType.BEACON, pContainerId);
        checkContainerDataCount(pBeaconData, 3);
        this.beaconData = pBeaconData;
        this.access = pAccess;
        this.paymentSlot = new BeaconMenu.PaymentSlot(this.beacon, 0, 136, 110);
        this.addSlot(this.paymentSlot);
        this.addDataSlots(pBeaconData);
        this.addStandardInventorySlots(pContainer, 36, 137);
    }

    @Override
    public void removed(Player pPlayer) {
        super.removed(pPlayer);
        if (!pPlayer.level().isClientSide) {
            ItemStack itemstack = this.paymentSlot.remove(this.paymentSlot.getMaxStackSize());
            if (!itemstack.isEmpty()) {
                pPlayer.drop(itemstack, false);
            }
        }
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(this.access, pPlayer, Blocks.BEACON);
    }

    @Override
    public void setData(int pId, int pData) {
        super.setData(pId, pData);
        this.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (pIndex == 0) {
                if (!this.moveItemStackTo(itemstack1, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemstack1, itemstack);
            } else if (!this.paymentSlot.hasItem() && this.paymentSlot.mayPlace(itemstack1) && itemstack1.getCount() == 1) {
                if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (pIndex >= 1 && pIndex < 28) {
                if (!this.moveItemStackTo(itemstack1, 28, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (pIndex >= 28 && pIndex < 37) {
                if (!this.moveItemStackTo(itemstack1, 1, 28, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 1, 37, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(pPlayer, itemstack1);
        }

        return itemstack;
    }

    public int getLevels() {
        return this.beaconData.get(0);
    }

    public static int encodeEffect(@Nullable Holder<MobEffect> pEffect) {
        return pEffect == null ? 0 : BuiltInRegistries.MOB_EFFECT.asHolderIdMap().getId(pEffect) + 1;
    }

    @Nullable
    public static Holder<MobEffect> decodeEffect(int pEffectId) {
        return pEffectId == 0 ? null : BuiltInRegistries.MOB_EFFECT.asHolderIdMap().byId(pEffectId - 1);
    }

    @Nullable
    public Holder<MobEffect> getPrimaryEffect() {
        return decodeEffect(this.beaconData.get(1));
    }

    @Nullable
    public Holder<MobEffect> getSecondaryEffect() {
        return decodeEffect(this.beaconData.get(2));
    }

    public void updateEffects(Optional<Holder<MobEffect>> pPrimaryEffect, Optional<Holder<MobEffect>> pSecondaryEffect) {
        if (this.paymentSlot.hasItem()) {
            this.beaconData.set(1, encodeEffect(pPrimaryEffect.orElse(null)));
            this.beaconData.set(2, encodeEffect(pSecondaryEffect.orElse(null)));
            this.paymentSlot.remove(1);
            this.access.execute(Level::blockEntityChanged);
        }
    }

    public boolean hasPayment() {
        return !this.beacon.getItem(0).isEmpty();
    }

    static class PaymentSlot extends Slot {
        public PaymentSlot(Container p_39071_, int p_39072_, int p_39073_, int p_39074_) {
            super(p_39071_, p_39072_, p_39073_, p_39074_);
        }

        @Override
        public boolean mayPlace(ItemStack pStack) {
            return pStack.is(ItemTags.BEACON_PAYMENT_ITEMS);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}