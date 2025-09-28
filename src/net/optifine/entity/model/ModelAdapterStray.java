package net.optifine.entity.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.StrayRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterStray extends ModelAdapterSkeleton {
    public ModelAdapterStray() {
        super(EntityType.STRAY, "stray", ModelLayers.STRAY);
    }

    public ModelAdapterStray(EntityType type, String name, ModelLayerLocation modelLayer) {
        super(type, name, modelLayer);
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new StrayRenderer(context);
    }
}