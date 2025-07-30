package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import java.util.OptionalLong;
import net.minecraft.util.RandomSource;
import org.apache.commons.lang3.StringUtils;

public class WorldOptions {
    public static final MapCodec<WorldOptions> CODEC = RecordCodecBuilder.mapCodec(
        p_327460_ -> p_327460_.group(
                    Codec.LONG.fieldOf("seed").stable().forGetter(WorldOptions::seed),
                    Codec.BOOL.fieldOf("generate_features").orElse(true).stable().forGetter(WorldOptions::generateStructures),
                    Codec.BOOL.fieldOf("bonus_chest").orElse(false).stable().forGetter(WorldOptions::generateBonusChest),
                    Codec.STRING.lenientOptionalFieldOf("legacy_custom_options").stable().forGetter(p_249400_ -> p_249400_.legacyCustomOptions)
                )
                .apply(p_327460_, p_327460_.stable(WorldOptions::new))
    );
    public static final WorldOptions DEMO_OPTIONS = new WorldOptions((long)"North Carolina".hashCode(), true, true);
    private final long seed;
    private final boolean generateStructures;
    private final boolean generateBonusChest;
    private final Optional<String> legacyCustomOptions;

    public WorldOptions(long pSeed, boolean pGenerateStructures, boolean pGenerateBonusChest) {
        this(pSeed, pGenerateStructures, pGenerateBonusChest, Optional.empty());
    }

    public static WorldOptions defaultWithRandomSeed() {
        return new WorldOptions(randomSeed(), true, false);
    }

    public static WorldOptions testWorldWithRandomSeed() {
        return new WorldOptions(randomSeed(), false, false);
    }

    private WorldOptions(long pSeed, boolean pGenerateStructures, boolean pGenerateBonusChest, Optional<String> pLegacyCustomOptions) {
        this.seed = pSeed;
        this.generateStructures = pGenerateStructures;
        this.generateBonusChest = pGenerateBonusChest;
        this.legacyCustomOptions = pLegacyCustomOptions;
    }

    public long seed() {
        return this.seed;
    }

    public boolean generateStructures() {
        return this.generateStructures;
    }

    public boolean generateBonusChest() {
        return this.generateBonusChest;
    }

    public boolean isOldCustomizedWorld() {
        return this.legacyCustomOptions.isPresent();
    }

    public WorldOptions withBonusChest(boolean pGenerateBonusChest) {
        return new WorldOptions(this.seed, this.generateStructures, pGenerateBonusChest, this.legacyCustomOptions);
    }

    public WorldOptions withStructures(boolean pGenerateStructures) {
        return new WorldOptions(this.seed, pGenerateStructures, this.generateBonusChest, this.legacyCustomOptions);
    }

    public WorldOptions withSeed(OptionalLong pSeed) {
        return new WorldOptions(pSeed.orElse(randomSeed()), this.generateStructures, this.generateBonusChest, this.legacyCustomOptions);
    }

    public static OptionalLong parseSeed(String pSeed) {
        pSeed = pSeed.trim();
        if (StringUtils.isEmpty(pSeed)) {
            return OptionalLong.empty();
        } else {
            try {
                return OptionalLong.of(Long.parseLong(pSeed));
            } catch (NumberFormatException numberformatexception) {
                return OptionalLong.of((long)pSeed.hashCode());
            }
        }
    }

    public static long randomSeed() {
        return RandomSource.create().nextLong();
    }
}