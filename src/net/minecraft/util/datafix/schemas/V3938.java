package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3938 extends NamespacedSchema {
    public V3938(int p_345209_, Schema p_342762_) {
        super(p_345209_, p_342762_);
    }

    protected static TypeTemplate abstractArrow(Schema pSchema) {
        return DSL.optionalFields(
            "inBlockState", References.BLOCK_STATE.in(pSchema), "item", References.ITEM_STACK.in(pSchema), "weapon", References.ITEM_STACK.in(pSchema)
        );
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerEntities(pSchema);
        pSchema.register(map, "minecraft:spectral_arrow", () -> abstractArrow(pSchema));
        pSchema.register(map, "minecraft:arrow", () -> abstractArrow(pSchema));
        return map;
    }
}