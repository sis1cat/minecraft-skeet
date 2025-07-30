package net.minecraft.util.datafix.fixes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class SavedDataFeaturePoolElementFix extends DataFix {
    private static final Pattern INDEX_PATTERN = Pattern.compile("\\[(\\d+)\\]");
    private static final Set<String> PIECE_TYPE = Sets.newHashSet(
        "minecraft:jigsaw", "minecraft:nvi", "minecraft:pcp", "minecraft:bastionremnant", "minecraft:runtime"
    );
    private static final Set<String> FEATURES = Sets.newHashSet("minecraft:tree", "minecraft:flower", "minecraft:block_pile", "minecraft:random_patch");

    public SavedDataFeaturePoolElementFix(Schema pOutputSchema) {
        super(pOutputSchema, false);
    }

    @Override
    public TypeRewriteRule makeRule() {
        return this.writeFixAndRead(
            "SavedDataFeaturePoolElementFix",
            this.getInputSchema().getType(References.STRUCTURE_FEATURE),
            this.getOutputSchema().getType(References.STRUCTURE_FEATURE),
            SavedDataFeaturePoolElementFix::fixTag
        );
    }

    private static <T> Dynamic<T> fixTag(Dynamic<T> pTag) {
        return pTag.update("Children", SavedDataFeaturePoolElementFix::updateChildren);
    }

    private static <T> Dynamic<T> updateChildren(Dynamic<T> pData) {
        return pData.asStreamOpt().map(SavedDataFeaturePoolElementFix::updateChildren).map(pData::createList).result().orElse(pData);
    }

    private static Stream<? extends Dynamic<?>> updateChildren(Stream<? extends Dynamic<?>> pChildren) {
        return pChildren.map(
            p_145667_ -> {
                String s = p_145667_.get("id").asString("");
                if (!PIECE_TYPE.contains(s)) {
                    return p_145667_;
                } else {
                    OptionalDynamic<?> optionaldynamic = p_145667_.get("pool_element");
                    return !optionaldynamic.get("element_type").asString("").equals("minecraft:feature_pool_element")
                        ? p_145667_
                        : p_145667_.update("pool_element", p_145669_ -> p_145669_.update("feature", SavedDataFeaturePoolElementFix::fixFeature));
                }
            }
        );
    }

    private static <T> OptionalDynamic<T> get(Dynamic<T> pDynamic, String... pPath) {
        if (pPath.length == 0) {
            throw new IllegalArgumentException("Missing path");
        } else {
            OptionalDynamic<T> optionaldynamic = pDynamic.get(pPath[0]);

            for (int i = 1; i < pPath.length; i++) {
                String s = pPath[i];
                Matcher matcher = INDEX_PATTERN.matcher(s);
                if (matcher.matches()) {
                    int j = Integer.parseInt(matcher.group(1));
                    List<? extends Dynamic<T>> list = optionaldynamic.asList(Function.identity());
                    if (j >= 0 && j < list.size()) {
                        optionaldynamic = new OptionalDynamic<>(pDynamic.getOps(), DataResult.success((Dynamic<T>)list.get(j)));
                    } else {
                        optionaldynamic = new OptionalDynamic<>(pDynamic.getOps(), DataResult.error(() -> "Missing id:" + j));
                    }
                } else {
                    optionaldynamic = optionaldynamic.get(s);
                }
            }

            return optionaldynamic;
        }
    }

    @VisibleForTesting
    protected static Dynamic<?> fixFeature(Dynamic<?> pDynamic) {
        Optional<String> optional = getReplacement(
            get(pDynamic, "type").asString(""),
            get(pDynamic, "name").asString(""),
            get(pDynamic, "config", "state_provider", "type").asString(""),
            get(pDynamic, "config", "state_provider", "state", "Name").asString(""),
            get(pDynamic, "config", "state_provider", "entries", "[0]", "data", "Name").asString(""),
            get(pDynamic, "config", "foliage_placer", "type").asString(""),
            get(pDynamic, "config", "leaves_provider", "state", "Name").asString("")
        );
        return optional.isPresent() ? pDynamic.createString(optional.get()) : pDynamic;
    }

    private static Optional<String> getReplacement(
        String pType, String pName, String pStateProviderType, String pState, String pStateProviderName, String pFoliagePlacerType, String pLeavesState
    ) {
        String s;
        if (!pType.isEmpty()) {
            s = pType;
        } else {
            if (pName.isEmpty()) {
                return Optional.empty();
            }

            if ("minecraft:normal_tree".equals(pName)) {
                s = "minecraft:tree";
            } else {
                s = pName;
            }
        }

        if (FEATURES.contains(s)) {
            if ("minecraft:random_patch".equals(s)) {
                if ("minecraft:simple_state_provider".equals(pStateProviderType)) {
                    if ("minecraft:sweet_berry_bush".equals(pState)) {
                        return Optional.of("minecraft:patch_berry_bush");
                    }

                    if ("minecraft:cactus".equals(pState)) {
                        return Optional.of("minecraft:patch_cactus");
                    }
                } else if ("minecraft:weighted_state_provider".equals(pStateProviderType) && ("minecraft:grass".equals(pStateProviderName) || "minecraft:fern".equals(pStateProviderName))
                    )
                 {
                    return Optional.of("minecraft:patch_taiga_grass");
                }
            } else if ("minecraft:block_pile".equals(s)) {
                if (!"minecraft:simple_state_provider".equals(pStateProviderType) && !"minecraft:rotated_block_provider".equals(pStateProviderType)) {
                    if ("minecraft:weighted_state_provider".equals(pStateProviderType)) {
                        if ("minecraft:packed_ice".equals(pStateProviderName) || "minecraft:blue_ice".equals(pStateProviderName)) {
                            return Optional.of("minecraft:pile_ice");
                        }

                        if ("minecraft:jack_o_lantern".equals(pStateProviderName) || "minecraft:pumpkin".equals(pStateProviderName)) {
                            return Optional.of("minecraft:pile_pumpkin");
                        }
                    }
                } else {
                    if ("minecraft:hay_block".equals(pState)) {
                        return Optional.of("minecraft:pile_hay");
                    }

                    if ("minecraft:melon".equals(pState)) {
                        return Optional.of("minecraft:pile_melon");
                    }

                    if ("minecraft:snow".equals(pState)) {
                        return Optional.of("minecraft:pile_snow");
                    }
                }
            } else {
                if ("minecraft:flower".equals(s)) {
                    return Optional.of("minecraft:flower_plain");
                }

                if ("minecraft:tree".equals(s)) {
                    if ("minecraft:acacia_foliage_placer".equals(pFoliagePlacerType)) {
                        return Optional.of("minecraft:acacia");
                    }

                    if ("minecraft:blob_foliage_placer".equals(pFoliagePlacerType) && "minecraft:oak_leaves".equals(pLeavesState)) {
                        return Optional.of("minecraft:oak");
                    }

                    if ("minecraft:pine_foliage_placer".equals(pFoliagePlacerType)) {
                        return Optional.of("minecraft:pine");
                    }

                    if ("minecraft:spruce_foliage_placer".equals(pFoliagePlacerType)) {
                        return Optional.of("minecraft:spruce");
                    }
                }
            }
        }

        return Optional.empty();
    }
}