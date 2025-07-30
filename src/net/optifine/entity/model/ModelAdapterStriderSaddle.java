package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.StriderModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.StriderRenderer;
import net.minecraft.client.renderer.entity.layers.SaddleLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.List;

public class ModelAdapterStriderSaddle extends ModelAdapterStrider {
    public ModelAdapterStriderSaddle() {
        super(EntityType.STRIDER, "strider_saddle", ModelLayers.STRIDER_SADDLE);
    }

    private ModelAdapterStriderSaddle(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterStriderSaddle modeladapterstridersaddle = new ModelAdapterStriderSaddle(this.getEntityType(), "strider_baby_saddle", ModelLayers.STRIDER_BABY_SADDLE);
        modeladapterstridersaddle.setBaby(true);
        modeladapterstridersaddle.setAlias(this.getName());
        return modeladapterstridersaddle;
    }

    @Override
    protected void modifyAgeableRenderer(AgeableMobRenderer renderer, Model modelBase) {
        StriderModel stridermodel = (StriderModel)modelBase;

        for (SaddleLayer saddlelayer : (List<SaddleLayer>)(List<?>) this.getRenderLayers(renderer, SaddleLayer.class)) {
            if (this.isBaby()) {
                saddlelayer.babyModel = stridermodel;
            } else {
                saddlelayer.adultModel = stridermodel;
            }
        }
    }

    @Override
    public boolean setTextureLocation(IEntityRenderer er, ResourceLocation textureLocation) {
        StriderRenderer striderrenderer = (StriderRenderer)er;

        for (SaddleLayer saddlelayer : striderrenderer.getLayers(SaddleLayer.class)) {
            if (this.isBaby()) {
                saddlelayer.textureLocationBaby = textureLocation;
            } else {
                saddlelayer.textureLocation = textureLocation;
            }
        }

        return true;
    }
}
