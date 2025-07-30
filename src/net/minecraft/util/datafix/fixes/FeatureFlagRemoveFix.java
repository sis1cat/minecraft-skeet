package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FeatureFlagRemoveFix extends DataFix {
    private final String name;
    private final Set<String> flagsToRemove;

    public FeatureFlagRemoveFix(Schema pOutputSchema, String pName, Set<String> pFlagsToRemove) {
        super(pOutputSchema, false);
        this.name = pName;
        this.flagsToRemove = pFlagsToRemove;
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            this.name, this.getInputSchema().getType(References.LEVEL), p_277407_ -> p_277407_.update(DSL.remainderFinder(), this::fixTag)
        );
    }

    private <T> Dynamic<T> fixTag(Dynamic<T> pTag) {
        List<Dynamic<T>> list = pTag.get("removed_features").asStream().collect(Collectors.toCollection(ArrayList::new));
        Dynamic<T> dynamic = pTag.update(
            "enabled_features", p_326589_ -> DataFixUtils.orElse(p_326589_.asStreamOpt().result().map(p_277400_ -> p_277400_.filter(p_326586_ -> {
                        Optional<String> optional = p_326586_.asString().result();
                        if (optional.isEmpty()) {
                            return true;
                        } else {
                            boolean flag = this.flagsToRemove.contains(optional.get());
                            if (flag) {
                                list.add(pTag.createString(optional.get()));
                            }

                            return !flag;
                        }
                    })).map(pTag::createList), p_326589_)
        );
        if (!list.isEmpty()) {
            dynamic = dynamic.set("removed_features", pTag.createList(list.stream()));
        }

        return dynamic;
    }
}