package net.minecraft.world.inventory;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.crafting.RecipeHolder;

public abstract class RecipeBookMenu extends AbstractContainerMenu {
    public RecipeBookMenu(MenuType<?> p_40115_, int p_40116_) {
        super(p_40115_, p_40116_);
    }

    public abstract RecipeBookMenu.PostPlaceAction handlePlacement(
        boolean pUseMaxItems, boolean pIsCreative, RecipeHolder<?> pRecipe, ServerLevel pLevel, Inventory pPlayerInventory
    );

    public abstract void fillCraftSlotsStackedContents(StackedItemContents pStackedItemContents);

    public abstract RecipeBookType getRecipeBookType();

    public static enum PostPlaceAction {
        NOTHING,
        PLACE_GHOST_RECIPE;
    }
}