package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

public class ShulkerBoxRenderer implements BlockEntityRenderer<ShulkerBoxBlockEntity> {
    private final ShulkerBoxRenderer.ShulkerBoxModel model;

    public ShulkerBoxRenderer(BlockEntityRendererProvider.Context pContext) {
        this(pContext.getModelSet());
    }

    public ShulkerBoxRenderer(EntityModelSet pModelSet) {
        this.model = new ShulkerBoxRenderer.ShulkerBoxModel(pModelSet.bakeLayer(ModelLayers.SHULKER_BOX));
    }

    public void render(ShulkerBoxBlockEntity p_112478_, float p_112479_, PoseStack p_112480_, MultiBufferSource p_112481_, int p_112482_, int p_112483_) {
        Direction direction = p_112478_.getBlockState().getValueOrElse(ShulkerBoxBlock.FACING, Direction.UP);
        DyeColor dyecolor = p_112478_.getColor();
        Material material;
        if (dyecolor == null) {
            material = Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION;
        } else {
            material = Sheets.getShulkerBoxMaterial(dyecolor);
        }

        float f = p_112478_.getProgress(p_112479_);
        this.render(p_112480_, p_112481_, p_112482_, p_112483_, direction, f, material);
    }

    public void render(
        PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay, Direction pFacing, float pProgress, Material pMaterial
    ) {
        pPoseStack.pushPose();
        pPoseStack.translate(0.5F, 0.5F, 0.5F);
        float f = 0.9995F;
        pPoseStack.scale(0.9995F, 0.9995F, 0.9995F);
        pPoseStack.mulPose(pFacing.getRotation());
        pPoseStack.scale(1.0F, -1.0F, -1.0F);
        pPoseStack.translate(0.0F, -1.0F, 0.0F);
        this.model.animate(pProgress);
        VertexConsumer vertexconsumer = pMaterial.buffer(pBufferSource, this.model::renderType);
        this.model.renderToBuffer(pPoseStack, vertexconsumer, pPackedLight, pPackedOverlay);
        pPoseStack.popPose();
    }

    public static class ShulkerBoxModel extends Model {
        private final ModelPart lid;

        public ShulkerBoxModel(ModelPart pRoot) {
            super(pRoot, RenderType::entityCutoutNoCull);
            this.lid = pRoot.getChild("lid");
        }

        public void animate(float pProgress) {
            this.lid.setPos(0.0F, 24.0F - pProgress * 0.5F * 16.0F, 0.0F);
            this.lid.yRot = 270.0F * pProgress * (float) (Math.PI / 180.0);
        }
    }
}