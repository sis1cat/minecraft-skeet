package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.layers.WolfCollarLayer;
import net.minecraft.world.entity.EntityType;

import java.util.List;

public class ModelAdapterWolfCollar extends ModelAdapterWolf {
    public ModelAdapterWolfCollar() {
        this(EntityType.WOLF, "wolf_collar", ModelLayers.WOLF);
        this.setAlias("wolf");
    }

    protected ModelAdapterWolfCollar(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterAgeable modeladapterageable = new ModelAdapterWolfCollar(this.getEntityType(), "wolf_baby_collar", ModelLayers.WOLF_BABY);
        modeladapterageable.setBaby(true);
        modeladapterageable.setAliases(new String[]{this.getName(), "wolf"});
        return modeladapterageable;
    }

    @Override
    protected void modifyAgeableRenderer(AgeableMobRenderer renderer, Model model) {
        WolfModel wolfmodel = (WolfModel)model;

        //TODO: i don't know is it correct cast
        for (WolfCollarLayer wolfcollarlayer : (List<WolfCollarLayer>)(List<?>)this.getRenderLayers(renderer, WolfCollarLayer.class)) {
            if (this.isBaby()) {
                wolfcollarlayer.babyModel = wolfmodel;
            } else {
                wolfcollarlayer.adultModel = wolfmodel;
            }
        }
    }
}
