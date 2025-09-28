package net.optifine.entity.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.UndeadHorseRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterSkeletonHorse extends ModelAdapterHorse {
    public ModelAdapterSkeletonHorse() {
        this(EntityType.SKELETON_HORSE, "skeleton_horse", ModelLayers.SKELETON_HORSE);
    }

    protected ModelAdapterSkeletonHorse(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterSkeletonHorse modeladapterskeletonhorse = new ModelAdapterSkeletonHorse(this.getEntityType(), "skeleton_horse_baby", ModelLayers.SKELETON_HORSE_BABY);
        modeladapterskeletonhorse.setBaby(true);
        modeladapterskeletonhorse.setAlias(this.getName());
        return modeladapterskeletonhorse;
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new UndeadHorseRenderer(context, ModelLayers.SKELETON_HORSE, ModelLayers.SKELETON_HORSE_BABY, true);
    }
}