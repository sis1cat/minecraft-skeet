package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.Util;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class CopyComponentsFunction extends LootItemConditionalFunction {
    public static final MapCodec<CopyComponentsFunction> CODEC = RecordCodecBuilder.mapCodec(
        p_334725_ -> commonFields(p_334725_)
                .and(
                    p_334725_.group(
                        CopyComponentsFunction.Source.CODEC.fieldOf("source").forGetter(p_329984_ -> p_329984_.source),
                        DataComponentType.CODEC.listOf().optionalFieldOf("include").forGetter(p_330902_ -> p_330902_.include),
                        DataComponentType.CODEC.listOf().optionalFieldOf("exclude").forGetter(p_331318_ -> p_331318_.exclude)
                    )
                )
                .apply(p_334725_, CopyComponentsFunction::new)
    );
    private final CopyComponentsFunction.Source source;
    private final Optional<List<DataComponentType<?>>> include;
    private final Optional<List<DataComponentType<?>>> exclude;
    private final Predicate<DataComponentType<?>> bakedPredicate;

    CopyComponentsFunction(
        List<LootItemCondition> pConditions,
        CopyComponentsFunction.Source pSource,
        Optional<List<DataComponentType<?>>> pInclude,
        Optional<List<DataComponentType<?>>> pExclude
    ) {
        super(pConditions);
        this.source = pSource;
        this.include = pInclude.map(List::copyOf);
        this.exclude = pExclude.map(List::copyOf);
        List<Predicate<DataComponentType<?>>> list = new ArrayList<>(2);
        pExclude.ifPresent(p_329848_ -> list.add(p_331276_ -> !p_329848_.contains(p_331276_)));
        pInclude.ifPresent(p_331486_ -> list.add(p_331486_::contains));
        this.bakedPredicate = Util.allOf(list);
    }

    @Override
    public LootItemFunctionType<CopyComponentsFunction> getType() {
        return LootItemFunctions.COPY_COMPONENTS;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return this.source.getReferencedContextParams();
    }

    @Override
    public ItemStack run(ItemStack p_329465_, LootContext p_328771_) {
        DataComponentMap datacomponentmap = this.source.get(p_328771_);
        p_329465_.applyComponents(datacomponentmap.filter(this.bakedPredicate));
        return p_329465_;
    }

    public static CopyComponentsFunction.Builder copyComponents(CopyComponentsFunction.Source pSource) {
        return new CopyComponentsFunction.Builder(pSource);
    }

    public static class Builder extends LootItemConditionalFunction.Builder<CopyComponentsFunction.Builder> {
        private final CopyComponentsFunction.Source source;
        private Optional<ImmutableList.Builder<DataComponentType<?>>> include = Optional.empty();
        private Optional<ImmutableList.Builder<DataComponentType<?>>> exclude = Optional.empty();

        Builder(CopyComponentsFunction.Source pSource) {
            this.source = pSource;
        }

        public CopyComponentsFunction.Builder include(DataComponentType<?> pInclude) {
            if (this.include.isEmpty()) {
                this.include = Optional.of(ImmutableList.builder());
            }

            this.include.get().add(pInclude);
            return this;
        }

        public CopyComponentsFunction.Builder exclude(DataComponentType<?> pExclude) {
            if (this.exclude.isEmpty()) {
                this.exclude = Optional.of(ImmutableList.builder());
            }

            this.exclude.get().add(pExclude);
            return this;
        }

        protected CopyComponentsFunction.Builder getThis() {
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new CopyComponentsFunction(
                this.getConditions(), this.source, this.include.map(ImmutableList.Builder::build), this.exclude.map(ImmutableList.Builder::build)
            );
        }
    }

    public static enum Source implements StringRepresentable {
        BLOCK_ENTITY("block_entity");

        public static final Codec<CopyComponentsFunction.Source> CODEC = StringRepresentable.fromValues(CopyComponentsFunction.Source::values);
        private final String name;

        private Source(final String pName) {
            this.name = pName;
        }

        public DataComponentMap get(LootContext pContext) {
            switch (this) {
                case BLOCK_ENTITY:
                    BlockEntity blockentity = pContext.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
                    return blockentity != null ? blockentity.collectComponents() : DataComponentMap.EMPTY;
                default:
                    throw new MatchException(null, null);
            }
        }

        public Set<ContextKey<?>> getReferencedContextParams() {
            switch (this) {
                case BLOCK_ENTITY:
                    return Set.of(LootContextParams.BLOCK_ENTITY);
                default:
                    throw new MatchException(null, null);
            }
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}