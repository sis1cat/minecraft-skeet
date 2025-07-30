package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Set;

public class WallPropertyFix extends DataFix {
    private static final Set<String> WALL_BLOCKS = ImmutableSet.of(
        "minecraft:andesite_wall",
        "minecraft:brick_wall",
        "minecraft:cobblestone_wall",
        "minecraft:diorite_wall",
        "minecraft:end_stone_brick_wall",
        "minecraft:granite_wall",
        "minecraft:mossy_cobblestone_wall",
        "minecraft:mossy_stone_brick_wall",
        "minecraft:nether_brick_wall",
        "minecraft:prismarine_wall",
        "minecraft:red_nether_brick_wall",
        "minecraft:red_sandstone_wall",
        "minecraft:sandstone_wall",
        "minecraft:stone_brick_wall"
    );

    public WallPropertyFix(Schema pOutputSchema, boolean pChangesType) {
        super(pOutputSchema, pChangesType);
    }

    @Override
    public TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            "WallPropertyFix",
            this.getInputSchema().getType(References.BLOCK_STATE),
            p_17157_ -> p_17157_.update(DSL.remainderFinder(), WallPropertyFix::upgradeBlockStateTag)
        );
    }

    private static String mapProperty(String pProperty) {
        return "true".equals(pProperty) ? "low" : "none";
    }

    private static <T> Dynamic<T> fixWallProperty(Dynamic<T> pDynamic, String pKey) {
        return pDynamic.update(
            pKey, p_326661_ -> DataFixUtils.orElse(p_326661_.asString().result().map(WallPropertyFix::mapProperty).map(p_326661_::createString), p_326661_)
        );
    }

    private static <T> Dynamic<T> upgradeBlockStateTag(Dynamic<T> pDynamic) {
        boolean flag = pDynamic.get("Name").asString().result().filter(WALL_BLOCKS::contains).isPresent();
        return !flag ? pDynamic : pDynamic.update("Properties", p_17166_ -> {
            Dynamic<?> dynamic = fixWallProperty(p_17166_, "east");
            dynamic = fixWallProperty(dynamic, "west");
            dynamic = fixWallProperty(dynamic, "north");
            return fixWallProperty(dynamic, "south");
        });
    }
}