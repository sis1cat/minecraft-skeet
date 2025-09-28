package net.minecraft.data.recipes;

import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;

public interface RecipeOutput {
    void accept(ResourceKey<Recipe<?>> pKey, Recipe<?> pRecipe, @Nullable AdvancementHolder pAdvancement);

    Advancement.Builder advancement();

    void includeRootAdvancement();
}