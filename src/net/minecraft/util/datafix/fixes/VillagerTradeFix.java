package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class VillagerTradeFix extends DataFix {
    public VillagerTradeFix(Schema pOutputSchema) {
        super(pOutputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.VILLAGER_TRADE);
        OpticFinder<?> opticfinder = type.findField("buy");
        OpticFinder<?> opticfinder1 = type.findField("buyB");
        OpticFinder<?> opticfinder2 = type.findField("sell");
        OpticFinder<Pair<String, String>> opticfinder3 = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
        Function<Typed<?>, Typed<?>> function = p_17150_ -> this.updateItemStack(opticfinder3, p_17150_);
        return this.fixTypeEverywhereTyped(
            "Villager trade fix",
            type,
            p_145788_ -> p_145788_.updateTyped(opticfinder, function).updateTyped(opticfinder1, function).updateTyped(opticfinder2, function)
        );
    }

    private Typed<?> updateItemStack(OpticFinder<Pair<String, String>> pId, Typed<?> pTyped) {
        return pTyped.update(
            pId, p_17145_ -> p_17145_.mapSecond(p_145790_ -> Objects.equals(p_145790_, "minecraft:carved_pumpkin") ? "minecraft:pumpkin" : p_145790_)
        );
    }
}