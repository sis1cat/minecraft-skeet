package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.slf4j.Logger;

public class MobSpawnSettings {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float DEFAULT_CREATURE_SPAWN_PROBABILITY = 0.1F;
    public static final WeightedRandomList<MobSpawnSettings.SpawnerData> EMPTY_MOB_LIST = WeightedRandomList.create();
    public static final MobSpawnSettings EMPTY = new MobSpawnSettings.Builder().build();
    public static final MapCodec<MobSpawnSettings> CODEC = RecordCodecBuilder.mapCodec(
        p_187051_ -> p_187051_.group(
                    Codec.floatRange(0.0F, 0.9999999F).optionalFieldOf("creature_spawn_probability", 0.1F).forGetter(p_187055_ -> p_187055_.creatureGenerationProbability),
                    Codec.simpleMap(
                            MobCategory.CODEC,
                            WeightedRandomList.codec(MobSpawnSettings.SpawnerData.CODEC).promotePartial(Util.prefix("Spawn data: ", LOGGER::error)),
                            StringRepresentable.keys(MobCategory.values())
                        )
                        .fieldOf("spawners")
                        .forGetter(p_187053_ -> p_187053_.spawners),
                    Codec.simpleMap(BuiltInRegistries.ENTITY_TYPE.byNameCodec(), MobSpawnSettings.MobSpawnCost.CODEC, BuiltInRegistries.ENTITY_TYPE)
                        .fieldOf("spawn_costs")
                        .forGetter(p_187049_ -> p_187049_.mobSpawnCosts)
                )
                .apply(p_187051_, MobSpawnSettings::new)
    );
    private final float creatureGenerationProbability;
    private final Map<MobCategory, WeightedRandomList<MobSpawnSettings.SpawnerData>> spawners;
    private final Map<EntityType<?>, MobSpawnSettings.MobSpawnCost> mobSpawnCosts;

    MobSpawnSettings(
        float pCreatureGenerationProbability,
        Map<MobCategory, WeightedRandomList<MobSpawnSettings.SpawnerData>> pSpawners,
        Map<EntityType<?>, MobSpawnSettings.MobSpawnCost> pMobSpawnCosts
    ) {
        this.creatureGenerationProbability = pCreatureGenerationProbability;
        this.spawners = ImmutableMap.copyOf(pSpawners);
        this.mobSpawnCosts = ImmutableMap.copyOf(pMobSpawnCosts);
    }

    public WeightedRandomList<MobSpawnSettings.SpawnerData> getMobs(MobCategory pCategory) {
        return this.spawners.getOrDefault(pCategory, EMPTY_MOB_LIST);
    }

    @Nullable
    public MobSpawnSettings.MobSpawnCost getMobSpawnCost(EntityType<?> pEntityType) {
        return this.mobSpawnCosts.get(pEntityType);
    }

    public float getCreatureProbability() {
        return this.creatureGenerationProbability;
    }

    public static class Builder {
        private final Map<MobCategory, List<MobSpawnSettings.SpawnerData>> spawners = Stream.of(MobCategory.values())
            .collect(ImmutableMap.toImmutableMap(p_48383_ -> (MobCategory)p_48383_, p_48375_ -> Lists.newArrayList()));
        private final Map<EntityType<?>, MobSpawnSettings.MobSpawnCost> mobSpawnCosts = Maps.newLinkedHashMap();
        private float creatureGenerationProbability = 0.1F;

        public MobSpawnSettings.Builder addSpawn(MobCategory pClassification, MobSpawnSettings.SpawnerData pSpawner) {
            this.spawners.get(pClassification).add(pSpawner);
            return this;
        }

        public MobSpawnSettings.Builder addMobCharge(EntityType<?> pEntityType, double pCharge, double pEnergyBudget) {
            this.mobSpawnCosts.put(pEntityType, new MobSpawnSettings.MobSpawnCost(pEnergyBudget, pCharge));
            return this;
        }

        public MobSpawnSettings.Builder creatureGenerationProbability(float pProbability) {
            this.creatureGenerationProbability = pProbability;
            return this;
        }

        public MobSpawnSettings build() {
            return new MobSpawnSettings(
                this.creatureGenerationProbability,
                this.spawners
                    .entrySet()
                    .stream()
                    .collect(ImmutableMap.toImmutableMap(Entry::getKey, p_151809_ -> WeightedRandomList.create(p_151809_.getValue()))),
                ImmutableMap.copyOf(this.mobSpawnCosts)
            );
        }
    }

    public static record MobSpawnCost(double energyBudget, double charge) {
        public static final Codec<MobSpawnSettings.MobSpawnCost> CODEC = RecordCodecBuilder.create(
            p_48399_ -> p_48399_.group(
                        Codec.DOUBLE.fieldOf("energy_budget").forGetter(p_151813_ -> p_151813_.energyBudget),
                        Codec.DOUBLE.fieldOf("charge").forGetter(p_151811_ -> p_151811_.charge)
                    )
                    .apply(p_48399_, MobSpawnSettings.MobSpawnCost::new)
        );
    }

    public static class SpawnerData extends WeightedEntry.IntrusiveBase {
        public static final Codec<MobSpawnSettings.SpawnerData> CODEC = RecordCodecBuilder.<MobSpawnSettings.SpawnerData>create(
                p_359962_ -> p_359962_.group(
                            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("type").forGetter(p_151826_ -> p_151826_.type),
                            Weight.CODEC.fieldOf("weight").forGetter(WeightedEntry.IntrusiveBase::getWeight),
                            ExtraCodecs.POSITIVE_INT.fieldOf("minCount").forGetter(p_151824_ -> p_151824_.minCount),
                            ExtraCodecs.POSITIVE_INT.fieldOf("maxCount").forGetter(p_151820_ -> p_151820_.maxCount)
                        )
                        .apply(p_359962_, MobSpawnSettings.SpawnerData::new)
            )
            .validate(
                p_275168_ -> p_275168_.minCount > p_275168_.maxCount
                        ? DataResult.error(() -> "minCount needs to be smaller or equal to maxCount")
                        : DataResult.success(p_275168_)
            );
        public final EntityType<?> type;
        public final int minCount;
        public final int maxCount;

        public SpawnerData(EntityType<?> pType, int pWeight, int pMinCount, int pMaxCount) {
            this(pType, Weight.of(pWeight), pMinCount, pMaxCount);
        }

        public SpawnerData(EntityType<?> pType, Weight pWeight, int pMinCount, int pMaxCount) {
            super(pWeight);
            this.type = pType.getCategory() == MobCategory.MISC ? EntityType.PIG : pType;
            this.minCount = pMinCount;
            this.maxCount = pMaxCount;
        }

        @Override
        public String toString() {
            return EntityType.getKey(this.type) + "*(" + this.minCount + "-" + this.maxCount + "):" + this.getWeight();
        }
    }
}