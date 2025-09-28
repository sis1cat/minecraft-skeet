package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.layers.DrownedOuterLayer;
import net.minecraft.world.entity.EntityType;
import net.optifine.reflect.Reflector;
import net.optifine.reflect.ReflectorField;

public class ModelAdapterDrownedOuter extends ModelAdapterDrowned {
    public ModelAdapterDrownedOuter() {
        super(EntityType.DROWNED, "drowned_outer", ModelLayers.DROWNED_OUTER_LAYER);
    }

    protected ModelAdapterDrownedOuter(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterDrownedOuter modeladapterdrownedouter = new ModelAdapterDrownedOuter(this.getEntityType(), "drowned_baby_outer", ModelLayers.DROWNED_BABY_OUTER_LAYER);
        modeladapterdrownedouter.setBaby(true);
        modeladapterdrownedouter.setAlias(this.getName());
        return modeladapterdrownedouter;
    }

    @Override
    protected void modifyAgeableRenderer(AgeableMobRenderer renderer, Model modelBase) {
        ReflectorField reflectorfield = this.isBaby() ? Reflector.DrownedOuterLayer_babyModel : Reflector.DrownedOuterLayer_adultModel;
        this.setLayerModel(renderer, DrownedOuterLayer.class, reflectorfield, "DrownedOuterLayer.model", modelBase);
    }
}