package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.storage.loot.LootContext;

public class TameAnimalTrigger extends SimpleCriterionTrigger<TameAnimalTrigger.TriggerInstance> {
    @Override
    public Codec<TameAnimalTrigger.TriggerInstance> codec() {
        return TameAnimalTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer pPlayer, Animal pEntity) {
        LootContext lootcontext = EntityPredicate.createContext(pPlayer, pEntity);
        this.trigger(pPlayer, p_68838_ -> p_68838_.matches(lootcontext));
    }

    public static record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> entity)
        implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TameAnimalTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_325254_ -> p_325254_.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TameAnimalTrigger.TriggerInstance::player),
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("entity").forGetter(TameAnimalTrigger.TriggerInstance::entity)
                    )
                    .apply(p_325254_, TameAnimalTrigger.TriggerInstance::new)
        );

        public static Criterion<TameAnimalTrigger.TriggerInstance> tamedAnimal() {
            return CriteriaTriggers.TAME_ANIMAL.createCriterion(new TameAnimalTrigger.TriggerInstance(Optional.empty(), Optional.empty()));
        }

        public static Criterion<TameAnimalTrigger.TriggerInstance> tamedAnimal(EntityPredicate.Builder pEntity) {
            return CriteriaTriggers.TAME_ANIMAL
                .createCriterion(new TameAnimalTrigger.TriggerInstance(Optional.empty(), Optional.of(EntityPredicate.wrap(pEntity))));
        }

        public boolean matches(LootContext pLootContext) {
            return this.entity.isEmpty() || this.entity.get().matches(pLootContext);
        }

        @Override
        public void validate(CriterionValidator p_309538_) {
            SimpleCriterionTrigger.SimpleInstance.super.validate(p_309538_);
            p_309538_.validateEntity(this.entity, ".entity");
        }

        @Override
        public Optional<ContextAwarePredicate> player() {
            return this.player;
        }
    }
}