package net.minecraft.client.renderer.entity.layers;

import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.resources.ResourceLocation;

public class CreeperPowerLayer extends EnergySwirlLayer<CreeperRenderState, CreeperModel> {
    private static final ResourceLocation POWER_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/creeper/creeper_armor.png");
    public CreeperModel model;
    public ResourceLocation customTextureLocation;

    public CreeperPowerLayer(RenderLayerParent<CreeperRenderState, CreeperModel> pRenderer, EntityModelSet pModelSet) {
        super(pRenderer);
        this.model = new CreeperModel(pModelSet.bakeLayer(ModelLayers.CREEPER_ARMOR));
    }

    protected boolean isPowered(CreeperRenderState p_367950_) {
        return p_367950_.isPowered;
    }

    @Override
    protected float xOffset(float p_116683_) {
        return p_116683_ * 0.01F;
    }

    @Override
    protected ResourceLocation getTextureLocation() {
        return this.customTextureLocation != null ? this.customTextureLocation : POWER_LOCATION;
    }

    protected CreeperModel model() {
        return this.model;
    }
}