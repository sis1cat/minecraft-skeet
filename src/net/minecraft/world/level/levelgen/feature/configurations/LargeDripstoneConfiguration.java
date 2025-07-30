package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;

public class LargeDripstoneConfiguration implements FeatureConfiguration {
    public static final Codec<LargeDripstoneConfiguration> CODEC = RecordCodecBuilder.create(
        p_160966_ -> p_160966_.group(
                    Codec.intRange(1, 512).fieldOf("floor_to_ceiling_search_range").orElse(30).forGetter(p_160984_ -> p_160984_.floorToCeilingSearchRange),
                    IntProvider.codec(1, 60).fieldOf("column_radius").forGetter(p_160982_ -> p_160982_.columnRadius),
                    FloatProvider.codec(0.0F, 20.0F).fieldOf("height_scale").forGetter(p_160980_ -> p_160980_.heightScale),
                    Codec.floatRange(0.1F, 1.0F).fieldOf("max_column_radius_to_cave_height_ratio").forGetter(p_160978_ -> p_160978_.maxColumnRadiusToCaveHeightRatio),
                    FloatProvider.codec(0.1F, 10.0F).fieldOf("stalactite_bluntness").forGetter(p_160976_ -> p_160976_.stalactiteBluntness),
                    FloatProvider.codec(0.1F, 10.0F).fieldOf("stalagmite_bluntness").forGetter(p_160974_ -> p_160974_.stalagmiteBluntness),
                    FloatProvider.codec(0.0F, 2.0F).fieldOf("wind_speed").forGetter(p_160972_ -> p_160972_.windSpeed),
                    Codec.intRange(0, 100).fieldOf("min_radius_for_wind").forGetter(p_160970_ -> p_160970_.minRadiusForWind),
                    Codec.floatRange(0.0F, 5.0F).fieldOf("min_bluntness_for_wind").forGetter(p_160968_ -> p_160968_.minBluntnessForWind)
                )
                .apply(p_160966_, LargeDripstoneConfiguration::new)
    );
    public final int floorToCeilingSearchRange;
    public final IntProvider columnRadius;
    public final FloatProvider heightScale;
    public final float maxColumnRadiusToCaveHeightRatio;
    public final FloatProvider stalactiteBluntness;
    public final FloatProvider stalagmiteBluntness;
    public final FloatProvider windSpeed;
    public final int minRadiusForWind;
    public final float minBluntnessForWind;

    public LargeDripstoneConfiguration(
        int pFloorToCeilingSearchRange,
        IntProvider pColumnRadius,
        FloatProvider pHeightScale,
        float pMaxColumnRadiusToCaveHeightRatio,
        FloatProvider pStalactiteBluntness,
        FloatProvider pStalagmiteBluntness,
        FloatProvider pWindSpeed,
        int pMinRadiusForWind,
        float pMinBluntnessForWind
    ) {
        this.floorToCeilingSearchRange = pFloorToCeilingSearchRange;
        this.columnRadius = pColumnRadius;
        this.heightScale = pHeightScale;
        this.maxColumnRadiusToCaveHeightRatio = pMaxColumnRadiusToCaveHeightRatio;
        this.stalactiteBluntness = pStalactiteBluntness;
        this.stalagmiteBluntness = pStalagmiteBluntness;
        this.windSpeed = pWindSpeed;
        this.minRadiusForWind = pMinRadiusForWind;
        this.minBluntnessForWind = pMinBluntnessForWind;
    }
}