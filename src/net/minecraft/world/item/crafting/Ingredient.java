package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.ItemLike;

public final class Ingredient implements StackedContents.IngredientInfo<Holder<Item>>, Predicate<ItemStack> {
    public static final StreamCodec<RegistryFriendlyByteBuf, Ingredient> CONTENTS_STREAM_CODEC = ByteBufCodecs.holderSet(Registries.ITEM)
        .map(Ingredient::new, p_359816_ -> p_359816_.values);
    public static final StreamCodec<RegistryFriendlyByteBuf, Optional<Ingredient>> OPTIONAL_CONTENTS_STREAM_CODEC = ByteBufCodecs.holderSet(Registries.ITEM)
        .map(
            p_359814_ -> p_359814_.size() == 0 ? Optional.empty() : Optional.of(new Ingredient((HolderSet<Item>)p_359814_)),
            p_359815_ -> p_359815_.<HolderSet<Item>>map(p_359810_ -> p_359810_.values).orElse(HolderSet.direct())
        );
    public static final Codec<HolderSet<Item>> NON_AIR_HOLDER_SET_CODEC = HolderSetCodec.create(Registries.ITEM, Item.CODEC, false);
    public static final Codec<Ingredient> CODEC = ExtraCodecs.nonEmptyHolderSet(NON_AIR_HOLDER_SET_CODEC).xmap(Ingredient::new, p_359811_ -> p_359811_.values);
    private final HolderSet<Item> values;

    private Ingredient(HolderSet<Item> pValues) {
        pValues.unwrap().ifRight(p_359817_ -> {
            if (p_359817_.isEmpty()) {
                throw new UnsupportedOperationException("Ingredients can't be empty");
            } else if (p_359817_.contains(Items.AIR.builtInRegistryHolder())) {
                throw new UnsupportedOperationException("Ingredient can't contain air");
            }
        });
        this.values = pValues;
    }

    public static boolean testOptionalIngredient(Optional<Ingredient> pIngredient, ItemStack pStack) {
        return pIngredient.<Boolean>map(p_359819_ -> p_359819_.test(pStack)).orElseGet(pStack::isEmpty);
    }

    @Deprecated
    public Stream<Holder<Item>> items() {
        return this.values.stream();
    }

    public boolean isEmpty() {
        return this.values.size() == 0;
    }

    public boolean test(ItemStack pStack) {
        return pStack.is(this.values);
    }

    public boolean acceptsItem(Holder<Item> p_378483_) {
        return this.values.contains(p_378483_);
    }

    @Override
    public boolean equals(Object pOther) {
        return pOther instanceof Ingredient ingredient ? Objects.equals(this.values, ingredient.values) : false;
    }

    public static Ingredient of(ItemLike pItem) {
        return new Ingredient(HolderSet.direct(pItem.asItem().builtInRegistryHolder()));
    }

    public static Ingredient of(ItemLike... pItems) {
        return of(Arrays.stream(pItems));
    }

    public static Ingredient of(Stream<? extends ItemLike> pItems) {
        return new Ingredient(HolderSet.direct(pItems.map(p_359813_ -> p_359813_.asItem().builtInRegistryHolder()).toList()));
    }

    public static Ingredient of(HolderSet<Item> pItems) {
        return new Ingredient(pItems);
    }

    public SlotDisplay display() {
        return (SlotDisplay)this.values
            .unwrap()
            .map(SlotDisplay.TagSlotDisplay::new, p_359812_ -> new SlotDisplay.Composite(p_359812_.stream().map(Ingredient::displayForSingleItem).toList()));
    }

    public static SlotDisplay optionalIngredientToDisplay(Optional<Ingredient> pIngredient) {
        return pIngredient.map(Ingredient::display).orElse(SlotDisplay.Empty.INSTANCE);
    }

    private static SlotDisplay displayForSingleItem(Holder<Item> pItem) {
        SlotDisplay slotdisplay = new SlotDisplay.ItemSlotDisplay(pItem);
        ItemStack itemstack = pItem.value().getCraftingRemainder();
        if (!itemstack.isEmpty()) {
            SlotDisplay slotdisplay1 = new SlotDisplay.ItemStackSlotDisplay(itemstack);
            return new SlotDisplay.WithRemainder(slotdisplay, slotdisplay1);
        } else {
            return slotdisplay;
        }
    }
}