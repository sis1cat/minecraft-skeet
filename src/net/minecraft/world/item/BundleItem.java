package net.minecraft.world.item;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.math.Fraction;

public class BundleItem extends Item {
    public static final int MAX_SHOWN_GRID_ITEMS_X = 4;
    public static final int MAX_SHOWN_GRID_ITEMS_Y = 3;
    public static final int MAX_SHOWN_GRID_ITEMS = 12;
    public static final int OVERFLOWING_MAX_SHOWN_GRID_ITEMS = 11;
    private static final int FULL_BAR_COLOR = ARGB.colorFromFloat(1.0F, 1.0F, 0.33F, 0.33F);
    private static final int BAR_COLOR = ARGB.colorFromFloat(1.0F, 0.44F, 0.53F, 1.0F);
    private static final int TICKS_AFTER_FIRST_THROW = 10;
    private static final int TICKS_BETWEEN_THROWS = 2;
    private static final int TICKS_MAX_THROW_DURATION = 200;

    public BundleItem(Item.Properties p_150726_) {
        super(p_150726_);
    }

    public static float getFullnessDisplay(ItemStack pStack) {
        BundleContents bundlecontents = pStack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        return bundlecontents.weight().floatValue();
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack p_150733_, Slot p_150734_, ClickAction p_150735_, Player p_150736_) {
        BundleContents bundlecontents = p_150733_.get(DataComponents.BUNDLE_CONTENTS);
        if (bundlecontents == null) {
            return false;
        } else {
            ItemStack itemstack = p_150734_.getItem();
            BundleContents.Mutable bundlecontents$mutable = new BundleContents.Mutable(bundlecontents);
            if (p_150735_ == ClickAction.PRIMARY && !itemstack.isEmpty()) {
                if (bundlecontents$mutable.tryTransfer(p_150734_, p_150736_) > 0) {
                    playInsertSound(p_150736_);
                } else {
                    playInsertFailSound(p_150736_);
                }

                p_150733_.set(DataComponents.BUNDLE_CONTENTS, bundlecontents$mutable.toImmutable());
                this.broadcastChangesOnContainerMenu(p_150736_);
                return true;
            } else if (p_150735_ == ClickAction.SECONDARY && itemstack.isEmpty()) {
                ItemStack itemstack1 = bundlecontents$mutable.removeOne();
                if (itemstack1 != null) {
                    ItemStack itemstack2 = p_150734_.safeInsert(itemstack1);
                    if (itemstack2.getCount() > 0) {
                        bundlecontents$mutable.tryInsert(itemstack2);
                    } else {
                        playRemoveOneSound(p_150736_);
                    }
                }

                p_150733_.set(DataComponents.BUNDLE_CONTENTS, bundlecontents$mutable.toImmutable());
                this.broadcastChangesOnContainerMenu(p_150736_);
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack p_150742_, ItemStack p_150743_, Slot p_150744_, ClickAction p_150745_, Player p_150746_, SlotAccess p_150747_) {
        if (p_150745_ == ClickAction.PRIMARY && p_150743_.isEmpty()) {
            toggleSelectedItem(p_150742_, -1);
            return false;
        } else {
            BundleContents bundlecontents = p_150742_.get(DataComponents.BUNDLE_CONTENTS);
            if (bundlecontents == null) {
                return false;
            } else {
                BundleContents.Mutable bundlecontents$mutable = new BundleContents.Mutable(bundlecontents);
                if (p_150745_ == ClickAction.PRIMARY && !p_150743_.isEmpty()) {
                    if (p_150744_.allowModification(p_150746_) && bundlecontents$mutable.tryInsert(p_150743_) > 0) {
                        playInsertSound(p_150746_);
                    } else {
                        playInsertFailSound(p_150746_);
                    }

                    p_150742_.set(DataComponents.BUNDLE_CONTENTS, bundlecontents$mutable.toImmutable());
                    this.broadcastChangesOnContainerMenu(p_150746_);
                    return true;
                } else if (p_150745_ == ClickAction.SECONDARY && p_150743_.isEmpty()) {
                    if (p_150744_.allowModification(p_150746_)) {
                        ItemStack itemstack = bundlecontents$mutable.removeOne();
                        if (itemstack != null) {
                            playRemoveOneSound(p_150746_);
                            p_150747_.set(itemstack);
                        }
                    }

                    p_150742_.set(DataComponents.BUNDLE_CONTENTS, bundlecontents$mutable.toImmutable());
                    this.broadcastChangesOnContainerMenu(p_150746_);
                    return true;
                } else {
                    toggleSelectedItem(p_150742_, -1);
                    return false;
                }
            }
        }
    }

    @Override
    public InteractionResult use(Level p_150760_, Player p_150761_, InteractionHand p_150762_) {
        p_150761_.startUsingItem(p_150762_);
        return InteractionResult.SUCCESS;
    }

    private void dropContent(Level pLevel, Player pPlayer, ItemStack pStack) {
        if (this.dropContent(pStack, pPlayer)) {
            playDropContentsSound(pLevel, pPlayer);
            pPlayer.awardStat(Stats.ITEM_USED.get(this));
        }
    }

    @Override
    public boolean isBarVisible(ItemStack p_150769_) {
        BundleContents bundlecontents = p_150769_.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        return bundlecontents.weight().compareTo(Fraction.ZERO) > 0;
    }

    @Override
    public int getBarWidth(ItemStack p_150771_) {
        BundleContents bundlecontents = p_150771_.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        return Math.min(1 + Mth.mulAndTruncate(bundlecontents.weight(), 12), 13);
    }

    @Override
    public int getBarColor(ItemStack p_150773_) {
        BundleContents bundlecontents = p_150773_.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        return bundlecontents.weight().compareTo(Fraction.ONE) >= 0 ? FULL_BAR_COLOR : BAR_COLOR;
    }

    public static void toggleSelectedItem(ItemStack pBundle, int pSelectedItem) {
        BundleContents bundlecontents = pBundle.get(DataComponents.BUNDLE_CONTENTS);
        if (bundlecontents != null) {
            BundleContents.Mutable bundlecontents$mutable = new BundleContents.Mutable(bundlecontents);
            bundlecontents$mutable.toggleSelectedItem(pSelectedItem);
            pBundle.set(DataComponents.BUNDLE_CONTENTS, bundlecontents$mutable.toImmutable());
        }
    }

    public static boolean hasSelectedItem(ItemStack pBundle) {
        BundleContents bundlecontents = pBundle.get(DataComponents.BUNDLE_CONTENTS);
        return bundlecontents != null && bundlecontents.getSelectedItem() != -1;
    }

    public static int getSelectedItem(ItemStack pBundle) {
        BundleContents bundlecontents = pBundle.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        return bundlecontents.getSelectedItem();
    }

    public static ItemStack getSelectedItemStack(ItemStack pBundle) {
        BundleContents bundlecontents = pBundle.get(DataComponents.BUNDLE_CONTENTS);
        return bundlecontents != null && bundlecontents.getSelectedItem() != -1 ? bundlecontents.getItemUnsafe(bundlecontents.getSelectedItem()) : ItemStack.EMPTY;
    }

    public static int getNumberOfItemsToShow(ItemStack pBundle) {
        BundleContents bundlecontents = pBundle.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        return bundlecontents.getNumberOfItemsToShow();
    }

    private boolean dropContent(ItemStack pStack, Player pPlayer) {
        BundleContents bundlecontents = pStack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundlecontents != null && !bundlecontents.isEmpty()) {
            Optional<ItemStack> optional = removeOneItemFromBundle(pStack, pPlayer, bundlecontents);
            if (optional.isPresent()) {
                pPlayer.drop(optional.get(), true);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private static Optional<ItemStack> removeOneItemFromBundle(ItemStack pStack, Player pPlayer, BundleContents pBundleContents) {
        BundleContents.Mutable bundlecontents$mutable = new BundleContents.Mutable(pBundleContents);
        ItemStack itemstack = bundlecontents$mutable.removeOne();
        if (itemstack != null) {
            playRemoveOneSound(pPlayer);
            pStack.set(DataComponents.BUNDLE_CONTENTS, bundlecontents$mutable.toImmutable());
            return Optional.of(itemstack);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void onUseTick(Level p_369274_, LivingEntity p_365864_, ItemStack p_364728_, int p_366618_) {
        if (p_365864_ instanceof Player player) {
            int i = this.getUseDuration(p_364728_, p_365864_);
            boolean flag = p_366618_ == i;
            if (flag || p_366618_ < i - 10 && p_366618_ % 2 == 0) {
                this.dropContent(p_369274_, player, p_364728_);
            }
        }
    }

    @Override
    public int getUseDuration(ItemStack p_363914_, LivingEntity p_368133_) {
        return 200;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack p_376256_) {
        return ItemUseAnimation.BUNDLE;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack p_150775_) {
        return !p_150775_.has(DataComponents.HIDE_TOOLTIP) && !p_150775_.has(DataComponents.HIDE_ADDITIONAL_TOOLTIP)
            ? Optional.ofNullable(p_150775_.get(DataComponents.BUNDLE_CONTENTS)).map(BundleTooltip::new)
            : Optional.empty();
    }

    @Override
    public void onDestroyed(ItemEntity p_150728_) {
        BundleContents bundlecontents = p_150728_.getItem().get(DataComponents.BUNDLE_CONTENTS);
        if (bundlecontents != null) {
            p_150728_.getItem().set(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
            ItemUtils.onContainerDestroyed(p_150728_, bundlecontents.itemsCopy());
        }
    }

    public static List<BundleItem> getAllBundleItemColors() {
        return Stream.of(
                Items.BUNDLE,
                Items.WHITE_BUNDLE,
                Items.ORANGE_BUNDLE,
                Items.MAGENTA_BUNDLE,
                Items.LIGHT_BLUE_BUNDLE,
                Items.YELLOW_BUNDLE,
                Items.LIME_BUNDLE,
                Items.PINK_BUNDLE,
                Items.GRAY_BUNDLE,
                Items.LIGHT_GRAY_BUNDLE,
                Items.CYAN_BUNDLE,
                Items.BLACK_BUNDLE,
                Items.BROWN_BUNDLE,
                Items.GREEN_BUNDLE,
                Items.RED_BUNDLE,
                Items.BLUE_BUNDLE,
                Items.PURPLE_BUNDLE
            )
            .map(p_359381_ -> (BundleItem)p_359381_)
            .toList();
    }

    public static Item getByColor(DyeColor pColor) {
        return switch (pColor) {
            case WHITE -> Items.WHITE_BUNDLE;
            case ORANGE -> Items.ORANGE_BUNDLE;
            case MAGENTA -> Items.MAGENTA_BUNDLE;
            case LIGHT_BLUE -> Items.LIGHT_BLUE_BUNDLE;
            case YELLOW -> Items.YELLOW_BUNDLE;
            case LIME -> Items.LIME_BUNDLE;
            case PINK -> Items.PINK_BUNDLE;
            case GRAY -> Items.GRAY_BUNDLE;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_BUNDLE;
            case CYAN -> Items.CYAN_BUNDLE;
            case BLUE -> Items.BLUE_BUNDLE;
            case BROWN -> Items.BROWN_BUNDLE;
            case GREEN -> Items.GREEN_BUNDLE;
            case RED -> Items.RED_BUNDLE;
            case BLACK -> Items.BLACK_BUNDLE;
            case PURPLE -> Items.PURPLE_BUNDLE;
        };
    }

    private static void playRemoveOneSound(Entity pEntity) {
        pEntity.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + pEntity.level().getRandom().nextFloat() * 0.4F);
    }

    private static void playInsertSound(Entity pEntity) {
        pEntity.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + pEntity.level().getRandom().nextFloat() * 0.4F);
    }

    private static void playInsertFailSound(Entity pEntity) {
        pEntity.playSound(SoundEvents.BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
    }

    private static void playDropContentsSound(Level pLevel, Entity pEntity) {
        pLevel.playSound(
            null, pEntity.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 0.8F, 0.8F + pEntity.level().getRandom().nextFloat() * 0.4F
        );
    }

    private void broadcastChangesOnContainerMenu(Player pPlayer) {
        AbstractContainerMenu abstractcontainermenu = pPlayer.containerMenu;
        if (abstractcontainermenu != null) {
            abstractcontainermenu.slotsChanged(pPlayer.getInventory());
        }
    }
}