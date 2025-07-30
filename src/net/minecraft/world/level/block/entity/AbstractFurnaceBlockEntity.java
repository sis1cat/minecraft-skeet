package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap.Entry;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractFurnaceBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, RecipeCraftingHolder, StackedContentsCompatible {
    protected static final int SLOT_INPUT = 0;
    protected static final int SLOT_FUEL = 1;
    protected static final int SLOT_RESULT = 2;
    public static final int DATA_LIT_TIME = 0;
    private static final int[] SLOTS_FOR_UP = new int[]{0};
    private static final int[] SLOTS_FOR_DOWN = new int[]{2, 1};
    private static final int[] SLOTS_FOR_SIDES = new int[]{1};
    public static final int DATA_LIT_DURATION = 1;
    public static final int DATA_COOKING_PROGRESS = 2;
    public static final int DATA_COOKING_TOTAL_TIME = 3;
    public static final int NUM_DATA_VALUES = 4;
    public static final int BURN_TIME_STANDARD = 200;
    public static final int BURN_COOL_SPEED = 2;
    protected NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    int litTimeRemaining;
    int litTotalTime;
    int cookingTimer;
    int cookingTotalTime;
    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int p_58431_) {
            switch (p_58431_) {
                case 0:
                    return AbstractFurnaceBlockEntity.this.litTimeRemaining;
                case 1:
                    return AbstractFurnaceBlockEntity.this.litTotalTime;
                case 2:
                    return AbstractFurnaceBlockEntity.this.cookingTimer;
                case 3:
                    return AbstractFurnaceBlockEntity.this.cookingTotalTime;
                default:
                    return 0;
            }
        }

        @Override
        public void set(int p_58433_, int p_58434_) {
            switch (p_58433_) {
                case 0:
                    AbstractFurnaceBlockEntity.this.litTimeRemaining = p_58434_;
                    break;
                case 1:
                    AbstractFurnaceBlockEntity.this.litTotalTime = p_58434_;
                    break;
                case 2:
                    AbstractFurnaceBlockEntity.this.cookingTimer = p_58434_;
                    break;
                case 3:
                    AbstractFurnaceBlockEntity.this.cookingTotalTime = p_58434_;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };
    private final Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> recipesUsed = new Reference2IntOpenHashMap<>();
    private final RecipeManager.CachedCheck<SingleRecipeInput, ? extends AbstractCookingRecipe> quickCheck;

    protected AbstractFurnaceBlockEntity(
        BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState, RecipeType<? extends AbstractCookingRecipe> pRecipeType
    ) {
        super(pType, pPos, pBlockState);
        this.quickCheck = RecipeManager.createCheck((RecipeType)pRecipeType);
    }

    private boolean isLit() {
        return this.litTimeRemaining > 0;
    }

    @Override
    protected void loadAdditional(CompoundTag p_335441_, HolderLookup.Provider p_330623_) {
        super.loadAdditional(p_335441_, p_330623_);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(p_335441_, this.items, p_330623_);
        this.cookingTimer = p_335441_.getShort("cooking_time_spent");
        this.cookingTotalTime = p_335441_.getShort("cooking_total_time");
        this.litTimeRemaining = p_335441_.getShort("lit_time_remaining");
        this.litTotalTime = p_335441_.getShort("lit_total_time");
        CompoundTag compoundtag = p_335441_.getCompound("RecipesUsed");

        for (String s : compoundtag.getAllKeys()) {
            this.recipesUsed.put(ResourceKey.create(Registries.RECIPE, ResourceLocation.parse(s)), compoundtag.getInt(s));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag p_187452_, HolderLookup.Provider p_330192_) {
        super.saveAdditional(p_187452_, p_330192_);
        p_187452_.putShort("cooking_time_spent", (short)this.cookingTimer);
        p_187452_.putShort("cooking_total_time", (short)this.cookingTotalTime);
        p_187452_.putShort("lit_time_remaining", (short)this.litTimeRemaining);
        p_187452_.putShort("lit_total_time", (short)this.litTotalTime);
        ContainerHelper.saveAllItems(p_187452_, this.items, p_330192_);
        CompoundTag compoundtag = new CompoundTag();
        this.recipesUsed.forEach((p_360478_, p_360479_) -> compoundtag.putInt(p_360478_.location().toString(), p_360479_));
        p_187452_.put("RecipesUsed", compoundtag);
    }

    public static void serverTick(ServerLevel pLevel, BlockPos pPos, BlockState pState, AbstractFurnaceBlockEntity pFurnace) {
        boolean flag = pFurnace.isLit();
        boolean flag1 = false;
        if (pFurnace.isLit()) {
            pFurnace.litTimeRemaining--;
        }

        ItemStack itemstack = pFurnace.items.get(1);
        ItemStack itemstack1 = pFurnace.items.get(0);
        boolean flag2 = !itemstack1.isEmpty();
        boolean flag3 = !itemstack.isEmpty();
        if (pFurnace.isLit() || flag3 && flag2) {
            SingleRecipeInput singlerecipeinput = new SingleRecipeInput(itemstack1);
            RecipeHolder<? extends AbstractCookingRecipe> recipeholder;
            if (flag2) {
                recipeholder = pFurnace.quickCheck.getRecipeFor(singlerecipeinput, pLevel).orElse(null);
            } else {
                recipeholder = null;
            }

            int i = pFurnace.getMaxStackSize();
            if (!pFurnace.isLit() && canBurn(pLevel.registryAccess(), recipeholder, singlerecipeinput, pFurnace.items, i)) {
                pFurnace.litTimeRemaining = pFurnace.getBurnDuration(pLevel.fuelValues(), itemstack);
                pFurnace.litTotalTime = pFurnace.litTimeRemaining;
                if (pFurnace.isLit()) {
                    flag1 = true;
                    if (flag3) {
                        Item item = itemstack.getItem();
                        itemstack.shrink(1);
                        if (itemstack.isEmpty()) {
                            pFurnace.items.set(1, item.getCraftingRemainder());
                        }
                    }
                }
            }

            if (pFurnace.isLit() && canBurn(pLevel.registryAccess(), recipeholder, singlerecipeinput, pFurnace.items, i)) {
                pFurnace.cookingTimer++;
                if (pFurnace.cookingTimer == pFurnace.cookingTotalTime) {
                    pFurnace.cookingTimer = 0;
                    pFurnace.cookingTotalTime = getTotalCookTime(pLevel, pFurnace);
                    if (burn(pLevel.registryAccess(), recipeholder, singlerecipeinput, pFurnace.items, i)) {
                        pFurnace.setRecipeUsed(recipeholder);
                    }

                    flag1 = true;
                }
            } else {
                pFurnace.cookingTimer = 0;
            }
        } else if (!pFurnace.isLit() && pFurnace.cookingTimer > 0) {
            pFurnace.cookingTimer = Mth.clamp(pFurnace.cookingTimer - 2, 0, pFurnace.cookingTotalTime);
        }

        if (flag != pFurnace.isLit()) {
            flag1 = true;
            pState = pState.setValue(AbstractFurnaceBlock.LIT, Boolean.valueOf(pFurnace.isLit()));
            pLevel.setBlock(pPos, pState, 3);
        }

        if (flag1) {
            setChanged(pLevel, pPos, pState);
        }
    }

    private static boolean canBurn(
        RegistryAccess pRegistryAccess,
        @Nullable RecipeHolder<? extends AbstractCookingRecipe> pRecipe,
        SingleRecipeInput pRecipeInput,
        NonNullList<ItemStack> pItems,
        int pMaxStackSize
    ) {
        if (!pItems.get(0).isEmpty() && pRecipe != null) {
            ItemStack itemstack = pRecipe.value().assemble(pRecipeInput, pRegistryAccess);
            if (itemstack.isEmpty()) {
                return false;
            } else {
                ItemStack itemstack1 = pItems.get(2);
                if (itemstack1.isEmpty()) {
                    return true;
                } else if (!ItemStack.isSameItemSameComponents(itemstack1, itemstack)) {
                    return false;
                } else {
                    return itemstack1.getCount() < pMaxStackSize && itemstack1.getCount() < itemstack1.getMaxStackSize()
                        ? true
                        : itemstack1.getCount() < itemstack.getMaxStackSize();
                }
            }
        } else {
            return false;
        }
    }

    private static boolean burn(
        RegistryAccess pRegistryAccess,
        @Nullable RecipeHolder<? extends AbstractCookingRecipe> pRecipe,
        SingleRecipeInput pRecipeInput,
        NonNullList<ItemStack> pItems,
        int pMaxStackSize
    ) {
        if (pRecipe != null && canBurn(pRegistryAccess, pRecipe, pRecipeInput, pItems, pMaxStackSize)) {
            ItemStack itemstack = pItems.get(0);
            ItemStack itemstack1 = pRecipe.value().assemble(pRecipeInput, pRegistryAccess);
            ItemStack itemstack2 = pItems.get(2);
            if (itemstack2.isEmpty()) {
                pItems.set(2, itemstack1.copy());
            } else if (ItemStack.isSameItemSameComponents(itemstack2, itemstack1)) {
                itemstack2.grow(1);
            }

            if (itemstack.is(Blocks.WET_SPONGE.asItem()) && !pItems.get(1).isEmpty() && pItems.get(1).is(Items.BUCKET)) {
                pItems.set(1, new ItemStack(Items.WATER_BUCKET));
            }

            itemstack.shrink(1);
            return true;
        } else {
            return false;
        }
    }

    protected int getBurnDuration(FuelValues pFuelValues, ItemStack pStack) {
        return pFuelValues.burnDuration(pStack);
    }

    private static int getTotalCookTime(ServerLevel pLevel, AbstractFurnaceBlockEntity pFurnace) {
        SingleRecipeInput singlerecipeinput = new SingleRecipeInput(pFurnace.getItem(0));
        return pFurnace.quickCheck.getRecipeFor(singlerecipeinput, pLevel).map(p_360485_ -> p_360485_.value().cookingTime()).orElse(200);
    }

    @Override
    public int[] getSlotsForFace(Direction pSide) {
        if (pSide == Direction.DOWN) {
            return SLOTS_FOR_DOWN;
        } else {
            return pSide == Direction.UP ? SLOTS_FOR_UP : SLOTS_FOR_SIDES;
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int pIndex, ItemStack pItemStack, @Nullable Direction pDirection) {
        return this.canPlaceItem(pIndex, pItemStack);
    }

    @Override
    public boolean canTakeItemThroughFace(int pIndex, ItemStack pStack, Direction pDirection) {
        return pDirection == Direction.DOWN && pIndex == 1 ? pStack.is(Items.WATER_BUCKET) || pStack.is(Items.BUCKET) : true;
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> p_327930_) {
        this.items = p_327930_;
    }

    @Override
    public void setItem(int pIndex, ItemStack pStack) {
        ItemStack itemstack = this.items.get(pIndex);
        boolean flag = !pStack.isEmpty() && ItemStack.isSameItemSameComponents(itemstack, pStack);
        this.items.set(pIndex, pStack);
        pStack.limitSize(this.getMaxStackSize(pStack));
        if (pIndex == 0 && !flag && this.level instanceof ServerLevel serverlevel) {
            this.cookingTotalTime = getTotalCookTime(serverlevel, this);
            this.cookingTimer = 0;
            this.setChanged();
        }
    }

    @Override
    public boolean canPlaceItem(int pIndex, ItemStack pStack) {
        if (pIndex == 2) {
            return false;
        } else if (pIndex != 1) {
            return true;
        } else {
            ItemStack itemstack = this.items.get(1);
            return this.level.fuelValues().isFuel(pStack) || pStack.is(Items.BUCKET) && !itemstack.is(Items.BUCKET);
        }
    }

    @Override
    public void setRecipeUsed(@Nullable RecipeHolder<?> p_297739_) {
        if (p_297739_ != null) {
            ResourceKey<Recipe<?>> resourcekey = p_297739_.id();
            this.recipesUsed.addTo(resourcekey, 1);
        }
    }

    @Nullable
    @Override
    public RecipeHolder<?> getRecipeUsed() {
        return null;
    }

    @Override
    public void awardUsedRecipes(Player p_58396_, List<ItemStack> p_282202_) {
    }

    public void awardUsedRecipesAndPopExperience(ServerPlayer pPlayer) {
        List<RecipeHolder<?>> list = this.getRecipesToAwardAndPopExperience(pPlayer.serverLevel(), pPlayer.position());
        pPlayer.awardRecipes(list);

        for (RecipeHolder<?> recipeholder : list) {
            if (recipeholder != null) {
                pPlayer.triggerRecipeCrafted(recipeholder, this.items);
            }
        }

        this.recipesUsed.clear();
    }

    public List<RecipeHolder<?>> getRecipesToAwardAndPopExperience(ServerLevel pLevel, Vec3 pPopVec) {
        List<RecipeHolder<?>> list = Lists.newArrayList();

        for (Entry<ResourceKey<Recipe<?>>> entry : this.recipesUsed.reference2IntEntrySet()) {
            pLevel.recipeAccess().byKey(entry.getKey()).ifPresent(p_360484_ -> {
                list.add((RecipeHolder<?>)p_360484_);
                createExperience(pLevel, pPopVec, entry.getIntValue(), ((AbstractCookingRecipe)p_360484_.value()).experience());
            });
        }

        return list;
    }

    private static void createExperience(ServerLevel pLevel, Vec3 pPopVec, int pRecipeIndex, float pExperience) {
        int i = Mth.floor((float)pRecipeIndex * pExperience);
        float f = Mth.frac((float)pRecipeIndex * pExperience);
        if (f != 0.0F && Math.random() < (double)f) {
            i++;
        }

        ExperienceOrb.award(pLevel, pPopVec, i);
    }

    @Override
    public void fillStackedContents(StackedItemContents p_363325_) {
        for (ItemStack itemstack : this.items) {
            p_363325_.accountStack(itemstack);
        }
    }
}