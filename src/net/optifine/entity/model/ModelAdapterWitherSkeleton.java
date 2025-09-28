package net.optifine.entity.model;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WitherSkeletonRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterWitherSkeleton extends ModelAdapterSkeleton {
    public ModelAdapterWitherSkeleton() {
        super(EntityType.WITHER_SKELETON, "wither_skeleton", ModelLayers.WITHER_SKELETON);
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new WitherSkeletonRenderer(context);
    }
}