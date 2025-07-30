package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;

public class CarverConfiguration extends ProbabilityFeatureConfiguration {
    public static final MapCodec<CarverConfiguration> CODEC = RecordCodecBuilder.mapCodec(
        p_224839_ -> p_224839_.group(
                    Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter(p_159113_ -> p_159113_.probability),
                    HeightProvider.CODEC.fieldOf("y").forGetter(p_159111_ -> p_159111_.y),
                    FloatProvider.CODEC.fieldOf("yScale").forGetter(p_159109_ -> p_159109_.yScale),
                    VerticalAnchor.CODEC.fieldOf("lava_level").forGetter(p_159107_ -> p_159107_.lavaLevel),
                    CarverDebugSettings.CODEC.optionalFieldOf("debug_settings", CarverDebugSettings.DEFAULT).forGetter(p_190637_ -> p_190637_.debugSettings),
                    RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("replaceable").forGetter(p_224841_ -> p_224841_.replaceable)
                )
                .apply(p_224839_, CarverConfiguration::new)
    );
    public final HeightProvider y;
    public final FloatProvider yScale;
    public final VerticalAnchor lavaLevel;
    public final CarverDebugSettings debugSettings;
    public final HolderSet<Block> replaceable;

    public CarverConfiguration(
        float pProbability, HeightProvider pY, FloatProvider pYScale, VerticalAnchor pLavaLevel, CarverDebugSettings pDebugSettings, HolderSet<Block> pReplaceable
    ) {
        super(pProbability);
        this.y = pY;
        this.yScale = pYScale;
        this.lavaLevel = pLavaLevel;
        this.debugSettings = pDebugSettings;
        this.replaceable = pReplaceable;
    }
}