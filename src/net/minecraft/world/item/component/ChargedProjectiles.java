package net.minecraft.world.item.component;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ChargedProjectiles {
    public static final ChargedProjectiles EMPTY = new ChargedProjectiles(List.of());
    public static final Codec<ChargedProjectiles> CODEC = ItemStack.CODEC.listOf().xmap(ChargedProjectiles::new, p_333238_ -> p_333238_.items);
    public static final StreamCodec<RegistryFriendlyByteBuf, ChargedProjectiles> STREAM_CODEC = ItemStack.STREAM_CODEC
        .apply(ByteBufCodecs.list())
        .map(ChargedProjectiles::new, p_330449_ -> p_330449_.items);
    private final List<ItemStack> items;

    private ChargedProjectiles(List<ItemStack> pItems) {
        this.items = pItems;
    }

    public static ChargedProjectiles of(ItemStack pStack) {
        return new ChargedProjectiles(List.of(pStack.copy()));
    }

    public static ChargedProjectiles of(List<ItemStack> pStack) {
        return new ChargedProjectiles(List.copyOf(Lists.transform(pStack, ItemStack::copy)));
    }

    public boolean contains(Item pItem) {
        for (ItemStack itemstack : this.items) {
            if (itemstack.is(pItem)) {
                return true;
            }
        }

        return false;
    }

    public List<ItemStack> getItems() {
        return Lists.transform(this.items, ItemStack::copy);
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            if (pOther instanceof ChargedProjectiles chargedprojectiles && ItemStack.listMatches(this.items, chargedprojectiles.items)) {
                return true;
            }

            return false;
        }
    }

    @Override
    public int hashCode() {
        return ItemStack.hashStackList(this.items);
    }

    @Override
    public String toString() {
        return "ChargedProjectiles[items=" + this.items + "]";
    }
}