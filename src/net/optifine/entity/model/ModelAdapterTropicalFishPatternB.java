package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.TropicalFishModelB;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.TropicalFishRenderer;
import net.minecraft.client.renderer.entity.layers.TropicalFishPatternLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.optifine.reflect.Reflector;

public class ModelAdapterTropicalFishPatternB extends ModelAdapterTropicalFishB {
    public ModelAdapterTropicalFishPatternB() {
        super(EntityType.TROPICAL_FISH, "tropical_fish_pattern_b", ModelLayers.TROPICAL_FISH_LARGE_PATTERN);
    }

    @Override
    protected void modifyLivingRenderer(LivingEntityRenderer renderer, Model modelBase) {
        TropicalFishRenderer tropicalfishrenderer = (TropicalFishRenderer)renderer;

        for (TropicalFishPatternLayer tropicalfishpatternlayer : tropicalfishrenderer.getLayers(TropicalFishPatternLayer.class)) {
            this.setModel(tropicalfishpatternlayer, Reflector.TropicalFishPatternLayer_modelB, "TropicalFishPatternLayer.modelB", modelBase);
        }
    }

    @Override
    public boolean setTextureLocation(IEntityRenderer er, ResourceLocation textureLocation) {
        TropicalFishRenderer tropicalfishrenderer = (TropicalFishRenderer)er;

        for (TropicalFishPatternLayer tropicalfishpatternlayer : tropicalfishrenderer.getLayers(TropicalFishPatternLayer.class)) {
            TropicalFishModelB tropicalfishmodelb = (TropicalFishModelB)Reflector.TropicalFishPatternLayer_modelB.getValue(tropicalfishpatternlayer);
            if (tropicalfishmodelb == null) {
                tropicalfishmodelb.locationTextureCustom = textureLocation;
            }
        }

        return true;
    }
}