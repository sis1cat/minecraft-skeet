package net.minecraft.client.tutorial;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CraftPlanksTutorialStep implements TutorialStepInstance {
    private static final int HINT_DELAY = 1200;
    private static final Component CRAFT_TITLE = Component.translatable("tutorial.craft_planks.title");
    private static final Component CRAFT_DESCRIPTION = Component.translatable("tutorial.craft_planks.description");
    private final Tutorial tutorial;
    @Nullable
    private TutorialToast toast;
    private int timeWaiting;

    public CraftPlanksTutorialStep(Tutorial pTutorial) {
        this.tutorial = pTutorial;
    }

    @Override
    public void tick() {
        this.timeWaiting++;
        if (!this.tutorial.isSurvival()) {
            this.tutorial.setStep(TutorialSteps.NONE);
        } else {
            Minecraft minecraft = this.tutorial.getMinecraft();
            if (this.timeWaiting == 1) {
                LocalPlayer localplayer = minecraft.player;
                if (localplayer != null) {
                    if (localplayer.getInventory().contains(ItemTags.PLANKS)) {
                        this.tutorial.setStep(TutorialSteps.NONE);
                        return;
                    }

                    if (hasCraftedPlanksPreviously(localplayer, ItemTags.PLANKS)) {
                        this.tutorial.setStep(TutorialSteps.NONE);
                        return;
                    }
                }
            }

            if (this.timeWaiting >= 1200 && this.toast == null) {
                this.toast = new TutorialToast(minecraft.font, TutorialToast.Icons.WOODEN_PLANKS, CRAFT_TITLE, CRAFT_DESCRIPTION, false);
                minecraft.getToastManager().addToast(this.toast);
            }
        }
    }

    @Override
    public void clear() {
        if (this.toast != null) {
            this.toast.hide();
            this.toast = null;
        }
    }

    @Override
    public void onGetItem(ItemStack pStack) {
        if (pStack.is(ItemTags.PLANKS)) {
            this.tutorial.setStep(TutorialSteps.NONE);
        }
    }

    public static boolean hasCraftedPlanksPreviously(LocalPlayer pPlayer, TagKey<Item> pItems) {
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(pItems)) {
            if (pPlayer.getStats().getValue(Stats.ITEM_CRAFTED.get(holder.value())) > 0) {
                return true;
            }
        }

        return false;
    }
}