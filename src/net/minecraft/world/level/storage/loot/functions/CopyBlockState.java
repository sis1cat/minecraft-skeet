package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class CopyBlockState extends LootItemConditionalFunction {
    public static final MapCodec<CopyBlockState> CODEC = RecordCodecBuilder.mapCodec(
        p_360670_ -> commonFields(p_360670_)
                .and(
                    p_360670_.group(
                        BuiltInRegistries.BLOCK.holderByNameCodec().fieldOf("block").forGetter(p_297074_ -> p_297074_.block),
                        Codec.STRING.listOf().fieldOf("properties").forGetter(p_297075_ -> p_297075_.properties.stream().map(Property::getName).toList())
                    )
                )
                .apply(p_360670_, CopyBlockState::new)
    );
    private final Holder<Block> block;
    private final Set<Property<?>> properties;

    CopyBlockState(List<LootItemCondition> pConditions, Holder<Block> pBlock, Set<Property<?>> pProperties) {
        super(pConditions);
        this.block = pBlock;
        this.properties = pProperties;
    }

    private CopyBlockState(List<LootItemCondition> pConditions, Holder<Block> pBlock, List<String> pProperties) {
        this(pConditions, pBlock, pProperties.stream().map(pBlock.value().getStateDefinition()::getProperty).filter(Objects::nonNull).collect(Collectors.toSet()));
    }

    @Override
    public LootItemFunctionType<CopyBlockState> getType() {
        return LootItemFunctions.COPY_STATE;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.BLOCK_STATE);
    }

    @Override
    protected ItemStack run(ItemStack pStack, LootContext pContext) {
        BlockState blockstate = pContext.getOptionalParameter(LootContextParams.BLOCK_STATE);
        if (blockstate != null) {
            pStack.update(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY, p_327562_ -> {
                for (Property<?> property : this.properties) {
                    if (blockstate.hasProperty(property)) {
                        p_327562_ = p_327562_.with(property, blockstate);
                    }
                }

                return p_327562_;
            });
        }

        return pStack;
    }

    public static CopyBlockState.Builder copyState(Block pBlock) {
        return new CopyBlockState.Builder(pBlock);
    }

    public static class Builder extends LootItemConditionalFunction.Builder<CopyBlockState.Builder> {
        private final Holder<Block> block;
        private final ImmutableSet.Builder<Property<?>> properties = ImmutableSet.builder();

        Builder(Block pBlock) {
            this.block = pBlock.builtInRegistryHolder();
        }

        public CopyBlockState.Builder copy(Property<?> pProperty) {
            if (!this.block.value().getStateDefinition().getProperties().contains(pProperty)) {
                throw new IllegalStateException("Property " + pProperty + " is not present on block " + this.block);
            } else {
                this.properties.add(pProperty);
                return this;
            }
        }

        protected CopyBlockState.Builder getThis() {
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new CopyBlockState(this.getConditions(), this.block, this.properties.build());
        }
    }
}