package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.BreezeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.BreezeRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.BreezeRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class BreezeWindLayer extends RenderLayer<BreezeRenderState, BreezeModel> {
    private ResourceLocation TEXTURE_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/breeze/breeze_wind.png");
    private BreezeModel model;

    public BreezeWindLayer(EntityRendererProvider.Context pContext, RenderLayerParent<BreezeRenderState, BreezeModel> pRenderer) {
        super(pRenderer);
        this.model = new BreezeModel(pContext.bakeLayer(ModelLayers.BREEZE_WIND));
    }

    public void render(PoseStack p_312401_, MultiBufferSource p_310855_, int p_312784_, BreezeRenderState p_362639_, float p_311307_, float p_312259_) {
        VertexConsumer vertexconsumer = p_310855_.getBuffer(RenderType.breezeWind(this.TEXTURE_LOCATION, this.xOffset(p_362639_.ageInTicks) % 1.0F, 0.0F));
        this.model.setupAnim(p_362639_);
        BreezeRenderer.enable(this.model, this.model.wind()).renderToBuffer(p_312401_, vertexconsumer, p_312784_, OverlayTexture.NO_OVERLAY);
    }

    private float xOffset(float pTickCount) {
        return pTickCount * 0.02F;
    }

    public void setModel(BreezeModel model) {
        this.model = model;
    }

    public void setTextureLocation(ResourceLocation textureLocation) {
        this.TEXTURE_LOCATION = textureLocation;
    }
}