package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.Util;

public class AllOfCondition extends CompositeLootItemCondition {
    public static final MapCodec<AllOfCondition> CODEC = createCodec(AllOfCondition::new);
    public static final Codec<AllOfCondition> INLINE_CODEC = createInlineCodec(AllOfCondition::new);

    AllOfCondition(List<LootItemCondition> pConditions) {
        super(pConditions, Util.allOf(pConditions));
    }

    public static AllOfCondition allOf(List<LootItemCondition> pConditions) {
        return new AllOfCondition(List.copyOf(pConditions));
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.ALL_OF;
    }

    public static AllOfCondition.Builder allOf(LootItemCondition.Builder... pConditions) {
        return new AllOfCondition.Builder(pConditions);
    }

    public static class Builder extends CompositeLootItemCondition.Builder {
        public Builder(LootItemCondition.Builder... p_286842_) {
            super(p_286842_);
        }

        @Override
        public AllOfCondition.Builder and(LootItemCondition.Builder p_286760_) {
            this.addTerm(p_286760_);
            return this;
        }

        @Override
        protected LootItemCondition create(List<LootItemCondition> p_299819_) {
            return new AllOfCondition(p_299819_);
        }
    }
}