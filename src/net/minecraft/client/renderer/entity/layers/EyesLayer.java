package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.optifine.Config;
import net.optifine.shaders.Shaders;

public abstract class EyesLayer<S extends EntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
    public EyesLayer(RenderLayerParent<S, M> p_116981_) {
        super(p_116981_);
    }

    @Override
    public void render(PoseStack p_116983_, MultiBufferSource p_116984_, int p_116985_, S p_362168_, float p_116987_, float p_116988_) {
        VertexConsumer vertexconsumer = p_116984_.getBuffer(this.renderType());
        if (Config.isShaders()) {
            Shaders.beginSpiderEyes();
        }

        Config.getRenderGlobal().renderOverlayEyes = true;
        this.getParentModel().renderToBuffer(p_116983_, vertexconsumer, p_116985_, OverlayTexture.NO_OVERLAY);
        Config.getRenderGlobal().renderOverlayEyes = false;
        if (Config.isShaders()) {
            Shaders.endSpiderEyes();
        }
    }

    public abstract RenderType renderType();
}