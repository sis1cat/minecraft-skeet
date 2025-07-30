package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.PolarBearModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PolarBearRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterPolarBear extends ModelAdapterQuadruped {
    public ModelAdapterPolarBear() {
        super(EntityType.POLAR_BEAR, "polar_bear", ModelLayers.POLAR_BEAR);
    }

    protected ModelAdapterPolarBear(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterPolarBear modeladapterpolarbear = new ModelAdapterPolarBear(this.getEntityType(), "polar_bear_baby", ModelLayers.POLAR_BEAR_BABY);
        modeladapterpolarbear.setBaby(true);
        modeladapterpolarbear.setAlias(this.getName());
        return modeladapterpolarbear;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new PolarBearModel(root);
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new PolarBearRenderer(context);
    }
}