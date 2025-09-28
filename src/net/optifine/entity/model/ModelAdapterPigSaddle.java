package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.PigRenderer;
import net.minecraft.client.renderer.entity.layers.SaddleLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.List;

public class ModelAdapterPigSaddle extends ModelAdapterPig {
    public ModelAdapterPigSaddle() {
        super(EntityType.PIG, "pig_saddle", ModelLayers.PIG_SADDLE);
    }

    protected ModelAdapterPigSaddle(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterPigSaddle modeladapterpigsaddle = new ModelAdapterPigSaddle(this.getEntityType(), "pig_baby_saddle", ModelLayers.PIG_BABY_SADDLE);
        modeladapterpigsaddle.setBaby(true);
        modeladapterpigsaddle.setAlias(this.getName());
        return modeladapterpigsaddle;
    }

    @Override
    protected void modifyAgeableRenderer(AgeableMobRenderer renderer, Model model) {
        PigModel pigmodel = (PigModel)model;

        //TODO: i don't know is it correct cast
        for (SaddleLayer saddlelayer : (List<SaddleLayer>)(List<?>)this.getRenderLayers(renderer, SaddleLayer.class)) {
            if (this.isBaby()) {
                saddlelayer.babyModel = pigmodel;
            } else {
                saddlelayer.adultModel = pigmodel;
            }
        }
    }

    @Override
    public boolean setTextureLocation(IEntityRenderer er, ResourceLocation textureLocation) {
        PigRenderer pigrenderer = (PigRenderer)er;

        for (SaddleLayer saddlelayer : pigrenderer.getLayers(SaddleLayer.class)) {
            if (this.isBaby()) {
                saddlelayer.textureLocationBaby = textureLocation;
            } else {
                saddlelayer.textureLocation = textureLocation;
            }
        }

        return true;
    }
}
