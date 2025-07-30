package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ApplyExplosionDecay extends LootItemConditionalFunction {
    public static final MapCodec<ApplyExplosionDecay> CODEC = RecordCodecBuilder.mapCodec(
        p_297802_ -> commonFields(p_297802_).apply(p_297802_, ApplyExplosionDecay::new)
    );

    private ApplyExplosionDecay(List<LootItemCondition> pConditions) {
        super(pConditions);
    }

    @Override
    public LootItemFunctionType<ApplyExplosionDecay> getType() {
        return LootItemFunctions.EXPLOSION_DECAY;
    }

    @Override
    public ItemStack run(ItemStack pStack, LootContext pContext) {
        Float f = pContext.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS);
        if (f != null) {
            RandomSource randomsource = pContext.getRandom();
            float f1 = 1.0F / f;
            int i = pStack.getCount();
            int j = 0;

            for (int k = 0; k < i; k++) {
                if (randomsource.nextFloat() <= f1) {
                    j++;
                }
            }

            pStack.setCount(j);
        }

        return pStack;
    }

    public static LootItemConditionalFunction.Builder<?> explosionDecay() {
        return simpleBuilder(ApplyExplosionDecay::new);
    }
}