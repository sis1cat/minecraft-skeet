package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicLike;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.OptionalDynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;

public class WorldGenSettingsFix extends DataFix {
    private static final String VILLAGE = "minecraft:village";
    private static final String DESERT_PYRAMID = "minecraft:desert_pyramid";
    private static final String IGLOO = "minecraft:igloo";
    private static final String JUNGLE_TEMPLE = "minecraft:jungle_pyramid";
    private static final String SWAMP_HUT = "minecraft:swamp_hut";
    private static final String PILLAGER_OUTPOST = "minecraft:pillager_outpost";
    private static final String END_CITY = "minecraft:endcity";
    private static final String WOODLAND_MANSION = "minecraft:mansion";
    private static final String OCEAN_MONUMENT = "minecraft:monument";
    private static final ImmutableMap<String, WorldGenSettingsFix.StructureFeatureConfiguration> DEFAULTS = ImmutableMap.<String, WorldGenSettingsFix.StructureFeatureConfiguration>builder()
        .put("minecraft:village", new WorldGenSettingsFix.StructureFeatureConfiguration(32, 8, 10387312))
        .put("minecraft:desert_pyramid", new WorldGenSettingsFix.StructureFeatureConfiguration(32, 8, 14357617))
        .put("minecraft:igloo", new WorldGenSettingsFix.StructureFeatureConfiguration(32, 8, 14357618))
        .put("minecraft:jungle_pyramid", new WorldGenSettingsFix.StructureFeatureConfiguration(32, 8, 14357619))
        .put("minecraft:swamp_hut", new WorldGenSettingsFix.StructureFeatureConfiguration(32, 8, 14357620))
        .put("minecraft:pillager_outpost", new WorldGenSettingsFix.StructureFeatureConfiguration(32, 8, 165745296))
        .put("minecraft:monument", new WorldGenSettingsFix.StructureFeatureConfiguration(32, 5, 10387313))
        .put("minecraft:endcity", new WorldGenSettingsFix.StructureFeatureConfiguration(20, 11, 10387313))
        .put("minecraft:mansion", new WorldGenSettingsFix.StructureFeatureConfiguration(80, 20, 10387319))
        .build();

    public WorldGenSettingsFix(Schema pOutputSchema) {
        super(pOutputSchema, true);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            "WorldGenSettings building",
            this.getInputSchema().getType(References.WORLD_GEN_SETTINGS),
            p_17184_ -> p_17184_.update(DSL.remainderFinder(), WorldGenSettingsFix::fix)
        );
    }

    private static <T> Dynamic<T> noise(long pSeed, DynamicLike<T> pData, Dynamic<T> pSettings, Dynamic<T> pBiomeNoise) {
        return pData.createMap(
            ImmutableMap.of(
                pData.createString("type"),
                pData.createString("minecraft:noise"),
                pData.createString("biome_source"),
                pBiomeNoise,
                pData.createString("seed"),
                pData.createLong(pSeed),
                pData.createString("settings"),
                pSettings
            )
        );
    }

    private static <T> Dynamic<T> vanillaBiomeSource(Dynamic<T> pData, long pSeed, boolean pLegacyBiomeInitLayer, boolean pLargeBiomes) {
        Builder<Dynamic<T>, Dynamic<T>> builder = ImmutableMap.<Dynamic<T>, Dynamic<T>>builder()
            .put(pData.createString("type"), pData.createString("minecraft:vanilla_layered"))
            .put(pData.createString("seed"), pData.createLong(pSeed))
            .put(pData.createString("large_biomes"), pData.createBoolean(pLargeBiomes));
        if (pLegacyBiomeInitLayer) {
            builder.put(pData.createString("legacy_biome_init_layer"), pData.createBoolean(pLegacyBiomeInitLayer));
        }

        return pData.createMap(builder.build());
    }

    private static <T> Dynamic<T> fix(Dynamic<T> pData) {
        DynamicOps<T> dynamicops = pData.getOps();
        long i = pData.get("RandomSeed").asLong(0L);
        Optional<String> optional = pData.get("generatorName").asString().map(p_17227_ -> p_17227_.toLowerCase(Locale.ROOT)).result();
        Optional<String> optional1 = pData.get("legacy_custom_options")
            .asString()
            .result()
            .map(Optional::of)
            .orElseGet(() -> optional.equals(Optional.of("customized")) ? pData.get("generatorOptions").asString().result() : Optional.empty());
        boolean flag = false;
        Dynamic<T> dynamic;
        if (optional.equals(Optional.of("customized"))) {
            dynamic = defaultOverworld(pData, i);
        } else if (optional.isEmpty()) {
            dynamic = defaultOverworld(pData, i);
        } else {
            String $$28 = optional.get();
            switch ($$28) {
                case "flat":
                    OptionalDynamic<T> optionaldynamic = pData.get("generatorOptions");
                    Map<Dynamic<T>, Dynamic<T>> map = fixFlatStructures(dynamicops, optionaldynamic);
                    dynamic = pData.createMap(
                        ImmutableMap.of(
                            pData.createString("type"),
                            pData.createString("minecraft:flat"),
                            pData.createString("settings"),
                            pData.createMap(
                                ImmutableMap.of(
                                    pData.createString("structures"),
                                    pData.createMap(map),
                                    pData.createString("layers"),
                                    optionaldynamic.get("layers")
                                        .result()
                                        .orElseGet(
                                            () -> pData.createList(
                                                    Stream.of(
                                                        pData.createMap(
                                                            ImmutableMap.of(
                                                                pData.createString("height"),
                                                                pData.createInt(1),
                                                                pData.createString("block"),
                                                                pData.createString("minecraft:bedrock")
                                                            )
                                                        ),
                                                        pData.createMap(
                                                            ImmutableMap.of(
                                                                pData.createString("height"),
                                                                pData.createInt(2),
                                                                pData.createString("block"),
                                                                pData.createString("minecraft:dirt")
                                                            )
                                                        ),
                                                        pData.createMap(
                                                            ImmutableMap.of(
                                                                pData.createString("height"),
                                                                pData.createInt(1),
                                                                pData.createString("block"),
                                                                pData.createString("minecraft:grass_block")
                                                            )
                                                        )
                                                    )
                                                )
                                        ),
                                    pData.createString("biome"),
                                    pData.createString(optionaldynamic.get("biome").asString("minecraft:plains"))
                                )
                            )
                        )
                    );
                    break;
                case "debug_all_block_states":
                    dynamic = pData.createMap(ImmutableMap.of(pData.createString("type"), pData.createString("minecraft:debug")));
                    break;
                case "buffet":
                    OptionalDynamic<T> optionaldynamic1 = pData.get("generatorOptions");
                    OptionalDynamic<?> optionaldynamic2 = optionaldynamic1.get("chunk_generator");
                    Optional<String> optional2 = optionaldynamic2.get("type").asString().result();
                    Dynamic<T> dynamic1;
                    if (Objects.equals(optional2, Optional.of("minecraft:caves"))) {
                        dynamic1 = pData.createString("minecraft:caves");
                        flag = true;
                    } else if (Objects.equals(optional2, Optional.of("minecraft:floating_islands"))) {
                        dynamic1 = pData.createString("minecraft:floating_islands");
                    } else {
                        dynamic1 = pData.createString("minecraft:overworld");
                    }

                    Dynamic<T> dynamic2 = optionaldynamic1.get("biome_source")
                        .result()
                        .orElseGet(() -> pData.createMap(ImmutableMap.of(pData.createString("type"), pData.createString("minecraft:fixed"))));
                    Dynamic<T> dynamic3;
                    if (dynamic2.get("type").asString().result().equals(Optional.of("minecraft:fixed"))) {
                        String s1 = dynamic2.get("options")
                            .get("biomes")
                            .asStream()
                            .findFirst()
                            .flatMap(p_326673_ -> p_326673_.asString().result())
                            .orElse("minecraft:ocean");
                        dynamic3 = dynamic2.remove("options").set("biome", pData.createString(s1));
                    } else {
                        dynamic3 = dynamic2;
                    }

                    dynamic = noise(i, pData, dynamic1, dynamic3);
                    break;
                default:
                    boolean flag1 = optional.get().equals("default");
                    boolean flag2 = optional.get().equals("default_1_1") || flag1 && pData.get("generatorVersion").asInt(0) == 0;
                    boolean flag3 = optional.get().equals("amplified");
                    boolean flag4 = optional.get().equals("largebiomes");
                    dynamic = noise(
                        i, pData, pData.createString(flag3 ? "minecraft:amplified" : "minecraft:overworld"), vanillaBiomeSource(pData, i, flag2, flag4)
                    );
            }
        }

        boolean flag5 = pData.get("MapFeatures").asBoolean(true);
        boolean flag6 = pData.get("BonusChest").asBoolean(false);
        Builder<T, T> builder = ImmutableMap.builder();
        builder.put(dynamicops.createString("seed"), dynamicops.createLong(i));
        builder.put(dynamicops.createString("generate_features"), dynamicops.createBoolean(flag5));
        builder.put(dynamicops.createString("bonus_chest"), dynamicops.createBoolean(flag6));
        builder.put(dynamicops.createString("dimensions"), vanillaLevels(pData, i, dynamic, flag));
        optional1.ifPresent(p_17182_ -> builder.put(dynamicops.createString("legacy_custom_options"), dynamicops.createString(p_17182_)));
        return new Dynamic<>(dynamicops, dynamicops.createMap(builder.build()));
    }

    protected static <T> Dynamic<T> defaultOverworld(Dynamic<T> pData, long pSeed) {
        return noise(pSeed, pData, pData.createString("minecraft:overworld"), vanillaBiomeSource(pData, pSeed, false, false));
    }

    protected static <T> T vanillaLevels(Dynamic<T> pData, long pSeed, Dynamic<T> pGenerator, boolean pCaves) {
        DynamicOps<T> dynamicops = pData.getOps();
        return dynamicops.createMap(
            ImmutableMap.of(
                dynamicops.createString("minecraft:overworld"),
                dynamicops.createMap(
                    ImmutableMap.of(
                        dynamicops.createString("type"),
                        dynamicops.createString("minecraft:overworld" + (pCaves ? "_caves" : "")),
                        dynamicops.createString("generator"),
                        pGenerator.getValue()
                    )
                ),
                dynamicops.createString("minecraft:the_nether"),
                dynamicops.createMap(
                    ImmutableMap.of(
                        dynamicops.createString("type"),
                        dynamicops.createString("minecraft:the_nether"),
                        dynamicops.createString("generator"),
                        noise(
                                pSeed,
                                pData,
                                pData.createString("minecraft:nether"),
                                pData.createMap(
                                    ImmutableMap.of(
                                        pData.createString("type"),
                                        pData.createString("minecraft:multi_noise"),
                                        pData.createString("seed"),
                                        pData.createLong(pSeed),
                                        pData.createString("preset"),
                                        pData.createString("minecraft:nether")
                                    )
                                )
                            )
                            .getValue()
                    )
                ),
                dynamicops.createString("minecraft:the_end"),
                dynamicops.createMap(
                    ImmutableMap.of(
                        dynamicops.createString("type"),
                        dynamicops.createString("minecraft:the_end"),
                        dynamicops.createString("generator"),
                        noise(
                                pSeed,
                                pData,
                                pData.createString("minecraft:end"),
                                pData.createMap(
                                    ImmutableMap.of(
                                        pData.createString("type"),
                                        pData.createString("minecraft:the_end"),
                                        pData.createString("seed"),
                                        pData.createLong(pSeed)
                                    )
                                )
                            )
                            .getValue()
                    )
                )
            )
        );
    }

    private static <T> Map<Dynamic<T>, Dynamic<T>> fixFlatStructures(DynamicOps<T> pOps, OptionalDynamic<T> pGeneratorOptions) {
        MutableInt mutableint = new MutableInt(32);
        MutableInt mutableint1 = new MutableInt(3);
        MutableInt mutableint2 = new MutableInt(128);
        MutableBoolean mutableboolean = new MutableBoolean(false);
        Map<String, WorldGenSettingsFix.StructureFeatureConfiguration> map = Maps.newHashMap();
        if (pGeneratorOptions.result().isEmpty()) {
            mutableboolean.setTrue();
            map.put("minecraft:village", DEFAULTS.get("minecraft:village"));
        }

        pGeneratorOptions.get("structures")
            .flatMap(Dynamic::getMapValues)
            .ifSuccess(
                p_17257_ -> p_17257_.forEach(
                        (p_326671_, p_326672_) -> p_326672_.getMapValues()
                                .result()
                                .ifPresent(
                                    p_145816_ -> p_145816_.forEach(
                                            (p_145807_, p_145808_) -> {
                                                String s = p_326671_.asString("");
                                                String s1 = p_145807_.asString("");
                                                String s2 = p_145808_.asString("");
                                                if ("stronghold".equals(s)) {
                                                    mutableboolean.setTrue();
                                                    switch (s1) {
                                                        case "distance":
                                                            mutableint.setValue(getInt(s2, mutableint.getValue(), 1));
                                                            return;
                                                        case "spread":
                                                            mutableint1.setValue(getInt(s2, mutableint1.getValue(), 1));
                                                            return;
                                                        case "count":
                                                            mutableint2.setValue(getInt(s2, mutableint2.getValue(), 1));
                                                            return;
                                                    }
                                                } else {
                                                    switch (s1) {
                                                        case "distance":
                                                            switch (s) {
                                                                case "village":
                                                                    setSpacing(map, "minecraft:village", s2, 9);
                                                                    return;
                                                                case "biome_1":
                                                                    setSpacing(map, "minecraft:desert_pyramid", s2, 9);
                                                                    setSpacing(map, "minecraft:igloo", s2, 9);
                                                                    setSpacing(map, "minecraft:jungle_pyramid", s2, 9);
                                                                    setSpacing(map, "minecraft:swamp_hut", s2, 9);
                                                                    setSpacing(map, "minecraft:pillager_outpost", s2, 9);
                                                                    return;
                                                                case "endcity":
                                                                    setSpacing(map, "minecraft:endcity", s2, 1);
                                                                    return;
                                                                case "mansion":
                                                                    setSpacing(map, "minecraft:mansion", s2, 1);
                                                                    return;
                                                                default:
                                                                    return;
                                                            }
                                                        case "separation":
                                                            if ("oceanmonument".equals(s)) {
                                                                WorldGenSettingsFix.StructureFeatureConfiguration worldgensettingsfix$structurefeatureconfiguration = map.getOrDefault(
                                                                    "minecraft:monument", DEFAULTS.get("minecraft:monument")
                                                                );
                                                                int i = getInt(s2, worldgensettingsfix$structurefeatureconfiguration.separation, 1);
                                                                map.put(
                                                                    "minecraft:monument",
                                                                    new WorldGenSettingsFix.StructureFeatureConfiguration(
                                                                        i,
                                                                        worldgensettingsfix$structurefeatureconfiguration.separation,
                                                                        worldgensettingsfix$structurefeatureconfiguration.salt
                                                                    )
                                                                );
                                                            }

                                                            return;
                                                        case "spacing":
                                                            if ("oceanmonument".equals(s)) {
                                                                setSpacing(map, "minecraft:monument", s2, 1);
                                                            }

                                                            return;
                                                    }
                                                }
                                            }
                                        )
                                )
                    )
            );
        Builder<Dynamic<T>, Dynamic<T>> builder = ImmutableMap.builder();
        builder.put(
            pGeneratorOptions.createString("structures"),
            pGeneratorOptions.createMap(
                map.entrySet()
                    .stream()
                    .collect(Collectors.toMap(p_17225_ -> pGeneratorOptions.createString(p_17225_.getKey()), p_17222_ -> p_17222_.getValue().serialize(pOps)))
            )
        );
        if (mutableboolean.isTrue()) {
            builder.put(
                pGeneratorOptions.createString("stronghold"),
                pGeneratorOptions.createMap(
                    ImmutableMap.of(
                        pGeneratorOptions.createString("distance"),
                        pGeneratorOptions.createInt(mutableint.getValue()),
                        pGeneratorOptions.createString("spread"),
                        pGeneratorOptions.createInt(mutableint1.getValue()),
                        pGeneratorOptions.createString("count"),
                        pGeneratorOptions.createInt(mutableint2.getValue())
                    )
                )
            );
        }

        return builder.build();
    }

    private static int getInt(String pString, int pDefaultValue) {
        return NumberUtils.toInt(pString, pDefaultValue);
    }

    private static int getInt(String pString, int pDefaultValue, int pMinValue) {
        return Math.max(pMinValue, getInt(pString, pDefaultValue));
    }

    private static void setSpacing(Map<String, WorldGenSettingsFix.StructureFeatureConfiguration> pMap, String pStructure, String pSpacing, int pMinValue) {
        WorldGenSettingsFix.StructureFeatureConfiguration worldgensettingsfix$structurefeatureconfiguration = pMap.getOrDefault(
            pStructure, DEFAULTS.get(pStructure)
        );
        int i = getInt(pSpacing, worldgensettingsfix$structurefeatureconfiguration.spacing, pMinValue);
        pMap.put(
            pStructure,
            new WorldGenSettingsFix.StructureFeatureConfiguration(
                i, worldgensettingsfix$structurefeatureconfiguration.separation, worldgensettingsfix$structurefeatureconfiguration.salt
            )
        );
    }

    static final class StructureFeatureConfiguration {
        public static final Codec<WorldGenSettingsFix.StructureFeatureConfiguration> CODEC = RecordCodecBuilder.create(
            p_17279_ -> p_17279_.group(
                        Codec.INT.fieldOf("spacing").forGetter(p_145830_ -> p_145830_.spacing),
                        Codec.INT.fieldOf("separation").forGetter(p_145828_ -> p_145828_.separation),
                        Codec.INT.fieldOf("salt").forGetter(p_145826_ -> p_145826_.salt)
                    )
                    .apply(p_17279_, WorldGenSettingsFix.StructureFeatureConfiguration::new)
        );
        final int spacing;
        final int separation;
        final int salt;

        public StructureFeatureConfiguration(int pSpacing, int pSeparation, int pSalt) {
            this.spacing = pSpacing;
            this.separation = pSeparation;
            this.salt = pSalt;
        }

        public <T> Dynamic<T> serialize(DynamicOps<T> pOps) {
            return new Dynamic<>(pOps, CODEC.encodeStart(pOps, this).result().orElse(pOps.emptyMap()));
        }
    }
}