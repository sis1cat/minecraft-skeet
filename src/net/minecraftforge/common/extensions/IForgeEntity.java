package net.minecraftforge.common.extensions;

import java.util.function.BiPredicate;
import net.minecraftforge.fluids.FluidType;

public interface IForgeEntity {
    default boolean isAddedToWorld() {
        return false;
    }

    default boolean isInFluidType(BiPredicate<FluidType, Double> predicate) {
        return this.isInFluidType(predicate, false);
    }

    default boolean isInFluidType(BiPredicate<FluidType, Double> predicate, boolean forAllTypes) {
        return false;
    }

    default boolean canSwimInFluidType(FluidType type) {
        return false;
    }
}