package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;

public class PointedDripstoneConfiguration implements FeatureConfiguration {
    public static final Codec<PointedDripstoneConfiguration> CODEC = RecordCodecBuilder.create(
        p_191286_ -> p_191286_.group(
                    Codec.floatRange(0.0F, 1.0F).fieldOf("chance_of_taller_dripstone").orElse(0.2F).forGetter(p_191294_ -> p_191294_.chanceOfTallerDripstone),
                    Codec.floatRange(0.0F, 1.0F).fieldOf("chance_of_directional_spread").orElse(0.7F).forGetter(p_191292_ -> p_191292_.chanceOfDirectionalSpread),
                    Codec.floatRange(0.0F, 1.0F).fieldOf("chance_of_spread_radius2").orElse(0.5F).forGetter(p_191290_ -> p_191290_.chanceOfSpreadRadius2),
                    Codec.floatRange(0.0F, 1.0F).fieldOf("chance_of_spread_radius3").orElse(0.5F).forGetter(p_191288_ -> p_191288_.chanceOfSpreadRadius3)
                )
                .apply(p_191286_, PointedDripstoneConfiguration::new)
    );
    public final float chanceOfTallerDripstone;
    public final float chanceOfDirectionalSpread;
    public final float chanceOfSpreadRadius2;
    public final float chanceOfSpreadRadius3;

    public PointedDripstoneConfiguration(float pChanceOfTallerDripstone, float pChanceOfDirectionalSpread, float pChanceOfSpreadRadius2, float pChanceOfSpreadRadius3) {
        this.chanceOfTallerDripstone = pChanceOfTallerDripstone;
        this.chanceOfDirectionalSpread = pChanceOfDirectionalSpread;
        this.chanceOfSpreadRadius2 = pChanceOfSpreadRadius2;
        this.chanceOfSpreadRadius3 = pChanceOfSpreadRadius3;
    }
}