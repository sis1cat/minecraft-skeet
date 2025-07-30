package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.SheepModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.layers.SheepWoolLayer;
import net.minecraft.world.entity.EntityType;

import java.util.List;

public class ModelAdapterSheepWool extends ModelAdapterSheep {
    public ModelAdapterSheepWool() {
        super(EntityType.SHEEP, "sheep_wool", ModelLayers.SHEEP_WOOL);
    }

    protected ModelAdapterSheepWool(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterSheepWool modeladaptersheepwool = new ModelAdapterSheepWool(this.getEntityType(), "sheep_baby_wool", ModelLayers.SHEEP_BABY_WOOL);
        modeladaptersheepwool.setBaby(true);
        modeladaptersheepwool.setAlias(this.getName());
        return modeladaptersheepwool;
    }

    @Override
    protected void modifyAgeableRenderer(AgeableMobRenderer renderer, Model modelBase) {
        SheepModel sheepmodel = (SheepModel)modelBase;

        //TODO: i don't know is it correct cast
        for (SheepWoolLayer sheepwoollayer : (List<SheepWoolLayer>)(List<?>)this.getRenderLayers(renderer, SheepWoolLayer.class)) {
            if (this.isBaby()) {
                sheepwoollayer.babyModel = sheepmodel;
            } else {
                sheepwoollayer.adultModel = sheepmodel;
            }
        }
    }
}
