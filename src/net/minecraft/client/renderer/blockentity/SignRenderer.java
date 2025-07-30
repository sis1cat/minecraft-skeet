package net.minecraft.client.renderer.blockentity;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.Vec3;

public class SignRenderer extends AbstractSignRenderer {
    private static final float RENDER_SCALE = 0.6666667F;
    private static final Vec3 TEXT_OFFSET = new Vec3(0.0, 0.33333334F, 0.046666667F);
    private Map<WoodType, SignRenderer.Models> signModels;

    public SignRenderer(BlockEntityRendererProvider.Context pContext) {
        super(pContext);
        this.signModels = WoodType.values()
            .collect(
                ImmutableMap.toImmutableMap(
                    woodTypeIn -> (WoodType)woodTypeIn,
                    typeIn -> new SignRenderer.Models(createSignModel(pContext.getModelSet(), typeIn, true), createSignModel(pContext.getModelSet(), typeIn, false))
                )
            );
    }

    @Override
    protected Model getSignModel(BlockState p_378677_, WoodType p_376798_) {
        SignRenderer.Models signrenderer$models = this.signModels.get(p_376798_);
        return p_378677_.getBlock() instanceof StandingSignBlock ? signrenderer$models.standing() : signrenderer$models.wall();
    }

    @Override
    protected Material getSignMaterial(WoodType pWoodType) {
        return Sheets.getSignMaterial(pWoodType);
    }

    @Override
    protected float getSignModelRenderScale() {
        return 0.6666667F;
    }

    @Override
    protected float getSignTextRenderScale() {
        return 0.6666667F;
    }

    private static void translateBase(PoseStack pPoseStack, float pYRot) {
        pPoseStack.translate(0.5F, 0.5F, 0.5F);
        pPoseStack.mulPose(Axis.YP.rotationDegrees(pYRot));
    }

    @Override
    protected void translateSign(PoseStack pPoseStack, float pYRot, BlockState pState) {
        translateBase(pPoseStack, pYRot);
        if (!(pState.getBlock() instanceof StandingSignBlock)) {
            pPoseStack.translate(0.0F, -0.3125F, -0.4375F);
        }
    }

    @Override
    protected Vec3 getTextOffset() {
        return TEXT_OFFSET;
    }

    public static void renderInHand(PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay, Model pModel, Material pMaterial) {
        pPoseStack.pushPose();
        translateBase(pPoseStack, 0.0F);
        pPoseStack.scale(0.6666667F, -0.6666667F, -0.6666667F);
        VertexConsumer vertexconsumer = pMaterial.buffer(pBufferSource, pModel::renderType);
        pModel.renderToBuffer(pPoseStack, vertexconsumer, pPackedLight, pPackedOverlay);
        pPoseStack.popPose();
    }

    public static Model createSignModel(EntityModelSet pModelSet, WoodType pWoodType, boolean pStandingSign) {
        ModelLayerLocation modellayerlocation = pStandingSign ? ModelLayers.createStandingSignModelName(pWoodType) : ModelLayers.createWallSignModelName(pWoodType);
        return new Model.Simple(pModelSet.bakeLayer(modellayerlocation), RenderType::entityCutoutNoCull);
    }

    public static LayerDefinition createSignLayer(boolean pStandingSign) {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("sign", CubeListBuilder.create().texOffs(0, 0).addBox(-12.0F, -14.0F, -1.0F, 24.0F, 12.0F, 2.0F), PartPose.ZERO);
        if (pStandingSign) {
            partdefinition.addOrReplaceChild(
                "stick", CubeListBuilder.create().texOffs(0, 14).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 14.0F, 2.0F), PartPose.ZERO
            );
        }

        return LayerDefinition.create(meshdefinition, 64, 32);
    }

    public void setSignModel(WoodType woodType, Model signModel, boolean standing) {
        if (this.signModels instanceof ImmutableMap) {
            this.signModels = new HashMap<>(this.signModels);
        }

        SignRenderer.Models signrenderer$models = this.signModels.get(woodType);
        SignRenderer.Models signrenderer$models1 = standing
            ? new SignRenderer.Models(signModel, signrenderer$models.wall())
            : new SignRenderer.Models(signrenderer$models.standing(), signModel);
        this.signModels.put(woodType, signrenderer$models1);
    }

    public static record Models(Model standing, Model wall) {
    }
}