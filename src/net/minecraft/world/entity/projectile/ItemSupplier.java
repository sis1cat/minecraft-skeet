package net.minecraft.world.entity.projectile;

import net.minecraft.world.item.ItemStack;

public interface ItemSupplier {
    ItemStack getItem();

    static boolean isInGround(AbstractArrow arrow) {
        return arrow.isInGround();
    }
}