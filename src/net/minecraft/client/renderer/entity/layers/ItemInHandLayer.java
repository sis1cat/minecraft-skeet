package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.optifine.entity.model.CustomEntityModels;
import net.optifine.model.AttachmentType;

public class ItemInHandLayer<S extends ArmedEntityRenderState, M extends EntityModel<S> & ArmedModel> extends RenderLayer<S, M> {
    public ItemInHandLayer(RenderLayerParent<S, M> p_234846_) {
        super(p_234846_);
    }

    public void render(PoseStack p_117204_, MultiBufferSource p_117205_, int p_117206_, S p_375467_, float p_117208_, float p_117209_) {
        this.renderArmWithItem(p_375467_, p_375467_.rightHandItem, HumanoidArm.RIGHT, p_117204_, p_117205_, p_117206_);
        this.renderArmWithItem(p_375467_, p_375467_.leftHandItem, HumanoidArm.LEFT, p_117204_, p_117205_, p_117206_);
    }

    protected void renderArmWithItem(
        S pRenderState, ItemStackRenderState pItemStackRenderState, HumanoidArm pArm, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight
    ) {
        if (!pItemStackRenderState.isEmpty()) {
            pPoseStack.pushPose();
            if (!this.applyAttachmentTransform(pArm, pPoseStack)) {
                this.getParentModel().translateToHand(pArm, pPoseStack);
            }

            pPoseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            pPoseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            boolean flag = pArm == HumanoidArm.LEFT;
            pPoseStack.translate((float)(flag ? -1 : 1) / 16.0F, 0.125F, -0.625F);
            pItemStackRenderState.render(pPoseStack, pBufferSource, pPackedLight, OverlayTexture.NO_OVERLAY);
            pPoseStack.popPose();
        }
    }

    private boolean applyAttachmentTransform(HumanoidArm armIn, PoseStack matrixStackIn) {
        if (!CustomEntityModels.isActive()) {
            return false;
        } else {
            ModelPart modelpart = this.getRoot();
            if (modelpart == null) {
                return false;
            } else {
                AttachmentType attachmenttype = armIn == HumanoidArm.LEFT ? AttachmentType.LEFT_HANDHELD_ITEM : AttachmentType.RIGHT_HANDHELD_ITEM;
                return modelpart.applyAttachmentTransform(attachmenttype, matrixStackIn);
            }
        }
    }

    private ModelPart getRoot() {
        ArmedModel armedmodel = this.getParentModel();
        if (armedmodel instanceof HumanoidModel humanoidmodel) {
            return humanoidmodel.body.getParent();
        } else {
            return armedmodel instanceof Model model ? model.root() : null;
        }
    }
}