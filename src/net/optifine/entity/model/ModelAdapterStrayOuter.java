package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.StrayRenderer;
import net.minecraft.client.renderer.entity.layers.SkeletonClothingLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterStrayOuter extends ModelAdapterStray {
    public ModelAdapterStrayOuter() {
        super(EntityType.STRAY, "stray_outer", ModelLayers.STRAY_OUTER_LAYER);
    }

    @Override
    protected void modifyAgeableRenderer(AgeableMobRenderer renderer, Model modelBase) {
        StrayRenderer strayrenderer = (StrayRenderer)renderer;
        ResourceLocation resourcelocation = new ResourceLocation("textures/entity/skeleton/stray_overlay.png");
        SkeletonClothingLayer skeletonclothinglayer = new SkeletonClothingLayer<>(
            strayrenderer, this.getContext().getModelSet(), ModelLayers.STRAY_OUTER_LAYER, resourcelocation
        );
        skeletonclothinglayer.layerModel = (SkeletonModel)modelBase;
        strayrenderer.replaceLayer(SkeletonClothingLayer.class, skeletonclothinglayer);
    }

    @Override
    public boolean setTextureLocation(IEntityRenderer er, ResourceLocation textureLocation) {
        StrayRenderer strayrenderer = (StrayRenderer)er;

        for (SkeletonClothingLayer skeletonclothinglayer : strayrenderer.getLayers(SkeletonClothingLayer.class)) {
            skeletonclothinglayer.clothesLocation = textureLocation;
        }

        return true;
    }
}
