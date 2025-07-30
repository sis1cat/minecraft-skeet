package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public class CrafterSlot extends Slot {
    private final CrafterMenu menu;

    public CrafterSlot(Container pContainer, int pSlot, int pX, int pY, CrafterMenu pMenu) {
        super(pContainer, pSlot, pX, pY);
        this.menu = pMenu;
    }

    @Override
    public boolean mayPlace(ItemStack p_310494_) {
        return !this.menu.isSlotDisabled(this.index) && super.mayPlace(p_310494_);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        this.menu.slotsChanged(this.container);
    }
}