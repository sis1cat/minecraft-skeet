package net.minecraft.world.inventory;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class HorseInventoryMenu extends AbstractContainerMenu {
    static final ResourceLocation SADDLE_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot/saddle");
    private static final ResourceLocation LLAMA_ARMOR_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot/llama_armor");
    private static final ResourceLocation ARMOR_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot/horse_armor");
    private final Container horseContainer;
    private final Container armorContainer;
    private final AbstractHorse horse;
    private static final int SLOT_BODY_ARMOR = 1;
    private static final int SLOT_HORSE_INVENTORY_START = 2;

    public HorseInventoryMenu(int pContainerId, Inventory pInventory, Container pHorseContainer, final AbstractHorse pHorse, int pColumns) {
        super(null, pContainerId);
        this.horseContainer = pHorseContainer;
        this.armorContainer = pHorse.getBodyArmorAccess();
        this.horse = pHorse;
        pHorseContainer.startOpen(pInventory.player);
        this.addSlot(new Slot(pHorseContainer, 0, 8, 18) {
            @Override
            public boolean mayPlace(ItemStack p_39677_) {
                return p_39677_.is(Items.SADDLE) && !this.hasItem() && pHorse.isSaddleable();
            }

            @Override
            public boolean isActive() {
                return pHorse.isSaddleable();
            }

            @Override
            public ResourceLocation getNoItemIcon() {
                return HorseInventoryMenu.SADDLE_SLOT_SPRITE;
            }
        });
        ResourceLocation resourcelocation = pHorse instanceof Llama ? LLAMA_ARMOR_SLOT_SPRITE : ARMOR_SLOT_SPRITE;
        this.addSlot(new ArmorSlot(this.armorContainer, pHorse, EquipmentSlot.BODY, 0, 8, 36, resourcelocation) {
            @Override
            public boolean mayPlace(ItemStack p_39690_) {
                return pHorse.isEquippableInSlot(p_39690_, EquipmentSlot.BODY);
            }

            @Override
            public boolean isActive() {
                return pHorse.canUseSlot(EquipmentSlot.BODY);
            }
        });
        if (pColumns > 0) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < pColumns; j++) {
                    this.addSlot(new Slot(pHorseContainer, 1 + j + i * pColumns, 80 + j * 18, 18 + i * 18));
                }
            }
        }

        this.addStandardInventorySlots(pInventory, 8, 84);
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return !this.horse.hasInventoryChanged(this.horseContainer)
            && this.horseContainer.stillValid(pPlayer)
            && this.armorContainer.stillValid(pPlayer)
            && this.horse.isAlive()
            && pPlayer.canInteractWithEntity(this.horse, 4.0);
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            int i = this.horseContainer.getContainerSize() + 1;
            if (pIndex < i) {
                if (!this.moveItemStackTo(itemstack1, i, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.getSlot(1).mayPlace(itemstack1) && !this.getSlot(1).hasItem()) {
                if (!this.moveItemStackTo(itemstack1, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.getSlot(0).mayPlace(itemstack1)) {
                if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (i <= 1 || !this.moveItemStackTo(itemstack1, 2, i, false)) {
                int j = i + 27;
                int k = j + 9;
                if (pIndex >= j && pIndex < k) {
                    if (!this.moveItemStackTo(itemstack1, i, j, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (pIndex >= i && pIndex < j) {
                    if (!this.moveItemStackTo(itemstack1, j, k, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemstack1, j, j, false)) {
                    return ItemStack.EMPTY;
                }

                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public void removed(Player pPlayer) {
        super.removed(pPlayer);
        this.horseContainer.stopOpen(pPlayer);
    }
}