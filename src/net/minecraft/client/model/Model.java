package net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.AnimationState;
import net.optifine.EmissiveTextures;
import org.joml.Vector3f;

public abstract class Model {
    private static final Vector3f ANIMATION_VECTOR_CACHE = new Vector3f();
    protected final ModelPart root;
    protected final Function<ResourceLocation, RenderType> renderType;
    private final List<ModelPart> allParts;
    public int textureWidth = 64;
    public int textureHeight = 32;
    public ResourceLocation locationTextureCustom;

    public Model(ModelPart pRoot, Function<ResourceLocation, RenderType> pRenderType) {
        this.root = pRoot;
        this.renderType = pRenderType;
        this.allParts = pRoot.getAllParts().toList();
    }

    public final RenderType renderType(ResourceLocation pLocation) {
        if (this.locationTextureCustom != null) {
            pLocation = this.locationTextureCustom;
        }

        RenderType rendertype = this.renderType.apply(pLocation);
        if (EmissiveTextures.isRenderEmissive() && rendertype.isEntitySolid()) {
            rendertype = RenderType.entityCutout(pLocation);
        }

        return rendertype;
    }

    public final void renderToBuffer(PoseStack pPoseStack, VertexConsumer pBuffer, int pPackedLight, int pPackedOverlay, int pColor) {
        this.root().render(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, pColor);
    }

    public final void renderToBuffer(PoseStack pPoseStack, VertexConsumer pBuffer, int pPackedLight, int pPackedOverlay) {
        this.renderToBuffer(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, -1);
    }

    public final ModelPart root() {
        return this.root;
    }

    public Optional<ModelPart> getAnyDescendantWithName(String pName) {
        return pName.equals("root")
            ? Optional.of(this.root())
            : this.root().getAllParts().filter(partIn -> partIn.hasChild(pName)).findFirst().map(part2In -> part2In.getChild(pName));
    }

    public final List<ModelPart> allParts() {
        return this.allParts;
    }

    public final void resetPose() {
        for (ModelPart modelpart : this.allParts) {
            modelpart.resetPose();
        }
    }

    protected void animate(AnimationState pState, AnimationDefinition pDefinition, float pAgeInTicks) {
        this.animate(pState, pDefinition, pAgeInTicks, 1.0F);
    }

    protected void animateWalk(AnimationDefinition pDefinition, float pWalkAnimationPos, float pWalkAnimationSpeed, float pTimeMultiplier, float pSpeedMultiplier) {
        long i = (long)(pWalkAnimationPos * 50.0F * pTimeMultiplier);
        float f = Math.min(pWalkAnimationSpeed * pSpeedMultiplier, 1.0F);
        KeyframeAnimations.animate(this, pDefinition, i, f, ANIMATION_VECTOR_CACHE);
    }

    protected void animate(AnimationState pState, AnimationDefinition pDefinition, float pAgeInTicks, float pSpeed) {
        pState.ifStarted(
            animState -> KeyframeAnimations.animate(this, pDefinition, (long)((float)animState.getTimeInMillis(pAgeInTicks) * pSpeed), 1.0F, ANIMATION_VECTOR_CACHE)
        );
    }

    protected void applyStatic(AnimationDefinition pAnimationDefinition) {
        KeyframeAnimations.animate(this, pAnimationDefinition, 0L, 1.0F, ANIMATION_VECTOR_CACHE);
    }

    public boolean isRenderRoot() {
        return true;
    }

    public static class Simple extends Model {
        public Simple(ModelPart pRoot, Function<ResourceLocation, RenderType> pRenderType) {
            super(pRoot, pRenderType);
        }
    }
}