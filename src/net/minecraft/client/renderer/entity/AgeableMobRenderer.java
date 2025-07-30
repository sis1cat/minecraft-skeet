package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Mob;

@Deprecated
public abstract class AgeableMobRenderer<T extends Mob, S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends MobRenderer<T, S, M> {
    private M adultModel;
    private M babyModel;

    public AgeableMobRenderer(EntityRendererProvider.Context pContext, M pAdultModel, M pBabyModel, float pScale) {
        super(pContext, pAdultModel, pScale);
        this.adultModel = pAdultModel;
        this.babyModel = pBabyModel;
    }

    @Override
    public void render(S p_363116_, PoseStack p_363662_, MultiBufferSource p_366693_, int p_363199_) {
        this.model = p_363116_.isBaby ? this.babyModel : this.adultModel;
        super.render(p_363116_, p_363662_, p_366693_, p_363199_);
    }

    public M getAdultModel() {
        return this.adultModel;
    }

    public M getBabyModel() {
        return this.babyModel;
    }

    public void setAdultModel(M adultModel) {
        this.adultModel = adultModel;
        super.model = adultModel;
    }

    public void setBabyModel(M babyModel) {
        this.babyModel = babyModel;
    }
}