package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.layers.WolfArmorLayer;
import net.minecraft.world.entity.EntityType;
import net.optifine.reflect.Reflector;
import net.optifine.reflect.ReflectorField;

public class ModelAdapterWolfArmor extends ModelAdapterWolf {
    public ModelAdapterWolfArmor() {
        this(EntityType.WOLF, "wolf_armor", ModelLayers.WOLF_ARMOR);
    }

    protected ModelAdapterWolfArmor(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterAgeable modeladapterageable = new ModelAdapterWolfArmor(this.getEntityType(), "wolf_baby_armor", ModelLayers.WOLF_BABY_ARMOR);
        modeladapterageable.setBaby(true);
        modeladapterageable.setAlias(this.getName());
        return modeladapterageable;
    }

    @Override
    protected void modifyAgeableRenderer(AgeableMobRenderer renderer, Model modelBase) {
        ReflectorField reflectorfield = this.isBaby() ? Reflector.WolfArmorLayer_babyModel : Reflector.WolfArmorLayer_adultModel;
        this.setLayerModel(renderer, WolfArmorLayer.class, reflectorfield, "WolfArmorLayer.model", modelBase);
    }
}