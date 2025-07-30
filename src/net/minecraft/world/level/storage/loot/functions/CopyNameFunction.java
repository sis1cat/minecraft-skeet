package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class CopyNameFunction extends LootItemConditionalFunction {
    public static final MapCodec<CopyNameFunction> CODEC = RecordCodecBuilder.mapCodec(
        p_297078_ -> commonFields(p_297078_)
                .and(CopyNameFunction.NameSource.CODEC.fieldOf("source").forGetter(p_297077_ -> p_297077_.source))
                .apply(p_297078_, CopyNameFunction::new)
    );
    private final CopyNameFunction.NameSource source;

    private CopyNameFunction(List<LootItemCondition> pConditions, CopyNameFunction.NameSource pSource) {
        super(pConditions);
        this.source = pSource;
    }

    @Override
    public LootItemFunctionType<CopyNameFunction> getType() {
        return LootItemFunctions.COPY_NAME;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(this.source.param);
    }

    @Override
    public ItemStack run(ItemStack pStack, LootContext pContext) {
        if (pContext.getOptionalParameter(this.source.param) instanceof Nameable nameable) {
            pStack.set(DataComponents.CUSTOM_NAME, nameable.getCustomName());
        }

        return pStack;
    }

    public static LootItemConditionalFunction.Builder<?> copyName(CopyNameFunction.NameSource pSource) {
        return simpleBuilder(p_297080_ -> new CopyNameFunction(p_297080_, pSource));
    }

    public static enum NameSource implements StringRepresentable {
        THIS("this", LootContextParams.THIS_ENTITY),
        ATTACKING_ENTITY("attacking_entity", LootContextParams.ATTACKING_ENTITY),
        LAST_DAMAGE_PLAYER("last_damage_player", LootContextParams.LAST_DAMAGE_PLAYER),
        BLOCK_ENTITY("block_entity", LootContextParams.BLOCK_ENTITY);

        public static final Codec<CopyNameFunction.NameSource> CODEC = StringRepresentable.fromEnum(CopyNameFunction.NameSource::values);
        private final String name;
        final ContextKey<?> param;

        private NameSource(final String pName, final ContextKey<?> pParam) {
            this.name = pName;
            this.param = pParam;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}