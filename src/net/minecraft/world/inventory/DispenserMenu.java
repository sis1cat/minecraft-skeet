package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class DispenserMenu extends AbstractContainerMenu {
    private static final int SLOT_COUNT = 9;
    private static final int INV_SLOT_START = 9;
    private static final int INV_SLOT_END = 36;
    private static final int USE_ROW_SLOT_START = 36;
    private static final int USE_ROW_SLOT_END = 45;
    private final Container dispenser;

    public DispenserMenu(int pContainerId, Inventory pPlayerInventory) {
        this(pContainerId, pPlayerInventory, new SimpleContainer(9));
    }

    public DispenserMenu(int pContainerId, Inventory pPlayerInventory, Container pContainer) {
        super(MenuType.GENERIC_3x3, pContainerId);
        checkContainerSize(pContainer, 9);
        this.dispenser = pContainer;
        pContainer.startOpen(pPlayerInventory.player);
        this.add3x3GridSlots(pContainer, 62, 17);
        this.addStandardInventorySlots(pPlayerInventory, 8, 84);
    }

    protected void add3x3GridSlots(Container pContainer, int pX, int pY) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int k = j + i * 3;
                this.addSlot(new Slot(pContainer, k, pX + j * 18, pY + i * 18));
            }
        }
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return this.dispenser.stillValid(pPlayer);
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (pIndex < 9) {
                if (!this.moveItemStackTo(itemstack1, 9, 45, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, 9, false)) {
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

    @Override
    public void removed(Player pPlayer) {
        super.removed(pPlayer);
        this.dispenser.stopOpen(pPlayer);
    }
}