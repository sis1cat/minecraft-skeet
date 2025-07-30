package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.alchemy.Potion;

public class BrewedPotionTrigger extends SimpleCriterionTrigger<BrewedPotionTrigger.TriggerInstance> {
    @Override
    public Codec<BrewedPotionTrigger.TriggerInstance> codec() {
        return BrewedPotionTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer pPlayer, Holder<Potion> pPotion) {
        this.trigger(pPlayer, p_308115_ -> p_308115_.matches(pPotion));
    }

    public static record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<Holder<Potion>> potion)
        implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<BrewedPotionTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_340751_ -> p_340751_.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(BrewedPotionTrigger.TriggerInstance::player),
                        Potion.CODEC.optionalFieldOf("potion").forGetter(BrewedPotionTrigger.TriggerInstance::potion)
                    )
                    .apply(p_340751_, BrewedPotionTrigger.TriggerInstance::new)
        );

        public static Criterion<BrewedPotionTrigger.TriggerInstance> brewedPotion() {
            return CriteriaTriggers.BREWED_POTION.createCriterion(new BrewedPotionTrigger.TriggerInstance(Optional.empty(), Optional.empty()));
        }

        public boolean matches(Holder<Potion> pPotion) {
            return !this.potion.isPresent() || this.potion.get().equals(pPotion);
        }

        @Override
        public Optional<ContextAwarePredicate> player() {
            return this.player;
        }
    }
}