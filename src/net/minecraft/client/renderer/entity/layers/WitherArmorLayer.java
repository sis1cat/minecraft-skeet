package net.minecraft.client.renderer.entity.layers;

import net.minecraft.client.model.WitherBossModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.WitherRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class WitherArmorLayer extends EnergySwirlLayer<WitherRenderState, WitherBossModel> {
    private static final ResourceLocation WITHER_ARMOR_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/wither/wither_armor.png");
    public WitherBossModel model;
    public ResourceLocation customTextureLocation;

    public WitherArmorLayer(RenderLayerParent<WitherRenderState, WitherBossModel> pRenderer, EntityModelSet pModelSet) {
        super(pRenderer);
        this.model = new WitherBossModel(pModelSet.bakeLayer(ModelLayers.WITHER_ARMOR));
    }

    protected boolean isPowered(WitherRenderState p_361038_) {
        return p_361038_.isPowered;
    }

    @Override
    protected float xOffset(float p_117702_) {
        return Mth.cos(p_117702_ * 0.02F) * 3.0F;
    }

    @Override
    protected ResourceLocation getTextureLocation() {
        return this.customTextureLocation != null ? this.customTextureLocation : WITHER_ARMOR_LOCATION;
    }

    protected WitherBossModel model() {
        return this.model;
    }
}