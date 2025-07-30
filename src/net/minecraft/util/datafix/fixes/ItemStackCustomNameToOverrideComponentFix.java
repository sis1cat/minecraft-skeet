package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.util.datafix.ComponentDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class ItemStackCustomNameToOverrideComponentFix extends DataFix {
    private static final Set<String> MAP_NAMES = Set.of(
        "filled_map.buried_treasure",
        "filled_map.explorer_jungle",
        "filled_map.explorer_swamp",
        "filled_map.mansion",
        "filled_map.monument",
        "filled_map.trial_chambers",
        "filled_map.village_desert",
        "filled_map.village_plains",
        "filled_map.village_savanna",
        "filled_map.village_snowy",
        "filled_map.village_taiga"
    );

    public ItemStackCustomNameToOverrideComponentFix(Schema pOutputSchema) {
        super(pOutputSchema, false);
    }

    @Override
    public final TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.ITEM_STACK);
        OpticFinder<Pair<String, String>> opticfinder = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
        OpticFinder<?> opticfinder1 = type.findField("components");
        return this.fixTypeEverywhereTyped(
            "ItemStack custom_name to item_name component fix",
            type,
            p_328527_ -> {
                Optional<Pair<String, String>> optional = p_328527_.getOptional(opticfinder);
                Optional<String> optional1 = optional.map(Pair::getSecond);
                if (optional1.filter(p_329654_ -> p_329654_.equals("minecraft:white_banner")).isPresent()) {
                    return p_328527_.updateTyped(
                        opticfinder1, p_331207_ -> p_331207_.update(DSL.remainderFinder(), ItemStackCustomNameToOverrideComponentFix::fixBanner)
                    );
                } else {
                    return optional1.filter(p_336047_ -> p_336047_.equals("minecraft:filled_map")).isPresent()
                        ? p_328527_.updateTyped(
                            opticfinder1, p_332534_ -> p_332534_.update(DSL.remainderFinder(), ItemStackCustomNameToOverrideComponentFix::fixMap)
                        )
                        : p_328527_;
                }
            }
        );
    }

    private static <T> Dynamic<T> fixMap(Dynamic<T> pData) {
        return fixCustomName(pData, MAP_NAMES::contains);
    }

    private static <T> Dynamic<T> fixBanner(Dynamic<T> pData) {
        return fixCustomName(pData, p_329571_ -> p_329571_.equals("block.minecraft.ominous_banner"));
    }

    private static <T> Dynamic<T> fixCustomName(Dynamic<T> pData, Predicate<String> pShouldFix) {
        OptionalDynamic<T> optionaldynamic = pData.get("minecraft:custom_name");
        Optional<String> optional = optionaldynamic.asString().result().flatMap(ComponentDataFixUtils::extractTranslationString).filter(pShouldFix);
        return optional.isPresent() ? pData.renameField("minecraft:custom_name", "minecraft:item_name") : pData;
    }
}