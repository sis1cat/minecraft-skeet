package net.minecraft.client.gui.screens.advancements;

import net.minecraft.advancements.AdvancementType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum AdvancementWidgetType {
    OBTAINED(
        ResourceLocation.withDefaultNamespace("advancements/box_obtained"),
        ResourceLocation.withDefaultNamespace("advancements/task_frame_obtained"),
        ResourceLocation.withDefaultNamespace("advancements/challenge_frame_obtained"),
        ResourceLocation.withDefaultNamespace("advancements/goal_frame_obtained")
    ),
    UNOBTAINED(
        ResourceLocation.withDefaultNamespace("advancements/box_unobtained"),
        ResourceLocation.withDefaultNamespace("advancements/task_frame_unobtained"),
        ResourceLocation.withDefaultNamespace("advancements/challenge_frame_unobtained"),
        ResourceLocation.withDefaultNamespace("advancements/goal_frame_unobtained")
    );

    private final ResourceLocation boxSprite;
    private final ResourceLocation taskFrameSprite;
    private final ResourceLocation challengeFrameSprite;
    private final ResourceLocation goalFrameSprite;

    private AdvancementWidgetType(
        final ResourceLocation pBoxSprite, final ResourceLocation pTaskFrameSprite, final ResourceLocation pChallengeFrameSprite, final ResourceLocation pGoalFrameSprite
    ) {
        this.boxSprite = pBoxSprite;
        this.taskFrameSprite = pTaskFrameSprite;
        this.challengeFrameSprite = pChallengeFrameSprite;
        this.goalFrameSprite = pGoalFrameSprite;
    }

    public ResourceLocation boxSprite() {
        return this.boxSprite;
    }

    public ResourceLocation frameSprite(AdvancementType pType) {
        return switch (pType) {
            case TASK -> this.taskFrameSprite;
            case CHALLENGE -> this.challengeFrameSprite;
            case GOAL -> this.goalFrameSprite;
        };
    }
}