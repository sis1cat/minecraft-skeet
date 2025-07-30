package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.loot.LootContext;

public record WeatherCheck(Optional<Boolean> isRaining, Optional<Boolean> isThundering) implements LootItemCondition {
    public static final MapCodec<WeatherCheck> CODEC = RecordCodecBuilder.mapCodec(
        p_327656_ -> p_327656_.group(
                    Codec.BOOL.optionalFieldOf("raining").forGetter(WeatherCheck::isRaining),
                    Codec.BOOL.optionalFieldOf("thundering").forGetter(WeatherCheck::isThundering)
                )
                .apply(p_327656_, WeatherCheck::new)
    );

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.WEATHER_CHECK;
    }

    public boolean test(LootContext pContext) {
        ServerLevel serverlevel = pContext.getLevel();
        return this.isRaining.isPresent() && this.isRaining.get() != serverlevel.isRaining()
            ? false
            : !this.isThundering.isPresent() || this.isThundering.get() == serverlevel.isThundering();
    }

    public static WeatherCheck.Builder weather() {
        return new WeatherCheck.Builder();
    }

    public static class Builder implements LootItemCondition.Builder {
        private Optional<Boolean> isRaining = Optional.empty();
        private Optional<Boolean> isThundering = Optional.empty();

        public WeatherCheck.Builder setRaining(boolean pIsRaining) {
            this.isRaining = Optional.of(pIsRaining);
            return this;
        }

        public WeatherCheck.Builder setThundering(boolean pIsThundering) {
            this.isThundering = Optional.of(pIsThundering);
            return this;
        }

        public WeatherCheck build() {
            return new WeatherCheck(this.isRaining, this.isThundering);
        }
    }
}