package net.optifine.entity.model;

import net.minecraft.client.model.SquidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.GlowSquidRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterGlowSquid extends ModelAdapterSquid {
    public ModelAdapterGlowSquid() {
        super(EntityType.GLOW_SQUID, "glow_squid", ModelLayers.GLOW_SQUID);
    }

    protected ModelAdapterGlowSquid(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterGlowSquid modeladapterglowsquid = new ModelAdapterGlowSquid(this.getEntityType(), "glow_squid_baby", ModelLayers.GLOW_SQUID_BABY);
        modeladapterglowsquid.setBaby(true);
        modeladapterglowsquid.setAlias(this.getName());
        return modeladapterglowsquid;
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new GlowSquidRenderer(context, new SquidModel(bakeModelLayer(ModelLayers.GLOW_SQUID)), new SquidModel(bakeModelLayer(ModelLayers.GLOW_SQUID_BABY)));
    }
}