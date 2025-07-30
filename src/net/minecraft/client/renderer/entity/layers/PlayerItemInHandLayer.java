package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerItemInHandLayer<S extends PlayerRenderState, M extends EntityModel<S> & ArmedModel & HeadedModel> extends ItemInHandLayer<S, M> {
    private static final float X_ROT_MIN = (float) (-Math.PI / 6);
    private static final float X_ROT_MAX = (float) (Math.PI / 2);

    public PlayerItemInHandLayer(RenderLayerParent<S, M> p_234866_) {
        super(p_234866_);
    }

    protected void renderArmWithItem(
        S p_369457_, ItemStackRenderState p_377241_, HumanoidArm p_364119_, PoseStack p_364225_, MultiBufferSource p_364963_, int p_366853_
    ) {
        if (!p_377241_.isEmpty()) {
            InteractionHand interactionhand = p_364119_ == p_369457_.mainArm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            if (p_369457_.isUsingItem && p_369457_.useItemHand == interactionhand && p_369457_.attackTime < 1.0E-5F && !p_369457_.heldOnHead.isEmpty()) {
                this.renderItemHeldToEye(p_369457_.heldOnHead, p_364119_, p_364225_, p_364963_, p_366853_);
            } else {
                super.renderArmWithItem(p_369457_, p_377241_, p_364119_, p_364225_, p_364963_, p_366853_);
            }
        }
    }

    private void renderItemHeldToEye(ItemStackRenderState pRenderState, HumanoidArm pArm, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight) {
        pPoseStack.pushPose();
        this.getParentModel().root().translateAndRotate(pPoseStack);
        ModelPart modelpart = this.getParentModel().getHead();
        float f = modelpart.xRot;
        modelpart.xRot = Mth.clamp(modelpart.xRot, (float) (-Math.PI / 6), (float) (Math.PI / 2));
        modelpart.translateAndRotate(pPoseStack);
        modelpart.xRot = f;
        CustomHeadLayer.translateToHead(pPoseStack, CustomHeadLayer.Transforms.DEFAULT);
        boolean flag = pArm == HumanoidArm.LEFT;
        pPoseStack.translate((flag ? -2.5F : 2.5F) / 16.0F, -0.0625F, 0.0F);
        pRenderState.render(pPoseStack, pBufferSource, pPackedLight, OverlayTexture.NO_OVERLAY);
        pPoseStack.popPose();
    }
}