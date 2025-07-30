package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.resources.ResourceLocation;

public class SlimeOuterLayer extends RenderLayer<SlimeRenderState, SlimeModel> {
    public SlimeModel model;
    public ResourceLocation customTextureLocation;

    public SlimeOuterLayer(RenderLayerParent<SlimeRenderState, SlimeModel> pRenderer, EntityModelSet pModelSet) {
        super(pRenderer);
        this.model = new SlimeModel(pModelSet.bakeLayer(ModelLayers.SLIME_OUTER));
    }

    public void render(PoseStack p_117459_, MultiBufferSource p_117460_, int p_117461_, SlimeRenderState p_367329_, float p_117463_, float p_117464_) {
        boolean flag = p_367329_.appearsGlowing && p_367329_.isInvisible;
        if (!p_367329_.isInvisible || flag) {
            ResourceLocation resourcelocation = this.customTextureLocation != null ? this.customTextureLocation : SlimeRenderer.SLIME_LOCATION;
            VertexConsumer vertexconsumer;
            if (flag) {
                vertexconsumer = p_117460_.getBuffer(RenderType.outline(resourcelocation));
            } else {
                vertexconsumer = p_117460_.getBuffer(RenderType.entityTranslucent(resourcelocation));
            }

            this.model.setupAnim(p_367329_);
            this.model.renderToBuffer(p_117459_, vertexconsumer, p_117461_, LivingEntityRenderer.getOverlayCoords(p_367329_, 0.0F));
        }
    }
}