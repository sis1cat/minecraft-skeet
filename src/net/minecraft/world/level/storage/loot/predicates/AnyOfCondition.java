package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.Util;

public class AnyOfCondition extends CompositeLootItemCondition {
    public static final MapCodec<AnyOfCondition> CODEC = createCodec(AnyOfCondition::new);

    AnyOfCondition(List<LootItemCondition> pConditions) {
        super(pConditions, Util.anyOf(pConditions));
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.ANY_OF;
    }

    public static AnyOfCondition.Builder anyOf(LootItemCondition.Builder... pConditions) {
        return new AnyOfCondition.Builder(pConditions);
    }

    public static class Builder extends CompositeLootItemCondition.Builder {
        public Builder(LootItemCondition.Builder... p_286497_) {
            super(p_286497_);
        }

        @Override
        public AnyOfCondition.Builder or(LootItemCondition.Builder p_286344_) {
            this.addTerm(p_286344_);
            return this;
        }

        @Override
        protected LootItemCondition create(List<LootItemCondition> p_297863_) {
            return new AnyOfCondition(p_297863_);
        }
    }
}