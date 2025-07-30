package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.SnowGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.SnowGolemHeadLayer;
import net.minecraft.client.renderer.entity.state.SnowGolemRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SnowGolemRenderer extends MobRenderer<SnowGolem, SnowGolemRenderState, SnowGolemModel> {
    private static final ResourceLocation SNOW_GOLEM_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/snow_golem.png");

    public SnowGolemRenderer(EntityRendererProvider.Context p_174393_) {
        super(p_174393_, new SnowGolemModel(p_174393_.bakeLayer(ModelLayers.SNOW_GOLEM)), 0.5F);
        this.addLayer(new SnowGolemHeadLayer(this, p_174393_.getBlockRenderDispatcher()));
    }

    public ResourceLocation getTextureLocation(SnowGolemRenderState p_376350_) {
        return SNOW_GOLEM_LOCATION;
    }

    public SnowGolemRenderState createRenderState() {
        return new SnowGolemRenderState();
    }

    public void extractRenderState(SnowGolem p_361607_, SnowGolemRenderState p_378334_, float p_368353_) {
        super.extractRenderState(p_361607_, p_378334_, p_368353_);
        p_378334_.hasPumpkin = p_361607_.hasPumpkin();
    }
}