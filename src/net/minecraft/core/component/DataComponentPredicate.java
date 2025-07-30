package net.minecraft.core.component;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public final class DataComponentPredicate implements Predicate<DataComponentMap> {
    public static final Codec<DataComponentPredicate> CODEC = DataComponentType.VALUE_MAP_CODEC
        .xmap(
            p_336043_ -> new DataComponentPredicate(p_336043_.entrySet().stream().map(TypedDataComponent::fromEntryUnchecked).collect(Collectors.toList())),
            p_335229_ -> p_335229_.expectedComponents
                    .stream()
                    .filter(p_332263_ -> !p_332263_.type().isTransient())
                    .collect(Collectors.toMap(TypedDataComponent::type, TypedDataComponent::value))
        );
    public static final StreamCodec<RegistryFriendlyByteBuf, DataComponentPredicate> STREAM_CODEC = TypedDataComponent.STREAM_CODEC
        .apply(ByteBufCodecs.list())
        .map(DataComponentPredicate::new, p_334923_ -> p_334923_.expectedComponents);
    public static final DataComponentPredicate EMPTY = new DataComponentPredicate(List.of());
    private final List<TypedDataComponent<?>> expectedComponents;

    DataComponentPredicate(List<TypedDataComponent<?>> pExpectedComponents) {
        this.expectedComponents = pExpectedComponents;
    }

    public static DataComponentPredicate.Builder builder() {
        return new DataComponentPredicate.Builder();
    }

    public static DataComponentPredicate allOf(DataComponentMap pExpectedComponents) {
        return new DataComponentPredicate(ImmutableList.copyOf(pExpectedComponents));
    }

    public static DataComponentPredicate someOf(DataComponentMap pExpectedComponents, DataComponentType<?>... pTypes) {
        DataComponentPredicate.Builder datacomponentpredicate$builder = new DataComponentPredicate.Builder();

        for (DataComponentType<?> datacomponenttype : pTypes) {
            TypedDataComponent<?> typeddatacomponent = pExpectedComponents.getTyped(datacomponenttype);
            if (typeddatacomponent != null) {
                datacomponentpredicate$builder.expect(typeddatacomponent);
            }
        }

        return datacomponentpredicate$builder.build();
    }

    @Override
    public boolean equals(Object pOther) {
        if (pOther instanceof DataComponentPredicate datacomponentpredicate && this.expectedComponents.equals(datacomponentpredicate.expectedComponents)) {
            return true;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.expectedComponents.hashCode();
    }

    @Override
    public String toString() {
        return this.expectedComponents.toString();
    }

    public boolean test(DataComponentMap pComponents) {
        for (TypedDataComponent<?> typeddatacomponent : this.expectedComponents) {
            Object object = pComponents.get(typeddatacomponent.type());
            if (!Objects.equals(typeddatacomponent.value(), object)) {
                return false;
            }
        }

        return true;
    }

    public boolean test(DataComponentHolder pComponents) {
        return this.test(pComponents.getComponents());
    }

    public boolean alwaysMatches() {
        return this.expectedComponents.isEmpty();
    }

    public DataComponentPatch asPatch() {
        DataComponentPatch.Builder datacomponentpatch$builder = DataComponentPatch.builder();

        for (TypedDataComponent<?> typeddatacomponent : this.expectedComponents) {
            datacomponentpatch$builder.set(typeddatacomponent);
        }

        return datacomponentpatch$builder.build();
    }

    public static class Builder {
        private final List<TypedDataComponent<?>> expectedComponents = new ArrayList<>();

        Builder() {
        }

        public <T> DataComponentPredicate.Builder expect(TypedDataComponent<T> pComponent) {
            return this.expect(pComponent.type(), pComponent.value());
        }

        public <T> DataComponentPredicate.Builder expect(DataComponentType<? super T> pComponent, T pValue) {
            for (TypedDataComponent<?> typeddatacomponent : this.expectedComponents) {
                if (typeddatacomponent.type() == pComponent) {
                    throw new IllegalArgumentException("Predicate already has component of type: '" + pComponent + "'");
                }
            }

            this.expectedComponents.add(new TypedDataComponent<>(pComponent, pValue));
            return this;
        }

        public DataComponentPredicate build() {
            return new DataComponentPredicate(List.copyOf(this.expectedComponents));
        }
    }
}