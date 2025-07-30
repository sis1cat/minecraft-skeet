package net.minecraft.world.level.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.random.SimpleWeightedRandomList;

public class BiomeSpecialEffects {
    public static final Codec<BiomeSpecialEffects> CODEC = RecordCodecBuilder.create(
        p_375318_ -> p_375318_.group(
                    Codec.INT.fieldOf("fog_color").forGetter(p_151782_ -> p_151782_.fogColor),
                    Codec.INT.fieldOf("water_color").forGetter(p_151780_ -> p_151780_.waterColor),
                    Codec.INT.fieldOf("water_fog_color").forGetter(p_151778_ -> p_151778_.waterFogColor),
                    Codec.INT.fieldOf("sky_color").forGetter(p_151776_ -> p_151776_.skyColor),
                    Codec.INT.optionalFieldOf("foliage_color").forGetter(p_151774_ -> p_151774_.foliageColorOverride),
                    Codec.INT.optionalFieldOf("grass_color").forGetter(p_151772_ -> p_151772_.grassColorOverride),
                    BiomeSpecialEffects.GrassColorModifier.CODEC
                        .optionalFieldOf("grass_color_modifier", BiomeSpecialEffects.GrassColorModifier.NONE)
                        .forGetter(p_151770_ -> p_151770_.grassColorModifier),
                    AmbientParticleSettings.CODEC.optionalFieldOf("particle").forGetter(p_151768_ -> p_151768_.ambientParticleSettings),
                    SoundEvent.CODEC.optionalFieldOf("ambient_sound").forGetter(p_151766_ -> p_151766_.ambientLoopSoundEvent),
                    AmbientMoodSettings.CODEC.optionalFieldOf("mood_sound").forGetter(p_151764_ -> p_151764_.ambientMoodSettings),
                    AmbientAdditionsSettings.CODEC.optionalFieldOf("additions_sound").forGetter(p_151762_ -> p_151762_.ambientAdditionsSettings),
                    SimpleWeightedRandomList.wrappedCodecAllowingEmpty(Music.CODEC).optionalFieldOf("music").forGetter(p_151760_ -> p_151760_.backgroundMusic),
                    Codec.FLOAT.fieldOf("music_volume").orElse(1.0F).forGetter(p_375319_ -> p_375319_.backgroundMusicVolume)
                )
                .apply(p_375318_, BiomeSpecialEffects::new)
    );
    private final int fogColor;
    private final int waterColor;
    private final int waterFogColor;
    private final int skyColor;
    private final Optional<Integer> foliageColorOverride;
    private final Optional<Integer> grassColorOverride;
    private final BiomeSpecialEffects.GrassColorModifier grassColorModifier;
    private final Optional<AmbientParticleSettings> ambientParticleSettings;
    private final Optional<Holder<SoundEvent>> ambientLoopSoundEvent;
    private final Optional<AmbientMoodSettings> ambientMoodSettings;
    private final Optional<AmbientAdditionsSettings> ambientAdditionsSettings;
    private final Optional<SimpleWeightedRandomList<Music>> backgroundMusic;
    private final float backgroundMusicVolume;

    BiomeSpecialEffects(
        int pFogColor,
        int pWaterColor,
        int pWaterFogColor,
        int pSkyColor,
        Optional<Integer> pFoliageColorOverride,
        Optional<Integer> pGrassColorOverride,
        BiomeSpecialEffects.GrassColorModifier pGrassColorModifier,
        Optional<AmbientParticleSettings> pAmbientParticleSettings,
        Optional<Holder<SoundEvent>> pAmbientLoopSoundEvent,
        Optional<AmbientMoodSettings> pAmbientMoodSettings,
        Optional<AmbientAdditionsSettings> pAmbientAdditionsSettings,
        Optional<SimpleWeightedRandomList<Music>> pBackgroundMusic,
        float pBackgroundMusicVolume
    ) {
        this.fogColor = pFogColor;
        this.waterColor = pWaterColor;
        this.waterFogColor = pWaterFogColor;
        this.skyColor = pSkyColor;
        this.foliageColorOverride = pFoliageColorOverride;
        this.grassColorOverride = pGrassColorOverride;
        this.grassColorModifier = pGrassColorModifier;
        this.ambientParticleSettings = pAmbientParticleSettings;
        this.ambientLoopSoundEvent = pAmbientLoopSoundEvent;
        this.ambientMoodSettings = pAmbientMoodSettings;
        this.ambientAdditionsSettings = pAmbientAdditionsSettings;
        this.backgroundMusic = pBackgroundMusic;
        this.backgroundMusicVolume = pBackgroundMusicVolume;
    }

    public int getFogColor() {
        return this.fogColor;
    }

    public int getWaterColor() {
        return this.waterColor;
    }

    public int getWaterFogColor() {
        return this.waterFogColor;
    }

    public int getSkyColor() {
        return this.skyColor;
    }

    public Optional<Integer> getFoliageColorOverride() {
        return this.foliageColorOverride;
    }

    public Optional<Integer> getGrassColorOverride() {
        return this.grassColorOverride;
    }

    public BiomeSpecialEffects.GrassColorModifier getGrassColorModifier() {
        return this.grassColorModifier;
    }

    public Optional<AmbientParticleSettings> getAmbientParticleSettings() {
        return this.ambientParticleSettings;
    }

    public Optional<Holder<SoundEvent>> getAmbientLoopSoundEvent() {
        return this.ambientLoopSoundEvent;
    }

    public Optional<AmbientMoodSettings> getAmbientMoodSettings() {
        return this.ambientMoodSettings;
    }

    public Optional<AmbientAdditionsSettings> getAmbientAdditionsSettings() {
        return this.ambientAdditionsSettings;
    }

    public Optional<SimpleWeightedRandomList<Music>> getBackgroundMusic() {
        return this.backgroundMusic;
    }

    public float getBackgroundMusicVolume() {
        return this.backgroundMusicVolume;
    }

    public static class Builder {
        private OptionalInt fogColor = OptionalInt.empty();
        private OptionalInt waterColor = OptionalInt.empty();
        private OptionalInt waterFogColor = OptionalInt.empty();
        private OptionalInt skyColor = OptionalInt.empty();
        private Optional<Integer> foliageColorOverride = Optional.empty();
        private Optional<Integer> grassColorOverride = Optional.empty();
        private BiomeSpecialEffects.GrassColorModifier grassColorModifier = BiomeSpecialEffects.GrassColorModifier.NONE;
        private Optional<AmbientParticleSettings> ambientParticle = Optional.empty();
        private Optional<Holder<SoundEvent>> ambientLoopSoundEvent = Optional.empty();
        private Optional<AmbientMoodSettings> ambientMoodSettings = Optional.empty();
        private Optional<AmbientAdditionsSettings> ambientAdditionsSettings = Optional.empty();
        private Optional<SimpleWeightedRandomList<Music>> backgroundMusic = Optional.empty();
        private float backgroundMusicVolume = 1.0F;

        public BiomeSpecialEffects.Builder fogColor(int pFogColor) {
            this.fogColor = OptionalInt.of(pFogColor);
            return this;
        }

        public BiomeSpecialEffects.Builder waterColor(int pWaterColor) {
            this.waterColor = OptionalInt.of(pWaterColor);
            return this;
        }

        public BiomeSpecialEffects.Builder waterFogColor(int pWaterFogColor) {
            this.waterFogColor = OptionalInt.of(pWaterFogColor);
            return this;
        }

        public BiomeSpecialEffects.Builder skyColor(int pSkyColor) {
            this.skyColor = OptionalInt.of(pSkyColor);
            return this;
        }

        public BiomeSpecialEffects.Builder foliageColorOverride(int pFoliageColorOverride) {
            this.foliageColorOverride = Optional.of(pFoliageColorOverride);
            return this;
        }

        public BiomeSpecialEffects.Builder grassColorOverride(int pGrassColorOverride) {
            this.grassColorOverride = Optional.of(pGrassColorOverride);
            return this;
        }

        public BiomeSpecialEffects.Builder grassColorModifier(BiomeSpecialEffects.GrassColorModifier pGrassColorModifier) {
            this.grassColorModifier = pGrassColorModifier;
            return this;
        }

        public BiomeSpecialEffects.Builder ambientParticle(AmbientParticleSettings pAmbientParticle) {
            this.ambientParticle = Optional.of(pAmbientParticle);
            return this;
        }

        public BiomeSpecialEffects.Builder ambientLoopSound(Holder<SoundEvent> pAmbientLoopSoundEvent) {
            this.ambientLoopSoundEvent = Optional.of(pAmbientLoopSoundEvent);
            return this;
        }

        public BiomeSpecialEffects.Builder ambientMoodSound(AmbientMoodSettings pAmbientMoodSettings) {
            this.ambientMoodSettings = Optional.of(pAmbientMoodSettings);
            return this;
        }

        public BiomeSpecialEffects.Builder ambientAdditionsSound(AmbientAdditionsSettings pAmbientAdditionsSettings) {
            this.ambientAdditionsSettings = Optional.of(pAmbientAdditionsSettings);
            return this;
        }

        public BiomeSpecialEffects.Builder backgroundMusic(@Nullable Music pBackgroundMusic) {
            if (pBackgroundMusic == null) {
                this.backgroundMusic = Optional.empty();
                return this;
            } else {
                this.backgroundMusic = Optional.of(SimpleWeightedRandomList.single(pBackgroundMusic));
                return this;
            }
        }

        public BiomeSpecialEffects.Builder silenceAllBackgroundMusic() {
            return this.backgroundMusic(SimpleWeightedRandomList.empty()).backgroundMusicVolume(0.0F);
        }

        public BiomeSpecialEffects.Builder backgroundMusic(SimpleWeightedRandomList<Music> pBackgroundMusic) {
            this.backgroundMusic = Optional.of(pBackgroundMusic);
            return this;
        }

        public BiomeSpecialEffects.Builder backgroundMusicVolume(float pBackgroundMusicVolume) {
            this.backgroundMusicVolume = pBackgroundMusicVolume;
            return this;
        }

        public BiomeSpecialEffects build() {
            return new BiomeSpecialEffects(
                this.fogColor.orElseThrow(() -> new IllegalStateException("Missing 'fog' color.")),
                this.waterColor.orElseThrow(() -> new IllegalStateException("Missing 'water' color.")),
                this.waterFogColor.orElseThrow(() -> new IllegalStateException("Missing 'water fog' color.")),
                this.skyColor.orElseThrow(() -> new IllegalStateException("Missing 'sky' color.")),
                this.foliageColorOverride,
                this.grassColorOverride,
                this.grassColorModifier,
                this.ambientParticle,
                this.ambientLoopSoundEvent,
                this.ambientMoodSettings,
                this.ambientAdditionsSettings,
                this.backgroundMusic,
                this.backgroundMusicVolume
            );
        }
    }

    public static enum GrassColorModifier implements StringRepresentable {
        NONE("none") {
            @Override
            public int modifyColor(double p_48081_, double p_48082_, int p_48083_) {
                return p_48083_;
            }
        },
        DARK_FOREST("dark_forest") {
            @Override
            public int modifyColor(double p_48089_, double p_48090_, int p_48091_) {
                return (p_48091_ & 16711422) + 2634762 >> 1;
            }
        },
        SWAMP("swamp") {
            @Override
            public int modifyColor(double p_48097_, double p_48098_, int p_48099_) {
                double d0 = Biome.BIOME_INFO_NOISE.getValue(p_48097_ * 0.0225, p_48098_ * 0.0225, false);
                return d0 < -0.1 ? 5011004 : 6975545;
            }
        };

        private final String name;
        public static final Codec<BiomeSpecialEffects.GrassColorModifier> CODEC = StringRepresentable.fromEnum(
            BiomeSpecialEffects.GrassColorModifier::values
        );

        public abstract int modifyColor(double pX, double pZ, int pGrassColor);

        GrassColorModifier(final String pName) {
            this.name = pName;
        }

        public String getName() {
            return this.name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}