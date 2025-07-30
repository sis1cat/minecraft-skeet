package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.Util;

public class StructureSettingsFlattenFix extends DataFix {
    public StructureSettingsFlattenFix(Schema pOutputSchema) {
        super(pOutputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.WORLD_GEN_SETTINGS);
        OpticFinder<?> opticfinder = type.findField("dimensions");
        return this.fixTypeEverywhereTyped(
            "StructureSettingsFlatten",
            type,
            p_204003_ -> p_204003_.updateTyped(
                    opticfinder,
                    p_308999_ -> Util.writeAndReadTypedOrThrow(p_308999_, opticfinder.type(), p_308997_ -> p_308997_.updateMapValues(StructureSettingsFlattenFix::fixDimension))
                )
        );
    }

    private static Pair<Dynamic<?>, Dynamic<?>> fixDimension(Pair<Dynamic<?>, Dynamic<?>> pDimensions) {
        Dynamic<?> dynamic = pDimensions.getSecond();
        return Pair.of(
            pDimensions.getFirst(),
            dynamic.update(
                "generator", p_204018_ -> p_204018_.update("settings", p_204020_ -> p_204020_.update("structures", StructureSettingsFlattenFix::fixStructures))
            )
        );
    }

    private static Dynamic<?> fixStructures(Dynamic<?> pDynamic) {
        Dynamic<?> dynamic = pDynamic.get("structures")
            .orElseEmptyMap()
            .updateMapValues(p_204010_ -> p_204010_.mapSecond(p_204013_ -> p_204013_.set("type", pDynamic.createString("minecraft:random_spread"))));
        return DataFixUtils.orElse(
            pDynamic.get("stronghold")
                .result()
                .map(p_207675_ -> dynamic.set("minecraft:stronghold", p_207675_.set("type", pDynamic.createString("minecraft:concentric_rings")))),
            dynamic
        );
    }
}