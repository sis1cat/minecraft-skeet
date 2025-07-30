package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.GiantZombieModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Giant;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GiantMobRenderer extends MobRenderer<Giant, ZombieRenderState, HumanoidModel<ZombieRenderState>> {
    private static final ResourceLocation ZOMBIE_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");

    public GiantMobRenderer(EntityRendererProvider.Context pContext, float pScale) {
        super(pContext, new GiantZombieModel(pContext.bakeLayer(ModelLayers.GIANT)), 0.5F * pScale);
        this.addLayer(new ItemInHandLayer<>(this));
        this.addLayer(
            new HumanoidArmorLayer<>(
                this,
                new GiantZombieModel(pContext.bakeLayer(ModelLayers.GIANT_INNER_ARMOR)),
                new GiantZombieModel(pContext.bakeLayer(ModelLayers.GIANT_OUTER_ARMOR)),
                pContext.getEquipmentRenderer()
            )
        );
    }

    public ResourceLocation getTextureLocation(ZombieRenderState p_368713_) {
        return ZOMBIE_LOCATION;
    }

    public ZombieRenderState createRenderState() {
        return new ZombieRenderState();
    }

    public void extractRenderState(Giant p_361435_, ZombieRenderState p_362298_, float p_367781_) {
        super.extractRenderState(p_361435_, p_362298_, p_367781_);
        HumanoidMobRenderer.extractHumanoidRenderState(p_361435_, p_362298_, p_367781_, this.itemModelResolver);
    }
}