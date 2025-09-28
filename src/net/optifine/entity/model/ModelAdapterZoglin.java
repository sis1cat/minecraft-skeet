package net.optifine.entity.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZoglinRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterZoglin extends ModelAdapterHoglin {
    public ModelAdapterZoglin() {
        this(EntityType.ZOGLIN, "zoglin", ModelLayers.ZOGLIN);
    }

    protected ModelAdapterZoglin(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterAgeable modeladapterageable = new ModelAdapterZoglin(this.getEntityType(), "zoglin_baby", ModelLayers.ZOGLIN_BABY);
        modeladapterageable.setBaby(true);
        modeladapterageable.setAlias(this.getName());
        return modeladapterageable;
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new ZoglinRenderer(context);
    }
}