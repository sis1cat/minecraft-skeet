package net.minecraft.world.level.storage.loot.functions;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

public class SmeltItemFunction extends LootItemConditionalFunction {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<SmeltItemFunction> CODEC = RecordCodecBuilder.mapCodec(
        p_298512_ -> commonFields(p_298512_).apply(p_298512_, SmeltItemFunction::new)
    );

    private SmeltItemFunction(List<LootItemCondition> pConditions) {
        super(pConditions);
    }

    @Override
    public LootItemFunctionType<SmeltItemFunction> getType() {
        return LootItemFunctions.FURNACE_SMELT;
    }

    @Override
    public ItemStack run(ItemStack pStack, LootContext pContext) {
        if (pStack.isEmpty()) {
            return pStack;
        } else {
            SingleRecipeInput singlerecipeinput = new SingleRecipeInput(pStack);
            Optional<RecipeHolder<SmeltingRecipe>> optional = pContext.getLevel()
                .recipeAccess()
                .getRecipeFor(RecipeType.SMELTING, singlerecipeinput, pContext.getLevel());
            if (optional.isPresent()) {
                ItemStack itemstack = optional.get().value().assemble(singlerecipeinput, pContext.getLevel().registryAccess());
                if (!itemstack.isEmpty()) {
                    return itemstack.copyWithCount(pStack.getCount());
                }
            }

            LOGGER.warn("Couldn't smelt {} because there is no smelting recipe", pStack);
            return pStack;
        }
    }

    public static LootItemConditionalFunction.Builder<?> smelted() {
        return simpleBuilder(SmeltItemFunction::new);
    }
}