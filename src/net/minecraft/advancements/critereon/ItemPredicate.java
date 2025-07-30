package net.minecraft.advancements.critereon;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public record ItemPredicate(
    Optional<HolderSet<Item>> items,
    MinMaxBounds.Ints count,
    DataComponentPredicate components,
    Map<ItemSubPredicate.Type<?>, ItemSubPredicate> subPredicates
) implements Predicate<ItemStack> {
    public static final Codec<ItemPredicate> CODEC = RecordCodecBuilder.create(
        p_325221_ -> p_325221_.group(
                    RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("items").forGetter(ItemPredicate::items),
                    MinMaxBounds.Ints.CODEC.optionalFieldOf("count", MinMaxBounds.Ints.ANY).forGetter(ItemPredicate::count),
                    DataComponentPredicate.CODEC.optionalFieldOf("components", DataComponentPredicate.EMPTY).forGetter(ItemPredicate::components),
                    ItemSubPredicate.CODEC.optionalFieldOf("predicates", Map.of()).forGetter(ItemPredicate::subPredicates)
                )
                .apply(p_325221_, ItemPredicate::new)
    );

    public boolean test(ItemStack pStack) {
        if (this.items.isPresent() && !pStack.is(this.items.get())) {
            return false;
        } else if (!this.count.matches(pStack.getCount())) {
            return false;
        } else if (!this.components.test(pStack)) {
            return false;
        } else {
            for (ItemSubPredicate itemsubpredicate : this.subPredicates.values()) {
                if (!itemsubpredicate.matches(pStack)) {
                    return false;
                }
            }

            return true;
        }
    }

    public static class Builder {
        private Optional<HolderSet<Item>> items = Optional.empty();
        private MinMaxBounds.Ints count = MinMaxBounds.Ints.ANY;
        private DataComponentPredicate components = DataComponentPredicate.EMPTY;
        private final ImmutableMap.Builder<ItemSubPredicate.Type<?>, ItemSubPredicate> subPredicates = ImmutableMap.builder();

        private Builder() {
        }

        public static ItemPredicate.Builder item() {
            return new ItemPredicate.Builder();
        }

        public ItemPredicate.Builder of(HolderGetter<Item> pItemRegistry, ItemLike... pItems) {
            this.items = Optional.of(HolderSet.direct(p_300947_ -> p_300947_.asItem().builtInRegistryHolder(), pItems));
            return this;
        }

        public ItemPredicate.Builder of(HolderGetter<Item> pItemRegistry, TagKey<Item> pTag) {
            this.items = Optional.of(pItemRegistry.getOrThrow(pTag));
            return this;
        }

        public ItemPredicate.Builder withCount(MinMaxBounds.Ints pCount) {
            this.count = pCount;
            return this;
        }

        public <T extends ItemSubPredicate> ItemPredicate.Builder withSubPredicate(ItemSubPredicate.Type<T> pType, T pSubPredicate) {
            this.subPredicates.put(pType, pSubPredicate);
            return this;
        }

        public ItemPredicate.Builder hasComponents(DataComponentPredicate pComponents) {
            this.components = pComponents;
            return this;
        }

        public ItemPredicate build() {
            return new ItemPredicate(this.items, this.count, this.components, this.subPredicates.build());
        }
    }
}