package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.stream.Stream;

public class CustomModelDataExpandFix extends DataFix {
    public CustomModelDataExpandFix(Schema pOutputSchema) {
        super(pOutputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.DATA_COMPONENTS);
        return this.fixTypeEverywhereTyped(
            "Custom Model Data expansion",
            type,
            p_376786_ -> p_376786_.update(DSL.remainderFinder(), p_378395_ -> p_378395_.update("minecraft:custom_model_data", p_376044_ -> {
                        float f = p_376044_.asNumber(Float.valueOf(0.0F)).floatValue();
                        return p_376044_.createMap(Map.of(p_376044_.createString("floats"), p_376044_.createList(Stream.of(p_376044_.createFloat(f)))));
                    }))
        );
    }
}