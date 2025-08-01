package net.minecraft.core;

import com.mojang.serialization.Lifecycle;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

public class DefaultedMappedRegistry<T> extends MappedRegistry<T> implements DefaultedRegistry<T> {
    private final ResourceLocation defaultKey;
    private Holder.Reference<T> defaultValue;

    public DefaultedMappedRegistry(String pDefaultKey, ResourceKey<? extends Registry<T>> pKey, Lifecycle pRegistryLifecycle, boolean pHasIntrusiveHolders) {
        super(pKey, pRegistryLifecycle, pHasIntrusiveHolders);
        this.defaultKey = ResourceLocation.parse(pDefaultKey);
    }

    @Override
    public Holder.Reference<T> register(ResourceKey<T> p_332872_, T p_328327_, RegistrationInfo p_331941_) {
        Holder.Reference<T> reference = super.register(p_332872_, p_328327_, p_331941_);
        if (this.defaultKey.equals(p_332872_.location())) {
            this.defaultValue = reference;
        }

        return reference;
    }

    @Override
    public int getId(@Nullable T p_260033_) {
        int i = super.getId(p_260033_);
        return i == -1 ? super.getId(this.defaultValue.value()) : i;
    }

    @Nonnull
    @Override
    public ResourceLocation getKey(T p_259233_) {
        ResourceLocation resourcelocation = super.getKey(p_259233_);
        return resourcelocation == null ? this.defaultKey : resourcelocation;
    }

    @Nonnull
    @Override
    public T getValue(@Nullable ResourceLocation p_365640_) {
        T t = super.getValue(p_365640_);
        return t == null ? this.defaultValue.value() : t;
    }

    @Override
    public Optional<T> getOptional(@Nullable ResourceLocation p_260078_) {
        return Optional.ofNullable(super.getValue(p_260078_));
    }

    @Override
    public Optional<Holder.Reference<T>> getAny() {
        return Optional.ofNullable(this.defaultValue);
    }

    @Nonnull
    @Override
    public T byId(int p_259534_) {
        T t = super.byId(p_259534_);
        return t == null ? this.defaultValue.value() : t;
    }

    @Override
    public Optional<Holder.Reference<T>> getRandom(RandomSource p_260255_) {
        return super.getRandom(p_260255_).or(() -> Optional.of(this.defaultValue));
    }

    @Override
    public ResourceLocation getDefaultKey() {
        return this.defaultKey;
    }
}