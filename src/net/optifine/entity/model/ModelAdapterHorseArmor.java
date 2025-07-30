package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.layers.HorseArmorLayer;
import net.minecraft.world.entity.EntityType;
import net.optifine.reflect.Reflector;
import net.optifine.reflect.ReflectorField;

public class ModelAdapterHorseArmor extends ModelAdapterHorse {
    public ModelAdapterHorseArmor() {
        super(EntityType.HORSE, "horse_armor", ModelLayers.HORSE_ARMOR);
    }

    protected ModelAdapterHorseArmor(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterHorseArmor modeladapterhorsearmor = new ModelAdapterHorseArmor(this.getEntityType(), "horse_baby_armor", ModelLayers.HORSE_BABY_ARMOR);
        modeladapterhorsearmor.setBaby(true);
        modeladapterhorsearmor.setAlias(this.getName());
        return modeladapterhorsearmor;
    }

    @Override
    protected void modifyAgeableRenderer(AgeableMobRenderer renderer, Model modelBase) {
        ReflectorField reflectorfield = this.isBaby() ? Reflector.HorseArmorLayer_babyModel : Reflector.HorseArmorLayer_adultModel;
        this.setLayerModel(renderer, HorseArmorLayer.class, reflectorfield, "HorseArmorLayer_model", modelBase);
    }
}