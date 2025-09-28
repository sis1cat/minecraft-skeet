package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.optifine.Config;
import net.optifine.entity.model.CustomEntityModels;
import net.optifine.shaders.Shaders;

public class LivingEntityEmissiveLayer<S extends LivingEntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
    private final ResourceLocation texture;
    private final LivingEntityEmissiveLayer.AlphaFunction<S> alphaFunction;
    private final LivingEntityEmissiveLayer.DrawSelector<S, M> drawSelector;
    private final Function<ResourceLocation, RenderType> bufferProvider;
    private final boolean alwaysVisible;

    public LivingEntityEmissiveLayer(
        RenderLayerParent<S, M> pRenderer,
        ResourceLocation pTexture,
        LivingEntityEmissiveLayer.AlphaFunction<S> pAlphaFunction,
        LivingEntityEmissiveLayer.DrawSelector<S, M> pDrawSelector,
        Function<ResourceLocation, RenderType> pBufferProvider,
        boolean pAlwaysVisible
    ) {
        super(pRenderer);
        this.texture = pTexture;
        this.alphaFunction = pAlphaFunction;
        this.drawSelector = pDrawSelector;
        this.bufferProvider = pBufferProvider;
        this.alwaysVisible = pAlwaysVisible;
    }

    public void render(PoseStack p_366547_, MultiBufferSource p_366685_, int p_367458_, S p_364851_, float p_362186_, float p_367844_) {
        if ((!p_364851_.isInvisible || this.alwaysVisible) && this.onlyDrawSelectedParts(p_364851_)) {
            if (Config.isShaders()) {
                Shaders.beginSpiderEyes();
            }

            Config.getRenderGlobal().renderOverlayEyes = true;
            VertexConsumer vertexconsumer = p_366685_.getBuffer(this.bufferProvider.apply(this.texture));
            float f = this.alphaFunction.apply(p_364851_, p_364851_.ageInTicks);
            int i = ARGB.color(Mth.floor(f * 255.0F), 255, 255, 255);
            this.getParentModel().renderToBuffer(p_366547_, vertexconsumer, p_367458_, LivingEntityRenderer.getOverlayCoords(p_364851_, 0.0F), i);
            this.resetDrawForAllParts();
            Config.getRenderGlobal().renderOverlayEyes = false;
            if (Config.isShaders()) {
                Shaders.endSpiderEyes();
            }
        }
    }

    private boolean onlyDrawSelectedParts(S pRenderState) {
        List<ModelPart> list = this.drawSelector.getPartsToDraw(this.getParentModel(), pRenderState);
        if (list.isEmpty()) {
            return false;
        } else {
            this.getParentModel().allParts().forEach(partIn -> partIn.skipDraw = true);
            list.forEach(partIn -> partIn.skipDraw = false);
            if (CustomEntityModels.isActive()) {
                list.forEach(partIn -> {
                    for (Entry<String, ModelPart> entry : partIn.children.entrySet()) {
                        if (entry.getKey().startsWith("CEM-")) {
                            entry.getValue().skipDraw = false;
                        }
                    }
                });
            }

            return true;
        }
    }

    private void resetDrawForAllParts() {
        this.getParentModel().allParts().forEach(partIn -> partIn.skipDraw = false);
    }

    public interface AlphaFunction<S extends LivingEntityRenderState> {
        float apply(S pRenderState, float pAlpha);
    }

    public interface DrawSelector<S extends LivingEntityRenderState, M extends EntityModel<S>> {
        List<ModelPart> getPartsToDraw(M pModel, S pRenderState);
    }
}